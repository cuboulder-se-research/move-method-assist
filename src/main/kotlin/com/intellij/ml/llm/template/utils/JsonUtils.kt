package com.intellij.ml.llm.template.utils

class JsonUtils {
    companion object{
        fun sanitizeJson(jsonText: String) = jsonText.removePrefix("```json").removeSuffix("```")
    }
}