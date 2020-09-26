package xyz.mcmxciv.halauncher.apps

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.ShortcutInfo
import xyz.mcmxciv.halauncher.icons.IconFactory
import xyz.mcmxciv.halauncher.shortcuts.Shortcut
import javax.inject.Inject

class AppUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val iconFactory: IconFactory
) {
    private val shortcutComparator = Comparator<ShortcutInfo> { a, b ->
        when {
            a.isDeclaredInManifest && !b.isDeclaredInManifest -> -1
            !a.isDeclaredInManifest && b.isDeclaredInManifest -> 1
            else -> a.rank.compareTo(b.rank)
        }
    }

    suspend fun getAppDrawerItems(): List<AppDrawerItem> {
        val launcherActivityInfo = appRepository.getLauncherActivityInfo()
        val cachedApps = appRepository.getApps()
        val appDrawerItems = cachedApps
            .filterNot { app -> app.isHidden }
            .mapNotNull { app -> createCachedAppDrawerItem(launcherActivityInfo, app) }
            .toMutableList()

        val cachedAppActivityNames = cachedApps.map { app -> app.activityName }
        val newAppDrawerItems = launcherActivityInfo
            .filterNot { info -> cachedAppActivityNames.contains(info.name) }
            .map { info -> createNewAppDrawerItem(info) }

        appDrawerItems.addAll(newAppDrawerItems)
        appDrawerItems.sortBy { info -> info.app.displayName }

        return appDrawerItems
    }

    suspend fun hideApp(activityName: String) {
        appRepository.getApp(activityName)?.let { app ->
            app.isHidden = true
            appRepository.updateApp(app)
        }
    }

    private suspend fun createCachedAppDrawerItem(
        launcherActivityInfo: List<LauncherActivityInfo>,
        app: App
    ): AppDrawerItem? {
        val launcherActivity = launcherActivityInfo.singleOrNull { info ->
            info.name == app.activityName
        }

        if (launcherActivity == null) {
            appRepository.removeApp(app)
            return null
        }

        return AppDrawerItem(
            app,
            iconFactory.getIcon(launcherActivity),
            launcherActivity.componentName,
            createShortcuts(app.packageName, launcherActivity.componentName)
        )
    }

    private suspend fun createNewAppDrawerItem(
        launcherActivityInfo: LauncherActivityInfo
    ): AppDrawerItem {
        val packageInfo = appRepository
            .getPackageInfo(launcherActivityInfo.applicationInfo.packageName)
        val isSystemApp = (launcherActivityInfo.applicationInfo.flags
                and ApplicationInfo.FLAG_SYSTEM) == 1
        val app = App(
            launcherActivityInfo.name,
            packageInfo.packageName,
            appRepository.getDisplayName(launcherActivityInfo),
            isSystemApp,
            false,
            -1
        )

        appRepository.addApp(app)

        return AppDrawerItem(
            app,
            iconFactory.getIcon(launcherActivityInfo),
            launcherActivityInfo.componentName,
            createShortcuts(app.packageName, launcherActivityInfo.componentName)
        )
    }

    private fun createShortcuts(packageName: String, componentName: ComponentName): List<Shortcut> {
        return if (appRepository.hasShortcutHostPermission) {
            appRepository
                .queryShortcuts(packageName, componentName)
                .sortedWith(shortcutComparator)
                .take(4)
                .mapNotNull { s ->
                    iconFactory.getShortcutIcon(s)?.let {
                        Shortcut(
                            s.id,
                            packageName,
                            s.shortLabel!!.toString(),
                            it
                        )
                    }
                }
        } else listOf()
    }
}
