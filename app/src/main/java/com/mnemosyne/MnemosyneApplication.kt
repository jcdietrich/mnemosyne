package com.mnemosyne

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.objectbox.BoxStore

@HiltAndroidApp
class MnemosyneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // BoxStore is initialized lazily via Hilt in DataModule
    }
}
