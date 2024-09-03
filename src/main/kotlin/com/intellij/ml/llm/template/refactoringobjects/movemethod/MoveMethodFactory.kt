package com.intellij.ml.llm.template.refactoringobjects.movemethod

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.intellij.ml.llm.template.intentions.ApplySuggestRefactoringIntention
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.MoveMethodRefactoringPrompt
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataManager
import com.intellij.ml.llm.template.utils.JsonUtils
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.move.MoveInstanceMembersUtil
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodProcessor
import com.intellij.refactoring.openapi.impl.JavaRefactoringFactoryImpl
import com.intellij.refactoring.suggested.startOffset
import com.intellij.usageView.UsageInfo
import dev.langchain4j.model.chat.ChatLanguageModel
import org.jetbrains.kotlin.idea.editor.fixers.endLine
import org.jetbrains.kotlin.idea.editor.fixers.startLine
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import kotlin.math.min
import kotlin.system.measureTimeMillis


class MoveMethodFactory {


    data class MovePivot(val psiClass: PsiClass, val psiElement: PsiElement?, var rationale: String? = null)
    data class MoveSuggestion(
        @SerializedName("target_class")
        val targetClassName: String,
        @SerializedName("rationale")
        val rationale: String
    )
    companion object: MyRefactoringFactory{

        const val TOPN_SUGGESTIONS4USER = 1
        const val TOPN_SUGGESTIONS4LLM = 5
        val llmResponseCache = mutableMapOf<String, LLMBaseResponse>()

        fun test(){

        }

        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {

            val params = getParamsFromFuncCall(funcCall)
            val methodName = getStringFromParam(params[0])
            val targetVariable = getStringFromParam(params[1])

            return createMoveMethodFromName(editor, file, methodName, project)
        }

        fun createMoveMethodFromName(
            editor: Editor,
            file: PsiFile,
            methodName: String,
            project: Project,
            llmChatModel: ChatLanguageModel? = null,
            telemetyDataManager: EFTelemetryDataManager? =null
        ): List<AbstractRefactoring> {
            val outerClass: PsiElement? =
                runReadAction {
                    PsiUtils.getParentClassOrNull(editor, language = file.language) ?: file.getChildOfType<PsiClass>()
                }
            val methodToMove =
                runReadAction { PsiUtils.getMethodNameFromClass(outerClass, methodName) } ?: return listOf()

            return createMoveMethodRefactorings(project, methodToMove, editor, file, llmChatModel, telemetyDataManager)
        }

        private fun createMoveMethodRefactorings(
            project: Project,
            methodToMove: PsiMethod,
            editor: Editor,
            file: PsiFile,
            llmChatModel: ChatLanguageModel?,
            telemetryDataManager: EFTelemetryDataManager?
        ): List<AbstractRefactoring> {

            val targetPivots = getPotentialMovePivots(project, editor, file, methodToMove)
            val validMovePivots = getValidPivots(project, editor, file, methodToMove, targetPivots)
            val targetPivotsWithSimilarity: List<Pair<MovePivot, Double>>
            val similarityComputationTime = measureTimeMillis {
                targetPivotsWithSimilarity = validMovePivots
                    .filter { methodToMove.containingClass?.qualifiedName != it.psiClass.qualifiedName }
                    .map { pivot ->
                        val similarity = PsiUtils.computeCosineSimilarity(methodToMove, pivot.psiClass)
                        pivot to similarity
                    }
            }
            val targetPivotsSorted = targetPivotsWithSimilarity
                .sortedByDescending { it.second }
                .distinctBy { it.first.psiClass.name + it.first.psiElement?.text }
                .map { it.first }
            telemetryDataManager
                ?.addPotentialTargetClassesOrdered(
                    methodToMove.name,
                    targetPivotsWithSimilarity.map{it.first.psiClass.name to it.second},
                    "cosine",
                    similarityComputationTime)

            val pivotsSortedByLLM =
                if (llmChatModel!=null)
                    rerankByLLM(targetPivotsSorted,
                        methodToMove,
                        methodToMove.containingClass,
                        project,
                        llmChatModel,
                        telemetryDataManager)?: targetPivotsSorted
                else
                    targetPivotsSorted
            if (pivotsSortedByLLM.isEmpty())
                return emptyList()
            logPotentialPivots(pivotsSortedByLLM.subList(0, min(3, pivotsSortedByLLM.size)), methodToMove)

            if (PsiUtils.isMethodStatic(methodToMove)){
                val moveMethods = pivotsSortedByLLM.map {
                    it.psiClass.qualifiedName?.let { it1 ->
                        MyMoveStaticMethodRefactoring(
                            methodToMove.startLine(editor.document),
                            methodToMove.endLine(editor.document),
                            methodToMove, it1,
                            rationale = it.rationale
                        )
                    }
                }.filterNotNull()
                if (moveMethods.isEmpty())
                    return emptyList()
                return moveMethods.subList(0, min(TOPN_SUGGESTIONS4USER, moveMethods.size))
            }

            val moveMethods = pivotsSortedByLLM
                .map {
                    if (it.psiElement!=null) {
                        val processor = runReadAction {
                            MoveInstanceMethodProcessor(
                                project, methodToMove, it.psiElement as PsiVariable, "public",
                                runReadAction {
                                    getParamNamesIfNeeded(
                                        MoveInstanceMembersUtil.getThisClassesToMembers(methodToMove),
                                        it.psiElement as? PsiField
                                    )
                                }
                            )
                        }
                        MyMoveMethodRefactoring(
                            methodToMove.startLine(editor.document),
                            methodToMove.endLine(editor.document),
                            methodToMove,
                            processor,
                            classToMoveTo = it.psiClass,
                            rationale = it.rationale
                        )
                    }else {
                        null
                    }
                }
                .filterNotNull()
            if (moveMethods.isEmpty())
                return emptyList()
            return moveMethods.subList(0, min(TOPN_SUGGESTIONS4USER, moveMethods.size)) // choose top-3 moves
        }

        private fun getValidPivots(
            project: Project,
            editor: Editor,
            file: PsiFile,
            methodToMove: PsiMethod,
            targetPivots: List<MovePivot>
        ): List<MovePivot> {
            return targetPivots.filter {
                if (PsiUtils.isMethodStatic(methodToMove))
                    checkStaticMoveValidity(project, methodToMove, it)
                else
                    checkInstanceMoveValidity(project, methodToMove, it)
            }
        }

        private fun checkInstanceMoveValidity(
            project: Project,
            methodToMove: PsiMethod,
            it: MovePivot
        ): Boolean {
            val processor = MoveInstanceMethodProcessorAutoValidator(
                project, methodToMove, it.psiElement as PsiVariable, "public",
                runReadAction {
                    getParamNamesIfNeeded(
                        MoveInstanceMembersUtil.getThisClassesToMembers(methodToMove),
                        it.psiElement as? PsiField
                    )
                }
            )
            // Reflection. This is a hacky way to call intellij API.
            val method = processor.javaClass.getDeclaredMethod("findUsages")
            method.setAccessible(true)
            val usages = method.invoke(processor)
            val refUsages = Ref<Array<UsageInfo>>(usages as Array<UsageInfo>)

            val preprocessUsagesMethod =
                processor.javaClass.getDeclaredMethod("preprocessUsages", refUsages::class.java)
            preprocessUsagesMethod.setAccessible(true)
            return preprocessUsagesMethod.invoke(processor, refUsages) as Boolean
        }

        private fun logPotentialPivots(
            targetPivotsSorted: List<MovePivot>,
            methodToMove: PsiMethod
        ) {
            ApplySuggestRefactoringIntention.log2fileAndViewer(
                "Found potential target class(s) for ${methodToMove.name}", Logger.getInstance(this::class.java))
            ApplySuggestRefactoringIntention.log2fileAndViewer(
                "Potential Target Classes -> ${
                    targetPivotsSorted.distinctBy { it.psiClass.name }.map{it.psiClass.name}.joinToString(", ")
                }",
                Logger.getInstance(this::class.java)
            )
        }

        private fun rerankByLLM(
            targetPivotsSorted: List<MovePivot>,
            methodToMove: PsiMethod,
            containingClass: PsiClass?,
            project: Project,
            llmChatModel: ChatLanguageModel,
            telemetryDataManager: EFTelemetryDataManager?
        ) : List<MovePivot>?{

            val response: LLMBaseResponse?
            val llmResponseTime = measureTimeMillis {
                response = llmResponseCache[methodToMove.text] ?: sendChatRequest(
                    project,
                    MoveMethodRefactoringPrompt().askForTargetClassPriorityPrompt(
                        methodToMove.text,
                        targetPivotsSorted.distinctBy { it.psiClass.name }
                    ),
                    llmChatModel)
            }
            if (response!=null){
                llmResponseCache[methodToMove.text]?: llmResponseCache.put(methodToMove.text, response)
                try {
                    val priorityOrder =
                        (JsonParser.parseString(JsonUtils.sanitizeJson(response.getSuggestions()[0].text)) as JsonArray)
                        .map { try{ Gson().fromJson(it, MoveSuggestion::class.java) } catch (e: Exception){null} }
                        .filterNotNull()
                    val targetClassesRanked = priorityOrder.map { it.targetClassName }
                    priorityOrder.forEach {
                        moveSuggestion -> targetPivotsSorted
                            .filter { it.psiClass.name == moveSuggestion.targetClassName}
                            .forEach { it.rationale=moveSuggestion.rationale }
                    }
                    val llmSortedPivots = targetPivotsSorted.sortedBy {
                        val index = targetClassesRanked.indexOf(it.psiClass.name)
                        if (index == -1){
                            targetPivotsSorted.size+1
                        }else {
                            index
                        }
                    }
                    telemetryDataManager?.addLlmTargetClassPriorityResponse(
                        methodToMove.name,
                        llmSortedPivots.map{it.psiClass.name}.filterNotNull(),
                        llmResponseTime
                    )
                    return llmSortedPivots
                } catch (e: Exception) {
                    telemetryDataManager?.addLlmTargetClassPriorityResponse(
                        methodToMove.name,
                        response.getSuggestions()[0].text,
                        llmResponseTime
                    )
                    e.printStackTrace()
                }
            }
            return null
        }

        private fun getPotentialMovePivots(project: Project, editor: Editor, file: PsiFile, methodToMove: PsiMethod): List<MovePivot> {
            if (methodToMove.containingClass==null) return emptyList()
            if (PsiUtils.isMethodStatic(methodToMove)
                && MoveMembersPreConditions.checkPreconditions(project, arrayOf(methodToMove), null, null)){
                return (PsiUtils.fetchClassesInPackage(methodToMove.containingClass!!, project) + PsiUtils.fetchImportsInFile(file, project))
                    .map { MovePivot(it, null) }
            }else{
                val handler = MoveInstanceMethodHandlerForPlugin()
                handler.invoke(project, arrayOf(methodToMove), null)
                return handler.suitableVariablesToMove.map {
                    val clazz = PsiUtils.findClassFromQualifier(it.type.canonicalText, project)
                    if (clazz!=null)
                        MovePivot(clazz, it)
                    else
                        null
                }.filterNotNull()
            }
        }

        fun tryMoveToClass(
            methodToMove: PsiMethod,
            targetClassName: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring>{

            if (PsiUtils.isMethodStatic(methodToMove)) {
                val qualifiedClassName = PsiUtils.getQualifiedTypeInFile(
                    methodToMove.containingFile, targetClassName
                )
                if (qualifiedClassName!=null){
                    return listOf(
                        MyMoveStaticMethodRefactoring(
                            methodToMove.startLine(editor.document),
                            methodToMove.endLine(editor.document),
                            methodToMove, qualifiedClassName)
                    )
                }
            }else{
                val variableOfType = PsiUtils.getVariableOfType(methodToMove, targetClassName)
                if (variableOfType!=null){
//                    return createMoveMethodRefactorings(variableOfType, project, methodToMove, editor)
                }
            }

            return listOf()
        }

        override val logicalName: String
            get() = "Move Method"
        override val apiFunctionName: String
            get() = "move_method"
        override val APIDocumentation: String
            get() = "def move_method(method_name, target_variable_name):\n" +
                    "    \"\"\"\n" +
                    "    Moves a method from its current class or context to a target class or object.\n" +
                    "\n" +
                    "    This function refactors code by moving a method identified by `method_name` from its original class or context\n" +
                    "    to a target object identified by `target_variable_name`. It assumes that the necessary updates to the\n" +
                    "    source code are handled externally.\n" +
                    "\n" +
                    "    Parameters:\n" +
                    "    - method_name (str): The name of the method to be moved.\n" +
                    "    - target_variable_name (str): The name of the target object to which the method should be moved.\n" +
                    "    \"\"\""



        class MyMoveMethodRefactoring(
            override val startLoc: Int,
            override val endLoc: Int,
            val methodToMove: PsiMethod,
            val processor: MoveInstanceMethodProcessor,
            val classToMoveTo: PsiClass,
            val rationale: String? = null
        ) : AbstractRefactoring(){

            init {
                description = "Move method to ${classToMoveTo.qualifiedName}\n" +
                        "Rationale: $rationale"
            }
            override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
                super.performRefactoring(project, editor, file)
                processor.run()
                reverseRefactoring = getReverseRefactoringObject(project, editor, file)
            }

            override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
                isValid = methodToMove.isPhysical
                return isValid!!
            }

            override fun getRefactoringPreview(): String {
                return "Move method ${methodToMove.name} to class ${classToMoveTo.name}"
            }

            override fun getStartOffset(): Int {
                return methodToMove.startOffset
            }

            override fun getEndOffset(): Int {
                return methodToMove.endOffset
            }

            override fun getReverseRefactoringObject(
                project: Project,
                editor: Editor,
                file: PsiFile
            ): AbstractRefactoring? {
                TODO("Not yet implemented")
            }

            override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
                if (isValid==true)
                    return this
                return null // TODO: implement search
            }

        }

        private fun checkStaticMoveValidity(project: Project,
                                            methodToMove: PsiMethod,
                                            movePivot: MovePivot) : Boolean {
            if (methodToMove.containingClass!!.name==movePivot.psiClass.name)
                return false
            val processor = MoveStaticMethodValidator(project, methodToMove.containingClass!!, movePivot.psiClass, methodToMove)
            val method = processor.javaClass.getDeclaredMethod("findUsages")
            method.setAccessible(true)
            val usages = method.invoke(processor)
            val refUsages = Ref<Array<UsageInfo>>(usages as Array<UsageInfo>)

            val preprocessUsagesMethod =
                processor.javaClass.getDeclaredMethod("preprocessUsages", refUsages::class.java)
            preprocessUsagesMethod.setAccessible(true)
            return preprocessUsagesMethod.invoke(processor, refUsages) as Boolean
        }

        private fun getParamNamesIfNeeded(
            myThisClassesMap: Map<PsiClass, Set<PsiMember>>,
            targetVariable: PsiField?
        ): MutableMap<PsiClass, String> {
            var parameterNames: MutableMap<PsiClass, String> = mutableMapOf()
            for (aClass in myThisClassesMap.keys) {

                val members = myThisClassesMap[aClass]
                if (targetVariable==null || members == null || members.size != 1 || !members.contains(targetVariable)) {
                    parameterNames.put(aClass, aClass.name!!.lowercase())
                }
                //Just the field is referenced
                // Skip adding parameter
            }
            return parameterNames
        }

    }

    class MyMoveStaticMethodRefactoring(
        override val startLoc: Int,
        override val endLoc: Int,
        val methodToMove: PsiMethod,
        val classToMoveTo: String,
        val rationale: String?=null
    ) : AbstractRefactoring(){
        val sourceClass: PsiClass = methodToMove.containingClass!!
        val methodName = methodToMove.name
        init {
            description = "move method to $classToMoveTo\n" +
                    "Rationale: $rationale"
        }

        override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
            val refFactory = JavaRefactoringFactoryImpl(project)
            val moveRefactoring =
                refFactory.createMoveMembers(
                    arrayOf(methodToMove),
                    classToMoveTo,
                    "public")
            moveRefactoring.run()
            reverseRefactoring = getReverseRefactoringObject(project, editor, file)
        }

        override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
            isValid = methodToMove.isPhysical
            return methodToMove.isPhysical
        }

        override fun getRefactoringPreview(): String {
            return "Move Static method ${methodToMove.name} to class ${classToMoveTo.split(".").last()}"
        }

        override fun getStartOffset(): Int {
            return methodToMove.startOffset
        }

        override fun getEndOffset(): Int {
            return methodToMove.endOffset
        }

        override fun getReverseRefactoringObject(
            project: Project,
            editor: Editor,
            file: PsiFile
        ): AbstractRefactoring? {

            val destClass = JavaPsiFacade.getInstance(project).findClass(
                classToMoveTo,
                GlobalSearchScope.projectScope(project))
            if (destClass!=null){
                val foundMethod = PsiUtils.getMethodNameFromClass(destClass, methodName)
                if (foundMethod!=null)
                    return MyMoveStaticMethodRefactoring(
                        foundMethod.startLine(editor.document),
                        foundMethod.endLine(editor.document),
                        foundMethod, sourceClass.qualifiedName!!)
            }
            return null
        }

        override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
            if (isValid==true)
                return this
            return null // TODO: Impl search
        }

    }

}