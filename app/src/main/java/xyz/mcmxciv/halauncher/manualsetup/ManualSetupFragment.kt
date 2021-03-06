package xyz.mcmxciv.halauncher.manualsetup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import xyz.mcmxciv.halauncher.BaseFragment
import xyz.mcmxciv.halauncher.databinding.FragmentManualSetupBinding
import xyz.mcmxciv.halauncher.navigate
import xyz.mcmxciv.halauncher.observe
import xyz.mcmxciv.halauncher.utils.value

@AndroidEntryPoint
class ManualSetupFragment : BaseFragment() {
    private lateinit var binding: FragmentManualSetupBinding
    private val viewModel: ManualSetupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManualSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observe(viewModel.navigation) { navigate(it) }

        binding.discoveryButton.setOnClickListener { viewModel.onDiscoveryButtonClicked() }
        binding.openUrlButton.setOnClickListener {
            viewModel.onUrlSubmitted(binding.hostText.value)
        }
        binding.hostText.setOnEditorActionListener { v, actionId, _ ->
            viewModel.onEditorAction(actionId, v.value)
        }
    }
}
