package com.intellij.ml.llm.template.models.grazie

import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage
import com.intellij.ml.llm.template.models.openai.OpenAiChatRequestBody
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.output.Response

class GrazieModel(
    val llm: String,
    val temperature: Double
): ChatLanguageModel {
    override fun generate(messages: MutableList<ChatMessage>?): Response<AiMessage> {
        if (messages != null) {
            val response = GrazieBaseRequest(
                OpenAiChatRequestBody(
                    llm,
                    messages.map{
                        OpenAiChatMessage(
                            if(it.type().name=="AI") "assistant" else it.type().name.lowercase(),
                            it.text())
                                },
                    temperature = temperature
                )
            ).sendSync()

            return Response(AiMessage.from(response?.getSuggestions()?.get(0)?.text ?: "no-response"))
        }
        return Response(AiMessage.from("no-response"))
    }
}

val GrazieGPT4 = GrazieModel("GPT-4", 0.5)