package ch.pissgrind.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ch.pissgrind.data.local.dao.CollectionDao
import ch.pissgrind.data.local.entity.CollectionItem

@Database(
    entities = [CollectionItem::class],
    version = 1,
    exportSchema = false
)
abstract class PissgrindDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
}
