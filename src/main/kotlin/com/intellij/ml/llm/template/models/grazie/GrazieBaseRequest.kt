package com.intellij.ml.llm.template.models.grazie

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
//import ai.grazie.api.gateway.client.api.llm.LlmAPIClient
import ai.grazie.auth.model.GrazieUserToken
import ai.grazie.client.HTTPClientTransform
//import ai.grazie.client.common.SuspendableClientWithBackoff
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.jdk.GrazieJdkHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.auth.GrazieAgent
//import ai.grazie.model.auth.application.GrazieApplication
//import ai.grazie.model.auth.application.GrazieApplicationType
import ai.grazie.model.auth.v5.AuthData
import ai.grazie.model.cloud.AuthType
import ai.grazie.model.llm.chat.LLMChat
import ai.grazie.model.llm.chat.LLMChatMessage
import ai.grazie.model.llm.chat.LLMChatRole
import ai.grazie.model.llm.parameters.OpenAILLMParameters
import ai.grazie.model.llm.profile.LLMProfileID
//import ai.grazie.model.llm.chat.v5.LLMChat
//import ai.grazie.model.llm.chat.v5.LLMChatMessage
//import ai.grazie.model.llm.chat.v5.LLMChatUserMessage
//import ai.grazie.model.llm.data.stream.LLMStreamData
import ai.grazie.model.llm.profile.OpenAIProfileIDs
import ai.grazie.utils.attributes.Attributes
//import ai.grazie.model.llm.prompt.LLMPromptID
//import ai.grazie.model.task.library.text.TextImproveTask
//import ai.grazie.model.task.library.text.TextParaphraseTask
import ai.grazie.utils.jwt.JWTToken
import com.intellij.ml.llm.template.models.LLMBaseRequest
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.openai.OpenAiChatRequestBody
import io.ktor.client.*
import io.ktor.util.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.kdoc.insert
import kotlin.math.absoluteValue

class GrazieBaseRequest(body: OpenAiChatRequestBody) : LLMBaseRequest<OpenAiChatRequestBody>(body)  {
    private val url = "https://api.app.stgn.grazie.aws.intellij.net"
    private val grazieToken = System.getenv("GRAZIE_JWT_TOKEN")
    private val authData = AuthData(
        token = grazieToken,
        originalUserToken = null,
        originalServiceToken = grazieToken,
        grazieAgent = GrazieAgent("suggest-refactoring-research", "0.1"),
    )
    private val client = SuspendableAPIGatewayClient(
        serverUrl = url,
        authType = AuthType.Service,
        httpClient = SuspendableHTTPClient.WithV5(GrazieKtorHTTPClient.Default, authData)
    )
    fun something(){
//
//        GrazieUserToken(JWTToken(""))
//        AuthType.Service
//        TextParaphraseTask.LLM.Grazie
//
//        GrazieJdkHTTPClient.Default



//        SuspendableHTTPClient.WithV5(GrazieJdkHTTPClient.Default)


        var fullString: String = ""




//        client = GrazieApiGatewayClient(
//            grazie_agent=GrazieAgent(name="llm4-function-improvements", version="dev"),
//            url=GrazieApiGatewayUrls.STAGING,
//            auth_type=AuthType.SERVICE,
//            grazie_jwt_token=os.environ["GRAZIE_JWT_TOKEN"],
//        )
//
//        response = client.chat(
//            chat=build_chat_prompt(chat_messages),
//            profile=LLM_MAPPING.get(llm_name, Profile.OPENAI_GPT_4),
//            parameters={
//                LLMParameters.Temperature: Parameters.FloatValue(temperature)
//            },
//            prompt_id='v1'
//        )
    }

    private fun getChatMessages(): LLMChat{

        val llmChatArray: MutableList<LLMChatMessage> = mutableListOf()
        for(message in body.messages){
            val chatRole= when(message.role){
                "user"-> LLMChatRole.User
                "assistant"-> LLMChatRole.Assistant
                "system" -> LLMChatRole.System
                else -> {throw Exception("Unknown role")}
            }

            llmChatArray+=LLMChatMessage(chatRole, message.content)

        }
        return LLMChat(llmChatArray.toTypedArray())
    }

    private fun getOpenAIProfileId(): LLMProfileID{
        return when(body.model.uppercase()){
            "GPT-4"-> OpenAIProfileIDs.GPT4
            "GPT-3.5-TURBO"-> OpenAIProfileIDs.ChatGPT
            else -> OpenAIProfileIDs.ChatGPT
        }
    }

    private fun getAttributes(): Attributes{
        return Attributes(
            mutableMapOf
                (
                Pair(
                    OpenAILLMParameters.Chat.Temperature,
                    Attributes.Value.Double(body.temperature?:0.5)
                )
            )
        )
    }

    override fun sendSync(): LLMBaseResponse? {

        val response = runBlocking {
            try {
                val response = client.llm().chat(
                    getChatMessages(),
                    getOpenAIProfileId(),
//                    getAttributes()
                )
                println( response.toString())
                var finalString: String=""
//                response.collect()
                response.collect{
                    finalString+=it
                    return@collect
                }
                println(finalString)
                return@runBlocking GrazieResponse(finalString, "completed")
            } catch (e: Exception){
                println("Couldn't make request.")
//                return@runBlocking GrazieResponse("", "failed")
                throw e
            }
        }

        return response

    }
}