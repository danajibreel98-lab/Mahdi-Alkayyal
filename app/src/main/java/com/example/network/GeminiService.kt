package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ======================== API MODELS ========================

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

// ======================== API INTERFACE ========================

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// ======================== API CLIENT ========================

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(65, TimeUnit.SECONDS)
        .readTimeout(65, TimeUnit.SECONDS)
        .writeTimeout(65, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun generatePrompt(prompt: String, systemPrompt: String = ""): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            return "عذراً، لم يتم العثور على مفتاح API الخاص بـ Gemini في إعدادات التطبيق الأمني. يرجى تهيئة المفتاح في لوحة الأسرار (Secrets panel) باسم GEMINI_API_KEY لتفعيل الذكاء الاصطناعي."
        }

        val systemInst = if (systemPrompt.isNotEmpty()) {
            Content(parts = listOf(Part(text = systemPrompt)))
        } else {
            Content(parts = listOf(Part(text = "أنت الخبير التقني المساعد للمركز الفلسطيني للأعمال المتعلقة بالألغام (PMAC). أجب باللغة العربية بمهنية ودقة واختصار لتنفيذ المهام الميدانية والتحليلات الإدارية.")))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemInst,
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "خطأ: لم يتم تلقي رد مناسب من نموذج الذكاء الاصطناعي لمكافحة الألغام."
        } catch (e: Exception) {
            "فشل في استدعاء الذكاء الاصطناعي لوقوع خطأ في الاتصال أو عدم توفر الشبكة: ${e.localizedMessage ?: e.message}"
        }
    }
}
