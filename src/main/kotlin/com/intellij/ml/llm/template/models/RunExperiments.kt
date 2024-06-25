package com.intellij.ml.llm.template.models

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import com.intellij.ml.llm.template.models.grazie.GrazieGPT4RequestProvider
import com.intellij.ml.llm.template.prompts.SuggestRefactoringPrompt
import java.io.File

val num_iterations = 10

fun main() {
    RunExperiments(temperature = 0.5, iterations = 1, moveMethodsFile = "/data/move_methods_selected.csv").runAllAndSave()
}

class RunExperiments(val temperature: Double, val iterations: Int, val moveMethodsFile: String) {
    val results = mutableMapOf<String, MutableMap<String, String>>()
    val logBatches: Int = 10
    var filename: File? = null
    init {
        filename = getFileName()
    }

    fun setResult(id: String, iteration: Int, llmResponse: String){
        val idKey = "ID_$id"
        val mutableMap = results[idKey]
        val key = "iteration-$iteration"
        if (mutableMap !=null){
            mutableMap[key] = llmResponse
        }else{
            results[idKey] = mutableMapOf(Pair(key, llmResponse))
        }
    }

    fun runAllAndSave(){
        val fileContents = readFile()
        // id -> iteration -> llm_response
        var count = 0
        if (fileContents != null) {
            for (row in fileContents) {
                count+=1
                if (count < 20){
                    continue
                }
                val classSource = row["file_contents"]!!
                val id = row["ID"]!!
                val prompt_messages = SuggestRefactoringPrompt().getPrompt(classSource)
                println(id)
                for (i in 1..iterations){
                    println("Iteration: $i")
                    val response = try {
                        sendChatRequest(
                            prompt_messages,
                            GrazieGPT4RequestProvider.chatModel,
                            GrazieGPT4RequestProvider,
                            temperature = temperature
                        )
                    } catch (e: Exception) {
                        println("Failed to send request to LLM.")
                        continue
                    }
                    val suggestions = response?.getSuggestions()?.get(0)
                    setResult(id, i, suggestions?.text?:"no response")
                }
                if (count%logBatches==0){
                    saveResults()
                }
            }
            saveResults()
        }
    }

    fun readFile(): List<Map<String, String>>? {
        val fileText = RunExperiments::class.java.getResource(moveMethodsFile)?.readText()

        if (fileText!=null)
            return csvReader().readAllWithHeader(fileText)
//            csvReader().open(fileText) {
//                return@open readAllAsSequence()

        return null
    }

    fun saveResults(){
        println("Saving results.")
        val contents = Gson().toJson(results).toString()
        filename!!.printWriter().use { it.print(contents) }
    }

    private fun getFileName(): File {
        var fileName = File("moveMethodResponses.json")
        var count = 1
        while (fileName.exists()) {
            fileName = File("moveMethodResponses($count).json")
            count += 1
        }
        return fileName
    }


}

