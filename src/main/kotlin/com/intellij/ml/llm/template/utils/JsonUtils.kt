package com.intellij.ml.llm.template.utils

class JsonUtils {
    companion object{
        fun sanitizeJson(jsonText: String): String {
            val t1 =  jsonText.removePrefix("```json").removeSuffix("```")
            if (t1.contains("```json")){
                return t1.split("```json")[1].split("```")[0]
            }
            if (t1.contains("```JSON")){
                return t1.split("```JSON")[1].split("```")[0]
            }
            if (t1.contains("```")){
                return t1.split("```")[1]
            }
            return t1
        }
    }
}