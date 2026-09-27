package ch.pissgrind.data.local.dao

import androidx.room.*
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.data.local.entity.CollectionItemType
import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Query

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collection_items ORDER BY addedAt DESC")
    fun getAll(): Flow<List<CollectionItem>>

    @Query("SELECT * FROM collection_items WHERE type = :type ORDER BY addedAt DESC")
    fun getByType(type: CollectionItemType): Flow<List<CollectionItem>>

    @Query("SELECT EXISTS(SELECT 1 FROM collection_items WHERE id = :id)")
    fun isInCollection(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM collection_items WHERE id = :id)")
    suspend fun isInCollectionSync(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(item: CollectionItem)

    @Query("DELETE FROM collection_items WHERE id = :id")
    suspend fun remove(id: String)

    @Query("DELETE FROM collection_items WHERE type = :type")
    suspend fun deleteByType(type: String)
}
