package xyz.mcmxciv.halauncher.activities.setup.integration

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer

import xyz.mcmxciv.halauncher.databinding.IntegrationFragmentBinding
import xyz.mcmxciv.halauncher.interfaces.IntegrationListener
import java.lang.Exception

class IntegrationFragment : Fragment() {
    private lateinit var binding: IntegrationFragmentBinding
    private lateinit var viewModel: IntegrationViewModel
    lateinit var integrationListener: IntegrationListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = IntegrationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(IntegrationViewModel::class.java)

        viewModel.registerDevice()

        viewModel.integrationState.observe(this, Observer {
            when (it) {
                IntegrationViewModel.IntegrationState.SUCCESS ->
                    integrationListener.onIntegrationComplete()
                IntegrationViewModel.IntegrationState.FAILED ->
                    showRetryView()
                else -> throw Exception("Unexpected integration state.")
            }
        })

        viewModel.integrationError.observe(this, Observer {
            integrationListener.onIntegrationFailed(it)
        })
    }

    private fun showRetryView() {

    }

}
