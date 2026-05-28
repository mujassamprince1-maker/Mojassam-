package com.example.data.remote

import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// --- Gemini Content Data Models (Moshi friendly) ---

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null
)

data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null
)

data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

// --- Retrofit API Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Singleton Network Client ---

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- Remote Assistant Helper ---

class GeminiRepository {
    suspend fun generateSmartReply(chatHistory: List<com.example.data.model.Message>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Get to bed early! 🌙" // A mock smart reply fallback if key is unconfigured
        }

        val promptBuilder = StringBuilder()
        promptBuilder.append("Based on the following last messages in a chat, generate a quick, natural short reply suggestion (just 1 to 5 words). Do not include formatting, just the reply itself.\n\n")
        chatHistory.takeLast(5).forEach { msg ->
            promptBuilder.append("${msg.senderName}: ${msg.text}\n")
        }
        promptBuilder.append("User:")

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = promptBuilder.toString())))
            ),
            generationConfig = GenerationConfig(temperature = 0.5f, maxOutputTokens = 30)
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() 
                ?: "Sounds perfect! 👍"
        } catch (e: Exception) {
            "Sounds perfect! 👍"
        }
    }

    suspend fun consultChatbot(history: List<Pair<String, String>>, prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Hi there! I am Privora's AI Chat Assistant. To get full responses, please configure your actual GEMINI_API_KEY in the Secrets Panel. Currently, I can tell you that Privora uses military-grade end-to-end symmetric state-based protection!"
        }

        val promptBuilder = java.lang.StringBuilder()
        promptBuilder.append("You are Privora AI, a secure, friendly, and helpful futuristic AI chat assistant inside Privora, the next-gen private messenger. Keep replies informative but concise.\n\n")
        history.forEach { (role, txt) ->
            promptBuilder.append("$role: $txt\n")
        }
        promptBuilder.append("User: $prompt\n")
        promptBuilder.append("Privora AI:")

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = promptBuilder.toString())))
            ),
            generationConfig = GenerationConfig(temperature = 0.7f, maxOutputTokens = 300)
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "I apologize, I encountered a connection issue. I'm here to safeguard your metadata!"
        } catch (e: Exception) {
            "Error querying AI engine: ${e.message}"
        }
    }

    suspend fun translateMessage(text: String, targetLanguage: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return when (targetLanguage) {
                "Spanish" -> "Traducido: $text"
                "French" -> "Traduit: $text"
                "German" -> "Übersetzt: $text"
                else -> "Translated: $text"
            }
        }

        val prompt = "Translate the following text into $targetLanguage. Provide ONLY the translated text, no introductory remarks or explanation:\n\n$text"
        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.1f)
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: text
        } catch (e: Exception) {
            text
        }
    }
}
