package com.intellij.ml.llm.template.prompts

import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage

abstract class MethodPromptBase {
    abstract fun getPrompt(methodCode: String): MutableList<OpenAiChatMessage>;
}