package ch.pissgrind.data.repository

import androidx.media3.common.MediaItem // MediaItem can represent songs, albums (via browse MediaId), etc.
import ch.pissgrind.data.datasource.local.LocalDataSource
import ch.pissgrind.data.datasource.navidrome.NavidromeDataSource
import ch.pissgrind.managers.LocalProviderManager
import ch.pissgrind.managers.NavidromeManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StarredRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val navidromeDataSource: NavidromeDataSource
) {

    suspend fun getStarredItems(ignoreCachedResponse: Boolean = false): List<MediaItem> = coroutineScope {
        val deferredStarred = mutableListOf<Deferred<List<MediaItem>>>()

        if (NavidromeManager.checkActiveServers())
            deferredStarred.add(async { navidromeDataSource.getNavidromeStarred(ignoreCachedResponse) })

        if (LocalProviderManager.checkActiveFolders())
            deferredStarred.add(async { localDataSource.getLocalStarredItems() })

        deferredStarred.awaitAll().flatten()
    }

     suspend fun starItem(id: String = "", albumId: String = "", artistId: String = "", ignoreCachedResponse: Boolean) {
         if (!id.startsWith("Local_") || !albumId.startsWith("Local_") || !artistId.startsWith("Local_"))
             navidromeDataSource.starNavidromeItem(id, albumId, artistId, ignoreCachedResponse)
         else
             localDataSource.starLocalItem(id)
     }

     suspend fun unStarItem(id: String = "", albumId: String = "", artistId: String = "", ignoreCachedResponse: Boolean) {
         if (!id.startsWith("Local_") || !albumId.startsWith("Local_") || !artistId.startsWith("Local_"))
             navidromeDataSource.unstarNavidromeItem(id, albumId, artistId, ignoreCachedResponse)
         else
            localDataSource.unstarLocalItem(id)
     }
}
