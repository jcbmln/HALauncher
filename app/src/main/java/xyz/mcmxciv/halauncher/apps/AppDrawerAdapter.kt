package xyz.mcmxciv.halauncher.apps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import xyz.mcmxciv.halauncher.HalauncherApplication
import xyz.mcmxciv.halauncher.databinding.ListItemAppBinding
import xyz.mcmxciv.halauncher.device.DeviceProfile
import xyz.mcmxciv.halauncher.shortcuts.ShortcutPopupWindow
import xyz.mcmxciv.halauncher.utils.HassTheme
import xyz.mcmxciv.halauncher.utils.ResourceProvider
import javax.inject.Inject

class AppDrawerAdapter @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val deviceProfile: DeviceProfile,
    private val appLauncher: AppLauncher
) : ListAdapter<AppListItem, AppDrawerAdapter.AppDrawerViewHolder>(
    itemCallback
) {
    private var onHideAppListener: OnHideAppListener = {}

    inner class AppDrawerViewHolder(
        private val binding: ListItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener,
        View.OnLongClickListener {
        private lateinit var _appListItem: AppListItem

        init {
            binding.appItem.setOnClickListener(this)
            binding.appItem.setOnLongClickListener(this)
        }

        fun bind(appListItem: AppListItem) {
            _appListItem = appListItem

            val theme = HassTheme.instance.appDrawerTheme
            val resources = HalauncherApplication.instance.resources

            binding.appItem.textSize = deviceProfile.iconTextSize
            binding.appItem.topIcon = appListItem.icon.toDrawable(resources)
            binding.appItem.text = appListItem.app.displayName
            binding.appItem.tag = appListItem
            binding.appItem.setTextColor(theme.labelTextColor)
        }

        override fun onClick(view: View) {
            val item = view.tag as AppListItem
            appLauncher.startMainActivity(item.componentName, view)
        }

        override fun onLongClick(view: View): Boolean {
            ShortcutPopupWindow(
                view,
                _appListItem,
                resourceProvider,
                appLauncher,
                onHideAppListener
            ).show()
            return true
        }
    }

    fun setOnHideAppListener(listener: OnHideAppListener) {
        onHideAppListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppDrawerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return AppDrawerViewHolder(ListItemAppBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: AppDrawerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val itemCallback = object : DiffUtil.ItemCallback<AppListItem>() {
            override fun areItemsTheSame(oldItem: AppListItem, newItem: AppListItem): Boolean =
                oldItem.app.activityName == oldItem.app.activityName

            override fun areContentsTheSame(
                oldItem: AppListItem,
                newItem: AppListItem
            ): Boolean = oldItem == newItem
        }
    }
}
