package com.example.musicplayer.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.musicplayer.R
import com.example.musicplayer.utils.Event
import com.example.musicplayer.utils.Resource

class MusicServiceConnection(context: Context) {

    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    val isConnected: LiveData<Event<Resource<Boolean>>>
        get() = _isConnected

    private val _networkError = MutableLiveData<Event<Resource<Boolean>>>()
    val networkError: LiveData<Event<Resource<Boolean>>>
        get() = _networkError

    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState: LiveData<PlaybackStateCompat?>
        get() = _playbackState

    private val _currentPlayingSong = MutableLiveData<MediaMetadataCompat?>()
    val currentPlayingSong: LiveData<MediaMetadataCompat?>
        get() = _currentPlayingSong

    lateinit var mediaController: MediaControllerCompat
    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    private val mediaServiceConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, MusicService::class.java),
        mediaServiceConnectionCallback,
        null
    ).apply { connect() }

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback(context))
            }
            _isConnected.postValue(Event(Resource.success(true)))
            Log.i("Internet_Browser", "SUCCESS")
        }

        override fun onConnectionSuspended() {
            Log.i("Internet_Browser", "SUSPEND")
            _isConnected.postValue(
                Event(
                    Resource.error(
                        context.getString(R.string.connection_suspended_message),
                        false
                    )
                )
            )
        }

        override fun onConnectionFailed() {
            Log.i("Internet_Browser", "FAIL")
            _isConnected.postValue(
                Event(
                    Resource.error(
                        context.getString(R.string.connection_failed_message),
                        false
                    )
                )
            )
        }

    }

    private inner class MediaControllerCallback(
        private val context: Context
    ) : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _currentPlayingSong.postValue(metadata)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when (event) {
                NETWORK_ERROR -> _networkError.postValue(
                    Event(
                        Resource.error(
                            context.getString(R.string.no_connection_message),
                            null
                        )
                    )
                )
            }
        }

        override fun onSessionDestroyed() {
            mediaServiceConnectionCallback.onConnectionSuspended()
        }
    }

    companion object {
        const val NETWORK_ERROR = "NETWORK_ERROR"
    }

}