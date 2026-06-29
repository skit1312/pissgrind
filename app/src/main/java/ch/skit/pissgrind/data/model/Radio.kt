package ch.skit.pissgrind.data.model

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import ch.skit.pissgrind.R

fun MediaData.Radio.toMediaItem(): MediaItem {
    val mediaMetadata =
        MediaMetadata.Builder()
            .setStation(this@toMediaItem.name)
            .setArtist(this@toMediaItem.name)
            .setArtworkUri(
                ("android.resource://ch.skit.pissgrind/" + R.drawable.radioplaceholder).toUri()
            )
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setExtras(Bundle().apply {
                putString("navidromeID", this@toMediaItem.navidromeID)
                putString("homepage", this@toMediaItem.homePageUrl ?: "")
            })
            .build()

    println(".toMediaItem() : station: ${this@toMediaItem.name} mediaitem station: ${mediaMetadata.station}")

    return MediaItem.Builder()
        .setMediaMetadata(mediaMetadata)
        .setMediaId(this@toMediaItem.media)
        .setUri(this@toMediaItem.media)
        .setMimeType(MimeTypes.AUDIO_MPEG)
        .build()
}