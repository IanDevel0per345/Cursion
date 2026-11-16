package com.opencell.app.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.text.Normalizer

data class InstalledApp(val label: String, val packageName: String)

class AppResolver(private val context: Context) {
    private val aliases = mapOf(
        "teste" to "com.opencell.playground",
        "app de teste" to "com.opencell.playground",
        "aplicativo de teste" to "com.opencell.playground",
        "playground" to "com.opencell.playground",
        "navegador" to "browser",
    )

    fun launchable(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = context.packageManager
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { InstalledApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
    }

    fun resolve(name: String?, packageName: String?): InstalledApp? {
        val apps = launchable()
        packageName?.let { pkg -> apps.firstOrNull { it.packageName == pkg }?.let { return it } }
        val wanted = norm(name ?: return null)
        val aliasTarget = aliases[wanted]
        if (aliasTarget != null && aliasTarget != "browser") apps.firstOrNull { it.packageName == aliasTarget }?.let { return it }
        if (aliasTarget == "browser") browser(apps)?.let { return it }
        return apps.firstOrNull { norm(it.label) == wanted }
            ?: apps.firstOrNull { norm(it.label).contains(wanted) }
            ?: apps.firstOrNull { it.packageName.equals(name, ignoreCase = true) }
    }

    private fun browser(apps: List<InstalledApp>): InstalledApp? {
        val probe = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com"))
        val pkg = context.packageManager.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        return apps.firstOrNull { it.packageName == pkg }
    }

    fun launchIntent(app: InstalledApp): Intent? =
        context.packageManager.getLaunchIntentForPackage(app.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    private fun norm(s: String) =
        Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
}
