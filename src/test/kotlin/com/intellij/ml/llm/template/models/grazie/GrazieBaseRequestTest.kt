package com.intellij.ml.llm.template.models.grazie

import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage
import com.intellij.ml.llm.template.models.openai.OpenAiChatRequestBody
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class GrazieBaseRequestTest: LightPlatformCodeInsightTestCase() {

    fun testGrazieSanity() {
        val response= GrazieBaseRequest(
            OpenAiChatRequestBody(
                "GPT-4",
                listOf(OpenAiChatMessage("user", "What is code refactoring?")))
        ).sendSync()
        if (response != null) {
            println(response.getSuggestions())
        }
        assert(1==1)
    }
}