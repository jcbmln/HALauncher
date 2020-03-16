package xyz.mcmxciv.halauncher.ui.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.play.core.install.model.ActivityResult
import xyz.mcmxciv.halauncher.LauncherApplication
import xyz.mcmxciv.halauncher.background.PackageReceiver
import xyz.mcmxciv.halauncher.R
import xyz.mcmxciv.halauncher.databinding.ActivityMainBinding
import xyz.mcmxciv.halauncher.models.InvariantDeviceProfile
import xyz.mcmxciv.halauncher.ui.createViewModel
import xyz.mcmxciv.halauncher.ui.observe
import javax.inject.Inject
import kotlin.math.hypot

class MainActivity : AppCompatActivity(), PackageReceiver.PackageListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var packageReceiver: PackageReceiver
    private val appListAnimators: List<Animator> by lazy {
        val x = binding.allAppsButton.left + (binding.allAppsButton.width / 2)
        val y = binding.allAppsButton.top + (binding.allAppsButton.height / 2)
        val closedRadius = 0f
        val openRadius = hypot(
            binding.root.width.toDouble(),
            binding.root.height.toDouble()
        ).toFloat()

        val openAnimator = ViewAnimationUtils.createCircularReveal(
            binding.appList, x, y, closedRadius, openRadius
        )
        val closeAnimator = ViewAnimationUtils.createCircularReveal(
            binding.appList, x, y, openRadius, closedRadius
        )

        closeAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                binding.appList.visibility = View.GONE
            }
        })

        return@lazy listOf(openAnimator, closeAnimator)
    }

    @Inject
    lateinit var idp: InvariantDeviceProfile

    @Inject
    lateinit var appListAdapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        LauncherApplication.instance.component.inject(this)
        viewModel = createViewModel {
            LauncherApplication.instance.component.mainActivityViewModel()
        }
        instance = this

        binding.appList.layoutManager = GridLayoutManager(this, idp.numColumns)
        binding.appList.adapter = appListAdapter

        observe(viewModel.appListItems) { items ->
            appListAdapter.update(items)
        }

        observe(viewModel.config) { config ->
            config?.let { setThemeColor(Color.parseColor(it.themeColor)) }
        }

        binding.allAppsButton.setOnClickListener {
            openCloseAppList()
        }
    }

    override fun onResume() {
        super.onResume()
        packageReceiver = PackageReceiver.initialize(this)
        registerReceiver(packageReceiver, packageReceiver.filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(packageReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UPDATE_REQUEST_CODE) {
            when (resultCode) {
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    Toast
                        .makeText(this, "Failed to update app.", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.app_navigation_host_fragment)

        if (navController.navigateUp()) {
            return true
        }

        return super.onSupportNavigateUp()
    }

    override fun onPackageReceived() {
        viewModel.updateAppListItems()
    }

    private fun openCloseAppList() {
        if (binding.appList.isVisible) {
            val animator = appListAnimators[1]
            animator.start()
        }
        else {
            val animator = appListAnimators[0]
            binding.appList.visibility = View.VISIBLE
            animator.start()
        }
    }

    private fun setThemeColor(color: Int) {
        window.statusBarColor = color
        window.navigationBarColor = color

        val drawable = ColorDrawable(color)
        drawable.alpha = 240
        binding.appList.background = drawable
        appListAdapter.setThemeColor(color)
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 1
        var instance: MainActivity? = null
    }
}
