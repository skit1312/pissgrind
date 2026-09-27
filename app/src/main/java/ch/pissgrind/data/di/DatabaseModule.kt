package ch.pissgrind.data.di

import android.content.Context
import androidx.room.Room
import ch.pissgrind.data.local.PissgrindDatabase
import ch.pissgrind.data.local.dao.CollectionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PissgrindDatabase {
        return Room.databaseBuilder(
            context,
            PissgrindDatabase::class.java,
            "pissgrind_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideCollectionDao(database: PissgrindDatabase): CollectionDao {
        return database.collectionDao()
    }
}
