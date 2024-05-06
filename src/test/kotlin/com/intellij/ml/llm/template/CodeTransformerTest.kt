package com.intellij.ml.llm.template

import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFSuggestion
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFApplicationResult
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFCandidateFactory
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethodFactory
import com.intellij.ml.llm.template.utils.EFObserver
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import junit.framework.TestCase

class CodeTransformerTest : LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }

    fun `test failed extract function candidates are reported correctly`() {
        val codeTransformer = CodeTransformer()
        val efObserver = EFObserver()
        codeTransformer.addObserver(efObserver)

        configureByFile("/testdata/KafkaAdminClientTest.java")
        val efs = EFSuggestion(
            functionName = "createPartitionMetadata",
            lineStart = 113,
            lineEnd = 119
        )
        val efCandidates = EFCandidateFactory().buildCandidates(efs, editor, file)

        val funcCall = "extract_method(113, 119, \"createPartitionMetadata\")"
        val emObj = ExtractMethodFactory.createObjectFromFuncCall(
            funcCall, project, editor, file
        )

        emObj.performRefactoring(
            project, editor, file
        )


//        efCandidates.forEach { candidate ->
//            configureByFile("/testdata/KafkaAdminClientTest.java")
//            TODO("apply candidate")
//            codeTransformer.applyCandidate(candidate, project, editor, file)
//        }

        val failedNotifications = efObserver.getNotifications(EFApplicationResult.FAIL)
        val successNotifications = efObserver.getNotifications(EFApplicationResult.OK)
        TestCase.assertEquals(1, failedNotifications.size)
        TestCase.assertEquals(1, successNotifications.size)
    }
}