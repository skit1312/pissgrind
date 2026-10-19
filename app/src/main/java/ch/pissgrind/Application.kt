package ch.pissgrind

import android.app.Application
import ch.pissgrind.managers.LocalProviderManager
import ch.pissgrind.managers.NavidromeManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PissgrindApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        NavidromeManager.init(this)
        LocalProviderManager.init(this)
    }
}
