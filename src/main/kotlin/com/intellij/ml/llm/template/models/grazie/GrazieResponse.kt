package com.intellij.ml.llm.template.models.grazie

import com.google.gson.annotations.SerializedName
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.LLMResponseChoice
import com.intellij.ml.llm.template.models.ollama.OllamaMessage

data class GrazieResponse(
    @SerializedName("llm_response")
    val llmResponse: String,

    @SerializedName("status")
    val status: String
): LLMBaseResponse {
    override fun getSuggestions(): List<LLMResponseChoice> {
        return listOf(LLMResponseChoice(llmResponse, status))
    }
}
