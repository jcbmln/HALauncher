package xyz.mcmxciv.halauncher.activities.authentication

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import xyz.mcmxciv.halauncher.repositories.AuthenticationRepository

class AuthenticationViewModel : ViewModel() {
    val authenticationError: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val authenticationSuccess: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>().also {
            it.value = false
        }
    }

    private val authenticationExceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, exception.message.toString())
        authenticationError.value = "Authentication failed."
        authenticationSuccess.value = false
    }

    fun shouldRedirect(url: String): Boolean {
        val code = Uri.parse(url).getQueryParameter(AuthenticationRepository.RESPONSE_TYPE)
        return if (url.contains(AuthenticationRepository.REDIRECT_URI) && !code.isNullOrBlank()) {
            viewModelScope.launch(authenticationExceptionHandler) {
                AuthenticationRepository().setAuthToken(code)
                authenticationSuccess.value = true
            }

            true
        }
        else {
            false
        }
    }

    companion object {
        private const val TAG = "AuthenticationViewModel"
    }
}