package com.intellij.ml.llm.template.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

class CodeBertScore {
    @Serializable
    private data class CodeBertRequest(val text1: String, val text2: String)

    @Serializable
    private data class CodeBertResponse(val score: Double)

    companion object {
        fun computeCodeBertScore(psiMethod: PsiMethod, psiClass: PsiClass): Double {
            val methodBody = psiMethod.text
            val classBody = psiClass.text

            return computeCodeBertScore(methodBody, classBody)
        }

        private fun computeCodeBertScore(text1: String, text2: String): Double {
            val client = OkHttpClient()

            val requestData = CodeBertRequest(text1, text2)
            val jsonBody = Json.encodeToString(requestData)

            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)

            val request = Request.Builder()
                .url("https://7fce-141-142-254-149.ngrok-free.app/compute_codebertscore")
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseBody = response.body?.string()
                    val codeBertResponse = Json.decodeFromString<CodeBertResponse>(responseBody ?: "")
                    return codeBertResponse.score
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return -1.0 // Return a default value or handle the error as needed
            }
        }
    }
}