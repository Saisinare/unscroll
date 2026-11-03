package com.unscroll.protect

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** An active device admin can't be uninstalled until it is deactivated, which TamperGuard blocks. */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Turning this off removes Unscroll's uninstall protection."

    companion object {
        fun component(context: Context) = ComponentName(context, AdminReceiver::class.java)
    }
}
