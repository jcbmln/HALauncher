package xyz.mcmxciv.halauncher.activities.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import org.json.JSONObject
import xyz.mcmxciv.halauncher.AppListAdapter
import xyz.mcmxciv.halauncher.databinding.ActivityHomeBinding
import xyz.mcmxciv.halauncher.utils.AppPreferences

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var prefs: AppPreferences
    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        prefs = AppPreferences.getInstance(this)
        viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        setContentView(binding.root)
        initializeWebView()

//        binding.homeAppBar.appList.layoutManager = LinearLayoutManager(this)

        viewModel.externalAuthCallback.observe(this, Observer {
            binding.homeWebView.evaluateJavascript(
                "${it.first}(true, ${it.second});",
                null
            )
        })

        viewModel.externalAuthRevokeCallback.observe(this, Observer {
            binding.homeWebView.evaluateJavascript("$it(true);", null)
            prefs.isAuthenticated = false
        })

//        viewModel.appList.observe(this, Observer {
//            binding.homeAppBar.appList.adapter = AppListAdapter(it)
//        })
    }

    private fun initializeWebView() {
        binding.homeWebView.apply {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()

            addJavascriptInterface(object : Any() {
                @JavascriptInterface
                fun getExternalAuth(result: String) {
                    viewModel.getExternalAuth(JSONObject(result).get("callback") as String)
                }

                @JavascriptInterface
                fun revokeExternalAuth(result: String) {
                    viewModel.revokeExternalAuth(JSONObject(result).get("callback") as String)
                }

//                @JavascriptInterface
//                fun externalBus(message: String) {
//                    Log.d(TAG, "External bus $message")
//                    binding.homeWebView.post {
//                        when {
//                            JSONObject(message).get("type") == "config/get" -> {
//                                val script = "externalBus(" +
//                                        "${JSONObject(
//                                            mapOf(
//                                                "id" to JSONObject(message).get("id"),
//                                                "type" to "result",
//                                                "success" to true,
//                                                "result" to JSONObject(mapOf("hasSettingsScreen" to true))
//                                            )
//                                        )}" +
//                                        ");"
//                                Log.d(TAG, script)
//                                binding.homeWebView.evaluateJavascript(script) {
//                                    Log.d(TAG, "Callback $it")
//                                }
//                            }
////                            JSONObject(message).get("type") == "config_screen/show" ->
////                                startActivity(SettingsActivity.newInstance(this@WebViewActivity))
//                        }
//                    }
//                }
            }, "externalApp")
        }

        binding.homeWebView.loadUrl(viewModel.buildUrl(prefs.url))
    }

//    companion object {
//        private const val TAG = "HomeActivity"
//    }
}
