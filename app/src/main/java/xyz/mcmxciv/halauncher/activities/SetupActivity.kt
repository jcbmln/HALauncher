package xyz.mcmxciv.halauncher.activities

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import xyz.mcmxciv.halauncher.HomeAssistantDiscoveryListener
import xyz.mcmxciv.halauncher.HomeAssistantResolveListener
import xyz.mcmxciv.halauncher.R
import xyz.mcmxciv.halauncher.ServiceListAdapter
import xyz.mcmxciv.halauncher.databinding.ActivitySetupBinding
import xyz.mcmxciv.halauncher.extensions.setPosition
import xyz.mcmxciv.halauncher.utils.BlurBuilder
import xyz.mcmxciv.halauncher.utils.UserPreferences
import xyz.mcmxciv.halauncher.utils.Utilities
import java.net.InetAddress
import java.util.*
import kotlin.collections.ArrayList

class SetupActivity : AppCompatActivity(), HomeAssistantResolveListener.Callback {
    private lateinit var binding: ActivitySetupBinding
    private lateinit var adapter: ServiceListAdapter
    private lateinit var prefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = UserPreferences.getInstance(this)

        binding.setupButtonEnter.setOnClickListener {
            val text = binding.setupUrlInput.text.toString()

            if (text.toLowerCase(Locale.ROOT).startsWith("https://") ||
                text.toLowerCase(Locale.ROOT).startsWith("http://"))
            {
                prefs.url = text
                prefs.setupDone = true
                finish()
            }
            else {
                val toast = Toast.makeText(
                    this, "You must enter a valid URL.", Toast.LENGTH_LONG
                )
                toast.setPosition(binding.setupManualContainer, window, 0, 5)
                toast.show()
            }
        }

        val layoutManager = LinearLayoutManager(this)
        val dividerItemDecoration =
            DividerItemDecoration(binding.serviceList.context, layoutManager.orientation)
        binding.serviceList.layoutManager = layoutManager
        binding.serviceList.addItemDecoration(dividerItemDecoration)
        adapter = ServiceListAdapter(ArrayList())
        binding.serviceList.adapter = adapter

        val nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        val discoveryListener = HomeAssistantDiscoveryListener(this, nsdManager)
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                prefs.canGetWallpaper = (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            }
        }
    }

    override fun addService(serviceInfo: NsdServiceInfo) {
        val handler = Handler(mainLooper)
        handler.post {
            adapter.addServiceItem(serviceInfo)
        }
    }

    companion object {
        private const val TAG = "SetupActivity"
        const val SERVICE_TYPE = "_home-assistant._tcp"



//        fun setWallpaper(context: Context, window: Window) {
//            if (prefs.canGetWallpaper) {
//                val wm = WallpaperManager.getInstance(context)
//                val wallpaper = BlurBuilder.blur(context, wm.drawable)
//                window.setBackgroundDrawable(wallpaper)
//            }
//        }
    }
}
