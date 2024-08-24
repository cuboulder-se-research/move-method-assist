package com.intellij.ml.llm.template.benchmark

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.UncreatableRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.InlineMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.ml.llm.template.utils.MethodSignature.Companion.getMethodSignatureParts
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl

class CreateBenchmarkForFile(
//    val projectPath: String,
//    val refMinerOut: String,
    val filename: String,
    val project: Project, val editor: Editor, val file: PsiFile,
    val refactorings: JsonArray
) {


    fun create(){
        val allRefactoringObjects = mutableListOf<AbstractRefactoring>()
        for (refactoring in refactorings) {
            val refactoringsObjects = createReverseRefactoringObject(refactoring)
            allRefactoringObjects.addAll(refactoringsObjects)

        }
        // Execute all inverse refactorings
        executeReverse(allRefactoringObjects)
    }



    private fun createReverseRefactoringObject(refactoring: JsonElement): List<AbstractRefactoring> {
        val inverseRefactorings = when(refactoring.asJsonObject.get("type").asString){
            "Extract Method" -> getExtractMethodInverse(refactoring)
            "Rename Variable" -> getRenameVariableInverse(refactoring)
            "Rename Method" -> getRenameMethodInverse(refactoring)
            else -> {getUnsupportedRefactoring(refactoring)}
        }
        return inverseRefactorings

    }


    private fun getRenameMethodInverse(refactoring: JsonElement): List<AbstractRefactoring> {

        val refJsonObj = refactoring.asJsonObject
        val oldNewSplit = refJsonObj.get("description").asString
            .split("renamed to")
        val oldMethodSignature = getMethodSignatureParts(oldNewSplit[0])
        val newMethodSignature = getMethodSignatureParts(oldNewSplit[1])
        val psiClass = (file as PsiJavaFileImpl).classes
        if (psiClass.size==0)
            return emptyList()

        if (oldMethodSignature!=null && newMethodSignature!=null) {
            val refObj = RenameVariableFactory.renameMethod(
                newMethodSignature.methodName, psiClass[0], oldMethodSignature.methodName, newMethodSignature
            )
            if (refObj!=null)
                return listOf(refObj)
        }

        return emptyList()
    }

    private fun getRenameVariableInverse(refactoring: JsonElement): List<AbstractRefactoring> {
        val refJsonObj = refactoring.asJsonObject
        val methodSignature = refJsonObj.get("description").asString
            .split("in method")[1].split("from class")[0]
        val signature = getMethodSignatureParts(methodSignature)
        if (signature!=null){
            val oldName = refactoring.asJsonObject
                .get("leftSideLocations").asJsonArray
                .filter { it.asJsonObject.get("description").asString.equals("original variable declaration") }
                .map{ it.asJsonObject.get("codeElement").asString.split(":")[0].strip()}
                .get(0)

            val newName = refactoring.asJsonObject
                .get("rightSideLocations").asJsonArray
                .filter { it.asJsonObject.get("description").asString.equals("renamed variable declaration") }
                .map{ it.asJsonObject.get("codeElement").asString.split(":")[0].strip()}
                .get(0)

            val psiClass = (file as PsiJavaFileImpl).classes
            if (psiClass.size==0)
                return emptyList()
            val psiMethod = PsiUtils.getMethodWithSignatureFromClass(
                psiClass[0], signature
            )
            if (psiMethod!=null)
                return listOf(
                    RenameVariableFactory.fromOldNewName(
                        project, psiMethod, newName, oldName
                    )?:UncreatableRefactoring(1, 1, refJsonObj.get("description").asString)
                )
            return emptyList()
        }else{
            return emptyList()
        }
    }

    private fun getUnsupportedRefactoring(refactoring: JsonElement): List<AbstractRefactoring> {
        return listOf(
            UncreatableRefactoring(1, 1, refactoring.asJsonObject.get("description").asString)
        )
    }

    private fun getExtractMethodInverse(refactoring: JsonElement): List<AbstractRefactoring>{
        val extractedCodeElement = refactoring.asJsonObject
            .get("rightSideLocations").asJsonArray
            .filter { it.asJsonObject.get("description").asString.equals("extracted method declaration") }
            .map{ it.asJsonObject.get("codeElement").asString}
            .get(0)
        val signature = getMethodSignatureParts(extractedCodeElement)
        val psiClass = (file as PsiJavaFileImpl).classes
        if (psiClass.size==0 || signature==null)
            return emptyList()
        return InlineMethodFactory.fromMethodSignature(
            file, editor, signature)

    }

    private fun executeReverse(refObjects: List<AbstractRefactoring>){
        for (r in refObjects){
            if (!r.isValid(project, editor, file))
                r.recalibrateRefactoring(project, editor, file)
            try {
                r.performRefactoring(project, editor, file)
                // TODO: mark success
            } catch (e: Exception) {
                // TODO: mark failure.
                println("Failed to reverse ${r.getRefactoringPreview()}")
                e.printStackTrace()
            }
        }
    }
}