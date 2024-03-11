package com.intellij.ml.llm.template.models.ollama

import com.intellij.ml.llm.template.models.*
import com.intellij.ml.llm.template.models.openai.OpenAiChatRequestBody

class OllamaRequestProvider(completionModel: String,
                             editModel: String,
                             chatModel: String):
    LLMRequestProvider(completionModel, editModel, chatModel) {
    override fun createChatGPTRequest(body: OpenAiChatRequestBody): LLMBaseRequest<*> {
        return OllamalBaseRequest<OpenAiChatRequestBody>("chat", body)
    }
}


val MistralChatRequestProvider =
    OllamaRequestProvider("mistral:text", "mistral", "mistral:instruct")