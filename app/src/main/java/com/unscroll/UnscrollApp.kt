package com.unscroll

import android.app.Application
import com.unscroll.data.BlockStats
import com.unscroll.data.ConfigStore

class UnscrollApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ConfigStore.init(this)
        BlockStats.init(this)
    }
}
