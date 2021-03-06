package com.example.musicplayer.utils

import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.musicplayer.data.Song
import java.text.SimpleDateFormat
import java.util.*

val PlaybackStateCompat.isPrepared
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
            state == PlaybackStateCompat.STATE_PLAYING ||
            state == PlaybackStateCompat.STATE_PAUSED

inline val PlaybackStateCompat.isPlaying
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
            state == PlaybackStateCompat.STATE_PLAYING

inline val PlaybackStateCompat.isPlayEnabled
    get() = actions and PlaybackStateCompat.ACTION_PLAY != 0L ||
            (actions and PlaybackStateCompat.ACTION_PLAY != 0L &&
                    state == PlaybackStateCompat.STATE_PAUSED)

inline val PlaybackStateCompat.currentPlaybackPosition: Long
    get() = if (state == PlaybackStateCompat.STATE_PLAYING) {
        val timeDifference = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        (timeDifference * playbackSpeed + position).toLong()
    } else position

fun MediaMetadataCompat.toSong(): Song? {
    return description?.let {
        Song(
            it.mediaId?.toInt() ?: 0,
            it.title.toString(),
            it.subtitle.toString(),
            it.iconUri.toString(),
            it.mediaUri.toString()
        )
    }
}

fun Long.toTimeFormat(): String {
    val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
    return dateFormat.format(this)
}
