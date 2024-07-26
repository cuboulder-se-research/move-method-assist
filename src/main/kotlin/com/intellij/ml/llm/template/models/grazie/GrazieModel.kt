package com.intellij.ml.llm.template.models.grazie

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.output.Response

class GrazieModel(
    val llm: String
): ChatLanguageModel {
    override fun generate(messages: MutableList<ChatMessage>?): Response<AiMessage> {
        val grazieObj = GrazieRequestProvider(llm, llm, llm)
        grazieObj.createChatGPTRequest(

        )

    }
}