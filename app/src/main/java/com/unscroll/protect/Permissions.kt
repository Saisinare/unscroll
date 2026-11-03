package com.unscroll.protect

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.unscroll.block.BlockerService
import com.unscroll.data.UsageTracker

object Permissions {
    fun accessibilityOn(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val me = ComponentName(context, BlockerService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun usageOn(context: Context) = UsageTracker.hasPermission(context)

    fun adminOn(context: Context): Boolean =
        context.getSystemService(DevicePolicyManager::class.java).isAdminActive(AdminReceiver.component(context))

    fun openAccessibility(context: Context) = context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openUsageAccess(context: Context) = context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    fun openBatterySettings(context: Context) =
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    fun requestAdmin(context: Context) = context.startActivity(
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.component(context))
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Stops Unscroll from being uninstalled while protection is on."),
    )
}
