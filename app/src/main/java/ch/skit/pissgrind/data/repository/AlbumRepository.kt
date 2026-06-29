package ch.skit.pissgrind.data.repository

import androidx.media3.common.MediaItem
import ch.skit.pissgrind.data.datasource.local.LocalDataSource
import ch.skit.pissgrind.data.datasource.navidrome.NavidromeDataSource
import ch.skit.pissgrind.managers.LocalProviderManager
import ch.skit.pissgrind.managers.NavidromeManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val navidromeDataSource: NavidromeDataSource
) {
    suspend fun getAlbums(
        sort: String? = "alphabeticalByName",
        size: Int? = 100,
        offset: Int? = 0,
        ignoreCachedResponse: Boolean = false,
        favoritesOnly: Boolean = false,
    ): List<MediaItem> = coroutineScope {
        val deferredAlbums = mutableListOf<Deferred<List<MediaItem>>>()

        if (NavidromeManager.checkActiveServers())
            deferredAlbums.add(async { navidromeDataSource.getNavidromeAlbums(sort, size, offset, ignoreCachedResponse, favoritesOnly=favoritesOnly) })

        if (LocalProviderManager.checkActiveFolders())
            if (offset == 0)
                deferredAlbums.add(async { localDataSource.getLocalAlbums(sort) })


        deferredAlbums.awaitAll().flatten()
    }

    suspend fun getAlbum(albumId: String, ignoreCachedResponse: Boolean = false): List<MediaItem>? = coroutineScope {
        if (albumId.startsWith("Local_"))
            localDataSource.getLocalAlbum(albumId)
        else
            navidromeDataSource.getNavidromeAlbum(albumId, ignoreCachedResponse)

    }

    suspend fun searchAlbum(query: String): List<MediaItem> = coroutineScope {
        val deferredAlbums = mutableListOf<Deferred<List<MediaItem>>>()

        if (LocalProviderManager.checkActiveFolders())
            deferredAlbums.add(async { localDataSource.searchLocalAlbums(query) })

        if (NavidromeManager.checkActiveServers())
            deferredAlbums.add(async { navidromeDataSource.searchNavidromeAlbums(query) })

        deferredAlbums.awaitAll().flatten()
    }
}