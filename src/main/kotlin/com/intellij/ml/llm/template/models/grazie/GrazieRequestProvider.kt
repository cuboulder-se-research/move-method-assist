package com.intellij.ml.llm.template.models.grazie

import com.intellij.ml.llm.template.models.LLMBaseRequest
import com.intellij.ml.llm.template.models.LLMRequestProvider
import com.intellij.ml.llm.template.models.openai.OpenAiChatRequestBody

class GrazieRequestProvider(completionModel: String,
                            editModel: String,
                            chatModel: String):
    LLMRequestProvider(completionModel, editModel, chatModel) {
    override fun createChatGPTRequest(body: OpenAiChatRequestBody): LLMBaseRequest<*> {
        return GrazieBaseRequest(body)
    }
}


val GrazieGPT4RequestProvider =
    GrazieRequestProvider("GPT-4", "GPT-4", "GPT-4")