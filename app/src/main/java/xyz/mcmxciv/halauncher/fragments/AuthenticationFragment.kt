package xyz.mcmxciv.halauncher.fragments

import android.annotation.SuppressLint
import android.content.Intent
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.lifecycle.Observer

import xyz.mcmxciv.halauncher.R
import xyz.mcmxciv.halauncher.activities.integration.IntegrationActivity
import xyz.mcmxciv.halauncher.databinding.AuthenticationFragmentBinding
import xyz.mcmxciv.halauncher.repositories.AuthenticationRepository
import xyz.mcmxciv.halauncher.utils.AppPreferences

class AuthenticationFragment : Fragment() {
    private lateinit var binding: AuthenticationFragmentBinding
    private lateinit var viewModel: AuthenticationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AuthenticationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(AuthenticationViewModel::class.java)

        binding.authenticationWebView.apply {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
                    return viewModel.authenticate(url)
                }
            }
        }

        binding.authenticationWebView.loadUrl(AuthenticationRepository.authenticationUrl)

//        viewModel.authenticationError.observe(this, Observer {
//            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
//        })
//
//        viewModel.authenticationSuccess.observe(this, Observer {
//            AppPreferences.getInstance(this).isAuthenticated = it
//
//            if (it) {
//                startActivity(Intent(this, IntegrationActivity::class.java))
//                finish()
//            }
//        })
    }

}
