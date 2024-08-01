package com.intellij.ml.llm.template.prompts

import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage

abstract class MethodPromptBase {
    abstract fun getPrompt(methodCode: String): MutableList<ChatMessage>;
}