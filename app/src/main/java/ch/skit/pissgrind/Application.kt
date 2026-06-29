package ch.skit.pissgrind

import android.app.Application
import ch.skit.pissgrind.managers.LocalProviderManager
import ch.skit.pissgrind.managers.NavidromeManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChoraApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        NavidromeManager.init(this)
        LocalProviderManager.init(this)
    }
}
