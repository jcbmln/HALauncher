package xyz.mcmxciv.halauncher.activities.integration

import android.os.Build
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import xyz.mcmxciv.halauncher.BuildConfig
import xyz.mcmxciv.halauncher.models.Device
import xyz.mcmxciv.halauncher.repositories.IntegrationRepository

class IntegrationViewModel : ViewModel() {
    val integrationError: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val integrationSuccess: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    private val integrationExceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, exception.message.toString())
        integrationSuccess.value = false
        integrationError.value = "Unable to register device."
    }

    fun registerDevice() {
        val device = Device(
            BuildConfig.APPLICATION_ID,
            "HALauncher",
            BuildConfig.VERSION_NAME,
            Build.DEVICE,
            Build.MANUFACTURER,
            Build.MODEL,
            "Android",
            Build.VERSION.SDK_INT.toString(),
            false,
            null
        )

        viewModelScope.launch(integrationExceptionHandler) {
            IntegrationRepository().registerDevice(device)
            integrationSuccess.value = true
        }
    }

    companion object {
        private const val TAG = "IntegrationViewModel"
    }
}