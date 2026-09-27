package ch.pissgrind.data.di

import ch.pissgrind.data.datasource.local.LocalDataSource
import ch.pissgrind.data.datasource.navidrome.NavidromeDataSource
import ch.pissgrind.managers.settings.AppearanceSettingsManager
import ch.pissgrind.managers.settings.LocalDataSettingsManager
import ch.pissgrind.providers.local.LocalProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
