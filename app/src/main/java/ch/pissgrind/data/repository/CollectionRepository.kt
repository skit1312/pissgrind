package ch.pissgrind.data.repository

import ch.pissgrind.data.local.dao.CollectionDao
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.data.local.entity.CollectionItemType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class CollectionRepository @Inject constructor(
    private val collectionDao: CollectionDao
) {

    fun getAllItems(): Flow<List<CollectionItem>> = collectionDao.getAll()

    fun getItemsByType(type: CollectionItemType): Flow<List<CollectionItem>> =
        collectionDao.getByType(type)

    fun isInCollection(id: String): Flow<Boolean> = collectionDao.isInCollection(id)

    suspend fun toggleItem(item: CollectionItem) {
        if (collectionDao.isInCollectionSync(item.id)) {
            collectionDao.remove(item.id)
        } else {
            collectionDao.add(item)
        }
    }

    suspend fun addItem(item: CollectionItem) = collectionDao.add(item)

    suspend fun removeItem(id: String) = collectionDao.remove(id)

    suspend fun removeItemsByType(type: CollectionItemType) {
        collectionDao.deleteByType(type.name)
    }
}
