package com.intellij.ml.llm.template.models

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import com.intellij.ml.llm.template.models.grazie.GrazieGPT4RequestProvider
import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage
import com.intellij.ml.llm.template.prompts.GetRefactoringObjParametersPrompt
import com.intellij.ml.llm.template.prompts.SuggestRefactoringPrompt
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.refactoringobjects.movemethod.MoveMethodFactory
import com.intellij.ml.llm.template.suggestrefactoring.AbstractRefactoringValidator
import com.intellij.ml.llm.template.suggestrefactoring.AtomicSuggestion
import com.intellij.ml.llm.template.suggestrefactoring.RefactoringSuggestion
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
        var fileName = File("data/moveMethodResponses.json")
        var count = 1
        while (fileName.exists()) {
            fileName = File("data/moveMethodResponses($count).json")
            count += 1
        }
        return fileName
    }
}

class GetRefactoringObjects(
    val responsesFilePath: String,
    val classSourcesFilePath: String,
    val apiResponsesFilePath:String?=null){

    val responses = mutableMapOf<String, MutableMap<String, String>>()
    val apiResponses = mutableMapOf<String, String>()
    val classSources = mutableMapOf<String, String>()
    val logBatches: Int = 10
    var apiResponsesFile: File? = null

    init {
        apiResponsesFile = getDestFile()
    }
    fun loadData(){
        val fileContents = File(responsesFilePath).readText()
        val jsonContent = Gson().fromJson(fileContents, responses.javaClass)
        responses.putAll(jsonContent)

        val fileContentsSources = File(classSourcesFilePath).readText()
        classSources.putAll(Gson().fromJson(fileContentsSources, classSources.javaClass))

        if (apiResponsesFile!=null && apiResponsesFile!!.exists()){
            apiResponses.putAll(
                Gson().fromJson(apiResponsesFile!!.readText(), apiResponses.javaClass)
            )
        }
    }

    fun getApiResponses(){
        var count = 0
        for (id in responses.keys){
            print("Completed: $count")
            count+=1
            for(iteration in responses[id]!!.keys) {

                val key = "$id-$iteration"
                if (apiResponses.get(key)!=null) {
                    println("Already got response.")
                    continue
                }
                val llmResponseText = responses[id]!![iteration]!!
                val refactoringSuggestion = try {
                    AbstractRefactoringValidator.getRawSuggestions(llmResponseText)
                } catch (e: Exception) {
                    print("Failed to parse json.")
                    continue
                }
                var apis = ""
                for (atomicSuggestion in refactoringSuggestion.improvements) {
                    val messageList: MutableList<OpenAiChatMessage> =
                        setupOpenAiChatMessages(atomicSuggestion, MoveMethodFactory, id)
                    val response = try {
                        sendChatRequest(
                            messageList,
                            GrazieGPT4RequestProvider.chatModel,
                            GrazieGPT4RequestProvider,
                            temperature = 0.5
                        )
                    } catch (e: Exception) {
                        println("Failed to send request to LLM.")
                        continue
                    }
                    val suggestions = response?.getSuggestions()?.get(0)
                    apis += (suggestions?.text + "\n")
                }
                apiResponses[key] = apis
            }

            if (count% logBatches==0){
                writeResults()
            }
        }
    }

    fun run(){
        loadData()
        getApiResponses()
        writeResults()
    }


    private fun setupOpenAiChatMessages(
        atomicSuggestion: AtomicSuggestion,
        refactoringFactory: MyRefactoringFactory,
        id: String
    ): MutableList<OpenAiChatMessage> {
        val messageList: MutableList<OpenAiChatMessage> = mutableListOf()
        val basePrompt = SuggestRefactoringPrompt().getPrompt(classSources[id]!!)
        messageList.addAll(basePrompt)
        messageList.add(
            OpenAiChatMessage(
                "assistant",
                Gson().toJson(RefactoringSuggestion(mutableListOf(atomicSuggestion))).toString()
            )
        )

        messageList.addAll(
            GetRefactoringObjParametersPrompt.get(
                atomicSuggestion.shortDescription +
                        "Line: ${atomicSuggestion.start} to ${atomicSuggestion.end}",
                refactoringFactory.logicalName,
                refactoringFactory.APIDocumentation)
        )
        return messageList
    }
    private fun getDestFile(): File {
        if (apiResponsesFilePath!=null){
            return File(apiResponsesFilePath)
        }
        var fileName = File("data/apiResponses.json")
        var count = 1
        while (fileName.exists()) {
            fileName = File("data/apiResponses($count).json")
            count += 1
        }
        return fileName
    }
    fun writeResults(){
        println("Saving results.")
        val contents = Gson().toJson(apiResponses).toString()
        apiResponsesFile!!.printWriter().use { it.print(contents) }
    }

}

