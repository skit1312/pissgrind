package ch.skit.pissgrind.data.di

import android.content.Context
import ch.skit.pissgrind.data.datasource.local.LocalDataSource
import ch.skit.pissgrind.data.datasource.lrclib.LrclibDataSource
import ch.skit.pissgrind.data.datasource.navidrome.NavidromeDataSource
import ch.skit.pissgrind.data.datasource.netease.NeteaseDataSource
import ch.skit.pissgrind.managers.settings.AppearanceSettingsManager
import ch.skit.pissgrind.managers.settings.LocalDataSettingsManager
import ch.skit.pissgrind.managers.settings.MediaProviderSettingsManager
import ch.skit.pissgrind.providers.local.LocalProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Singleton
    @Provides
    fun provideLocalDataSource(
        localProvider: LocalProvider,
        localDataSettingsManager: LocalDataSettingsManager,
        appearanceSettingsManager: AppearanceSettingsManager
    ): LocalDataSource {
        return LocalDataSource(localProvider, localDataSettingsManager, appearanceSettingsManager)
    }

    @Singleton
    @Provides
    fun provideNavidromeDataSource(): NavidromeDataSource {
        return NavidromeDataSource()
    }

    @Singleton
    @Provides
    fun provideLrcLibDataSource(
        settingsManager: MediaProviderSettingsManager,
        @ApplicationContext context: Context
    ): LrclibDataSource {
        return LrclibDataSource(settingsManager, context)
    }

    @Singleton
    @Provides
    fun provideNetEaseDataSource(
        @ApplicationContext context: Context
    ): NeteaseDataSource {
        return NeteaseDataSource(context)
    }
}