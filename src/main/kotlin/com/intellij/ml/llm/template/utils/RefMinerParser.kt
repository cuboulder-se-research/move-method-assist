package com.intellij.ml.llm.template.utils

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path

class RefMinerParser(
    filePath: Path,
    projectBasePath: Path,
    val refData: JsonElement = JsonParser.parseString(Files.readString(filePath))
) {
    lateinit var commitsData: JsonArray
    init {
        commitsData = refData.asJsonObject.getAsJsonArray("commits")
    }

    fun getStaticMoveMethods(){
//        val staticMethodCommits = listOf()
        commitsData.forEach{
            val refactorings = it.asJsonObject.getAsJsonArray("refactorings")
            val moveMethods = refactorings.filter {
                ref -> ref.asJsonObject.get("type").asString == "Move Method"
            }
            val staticMoveMethods = moveMethods.filter { isStaticMove(it.asJsonObject) }
        }
    }
    fun isStaticMove(jsonElement: JsonObject): Boolean{
        return true
    }
}