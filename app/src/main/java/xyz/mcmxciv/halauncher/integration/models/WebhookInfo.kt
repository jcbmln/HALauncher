package xyz.mcmxciv.halauncher.integration.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebhookInfo(
    @Json(name = "cloudhook_url")
    val cloudhookUrl: String?,
    @Json(name = "remote_ui_url")
    val remoteUiUrl: String?,
    @Json(name = "secret")
    val secret: String?,
    @Json(name = "webhook_id")
    val webhookId: String
)
