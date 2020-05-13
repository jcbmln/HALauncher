package xyz.mcmxciv.halauncher.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class HomeAssistantMockWebServer<T>(private val c: Class<T>) {
    private val server = MockWebServer()
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val client = OkHttpClient.Builder()
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
    val api: T
        get() = retrofit.create(c)

    fun enqueue(code: Int, body: String? = null) {
        val response = MockResponse()
        if (body != null) response.setBody(body)
        server.enqueue(response.setResponseCode(code))
    }

    fun takeRequest() = server.takeRequest()
}