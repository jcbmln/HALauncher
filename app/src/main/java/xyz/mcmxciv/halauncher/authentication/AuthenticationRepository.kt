package xyz.mcmxciv.halauncher.authentication

import android.content.SharedPreferences
import androidx.core.content.edit
import xyz.mcmxciv.halauncher.authentication.models.Session
import xyz.mcmxciv.halauncher.authentication.models.Token
import xyz.mcmxciv.halauncher.utils.Serializer
import xyz.mcmxciv.halauncher.utils.deserialize
import xyz.mcmxciv.halauncher.utils.serialize
import javax.inject.Inject

class AuthenticationRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val authenticationApi: AuthenticationApi
) {
    val session: Session?
        get() {
            val sessionString = sharedPreferences.getString(SESSION_KEY, null)
            return sessionString?.let { Serializer.deserialize(it) }
        }

    suspend fun getToken(authenticationCode: String): Token {
        return authenticationApi.getToken(GRANT_TYPE_CODE, authenticationCode, CLIENT_ID)
    }

    fun saveSession(session: Session) {
        sharedPreferences.edit { putString(SESSION_KEY, Serializer.serialize(session)) }
    }

    companion object {
        const val SESSION_KEY = "session"
        const val CLIENT_ID = "https://halauncher.app"
        const val RESPONSE_TYPE = "code"
        const val GRANT_TYPE_CODE = "authorization_code"
        const val GRANT_TYPE_REFRESH = "refresh_token"
        const val REVOKE_ACTION = "revoke"
        const val REDIRECT_URI = "hass://auth"
    }
}
