package com.intellij.ml.llm.template.benchmark

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.UncreatableRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.InlineMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl
import org.jetbrains.kotlin.j2k.getContainingClass
import java.nio.file.Files
import java.nio.file.Path

class CreateBenchmarkForFile(
//    val projectPath: String,
//    val refMinerOut: String,
    val filename: String,
    val project: Project, val editor: Editor, val file: PsiFile,
    val refactorings: JsonArray
) {

    fun create(){
//        val jsonContent = Files.readString(Path.of(refMinerOut))
//        val json = JsonParser.parseString(jsonContent)
//        for (filename in json.asJsonObject.keySet()) {
//            val commitInfo = json.asJsonObject[filename].asJsonArray[0]
//            val commitHash = commitInfo.asJsonObject["sha1"].asString

            // Checkout commit

            // Open file
//            openFile()

//            val refactorings = commitInfo.asJsonObject["refactorings"].asJsonArray
        val allRefactoringObjects = mutableListOf<AbstractRefactoring>()
        for (refactoring in refactorings) {
            val refactoringsObjects = createReverseRefactoringObject(refactoring)
            allRefactoringObjects.addAll(refactoringsObjects)

        }
        // Execute all inverse refactorings
        executeReverse(allRefactoringObjects)

            // Make commit and checkout branch/tag
//        }
    }



    private fun createReverseRefactoringObject(refactoring: JsonElement): List<AbstractRefactoring> {
        val inverseRefactorings = when(refactoring.asJsonObject.get("type").asString){
            "Extract Method" -> getExtractMethodInverse(refactoring)
            "Rename Variable" -> getRenameVariableInverse(refactoring)
            else -> {getUnsupportedRefactoring(refactoring)}
        }
        return inverseRefactorings

    }

    private fun getRenameVariableInverse(refactoring: JsonElement): List<AbstractRefactoring> {
        val refJsonObj = refactoring.asJsonObject
        val methodInformation = refJsonObj.get("description").asString
            .split("in method")[1].strip().split(" ")
        val modifier = methodInformation[0]
        val methodNameAndParams = methodInformation[1]
        val methodName = methodNameAndParams.split("(")[0]
        val returnType = methodInformation[3]
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
        return listOf(
            RenameVariableFactory.fromMethodOldNewName(
                project, psiClass[0], methodName, newName, oldName
            )?:UncreatableRefactoring(1, 1, refJsonObj.get("description").asString)
        )
    }

    private fun getUnsupportedRefactoring(refactoring: JsonElement): List<AbstractRefactoring> {
        return listOf(
            UncreatableRefactoring(1, 1, refactoring.asJsonObject.get("description").asString)
        )
    }

    private fun getExtractMethodInverse(refactoring: JsonElement): List<AbstractRefactoring>{
        val extractedMethodName = refactoring.asJsonObject
            .get("rightSideLocations").asJsonArray
            .filter { it.asJsonObject.get("description").asString.equals("extracted method declaration") }
            .map{ it.asJsonObject.get("codeElement").asString.split(":")[0].strip().split(" ")[1].split("(")[0]}
            .get(0)
//        val methodName = "";
        val psiClass = (file as PsiJavaFileImpl).classes
        if (psiClass.size==0)
            return emptyList()
        return InlineMethodFactory.fromMethodName(file, editor, extractedMethodName)

    }

    private fun executeReverse(refObjects: List<AbstractRefactoring>){
        for (r in refObjects){
            try {
                r.performRefactoring(project, editor, file)
                // mark success
            } catch (e: Exception) {
                // mark failure.
                print("Failed to reverse")
//                TODO("Not yet implemented")
            }
        }
        // TODO: mark status.
    }
}