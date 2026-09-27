package ch.pissgrind.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CollectionItemType {
    ALBUM, ARTIST, SONG, PLAYLIST
}

@Entity(tableName = "collection_items")
data class CollectionItem(
    @PrimaryKey val id: String,
    val type: CollectionItemType,
    val title: String,
    val subtitle: String? = null,
    val coverArtUrl: String? = null,
    val streamUrl: String? = null,         
    val duration: Int? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val album: String? = null,             
    val addedAt: Long = System.currentTimeMillis()
)
