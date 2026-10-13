package com.volteenabler.app

import android.app.Application
import com.volteenabler.app.shizuku.ShizukuManager

class VoLTEApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ShizukuManager.init(this)
    }
}
