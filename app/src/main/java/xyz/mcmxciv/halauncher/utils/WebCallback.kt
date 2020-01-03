package xyz.mcmxciv.halauncher.utils

sealed class WebCallback(open val callback: String) {
    data class AuthCallback(override val callback: String) : WebCallback(callback)
    data class RevokeAuthCallback(override val callback: String) : WebCallback(callback)
}