package com.example.system

import android.content.Context
import android.content.Intent
import com.example.data.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads user-launchable installed apps using ACTION_MAIN + CATEGORY_LAUNCHER
 * (backed by <queries> in AndroidManifest.xml — Google Play compliant, no QUERY_ALL_PACKAGES).
 */
object InstalledAppsLoader {

    suspend fun loadLaunchableApps(context: Context): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolved = pm.queryIntentActivities(intent, 0)
                resolved
                    .mapNotNull { info ->
                        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                        if (pkg == context.packageName) return@mapNotNull null
                        val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                        InstalledAppInfo(
                            packageName = pkg,
                            label = label.ifEmpty { pkg }
                        )
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }.getOrDefault(emptyList())
        }
}
