package com.example.musicplayer.ui

import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.example.musicplayer.R
import com.example.musicplayer.data.Song
import com.example.musicplayer.databinding.ActivityMainBinding
import com.example.musicplayer.ui.adapter.SongListAdapter
import com.example.musicplayer.utils.*
import com.example.musicplayer.viewmodel.MainViewModel
import com.example.musicplayer.viewmodel.SongViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Objects.requireNonNull
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private val songViewModel: SongViewModel by viewModels()
    private var currentPlayingSong: Song? = null
    private var shouldUpdateSeekbar = true
    @Inject
    lateinit var songListAdapter: SongListAdapter
    @Inject
    lateinit var glide: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        setupRecyclerView()
        subscribeToObservers()
        initialiseButton()
    }

    private fun initialiseButton() {
        with(binding) {
            buttonPause.setOnClickListener {
                currentPlayingSong?.let {
                    mainViewModel.playOrToggleSong(it, true)
                    songListAdapter.currentPlayingSongId = it.id
                }
            }
            buttonPauseMini.setOnClickListener {
                currentPlayingSong?.let {
                    mainViewModel.playOrToggleSong(it, true)
                }
            }
            buttonNext.setOnClickListener {
                mainViewModel.skipToNextSong()
            }
            buttonPrev.setOnClickListener {
                mainViewModel.skipToPreviousSong()
            }
        }
        updateSeekBars()
        fixThroughMotionTouch()
    }

    private fun fixThroughMotionTouch() {
        binding.motionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
                binding.recyclerView.isVisible = true
                window.statusBarColor = resources.getColor(R.color.white)
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {}

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == R.id.end) {
                    binding.recyclerView.isVisible = false
                    window.statusBarColor = resources.getColor(R.color.green_100)
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {}

        })
    }

    private fun updateSeekBars() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    binding.timerCurrent.text = progress.toLong().toTimeFormat()
                } else {
                    binding.seekBar.progress =
                        songViewModel.currentPlayerPosition.value?.toInt() ?: 0
                    binding.progressBar.progress =
                        songViewModel.currentPlayerPosition.value?.toInt() ?: 0
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                shouldUpdateSeekbar = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    mainViewModel.seekTo(it.progress.toLong())
                }
                shouldUpdateSeekbar = true
            }
        })
    }

    private fun setupRecyclerView() {
        val manager = LinearLayoutManager(this)
        binding.recyclerView.apply {
            adapter = songListAdapter
            layoutManager = manager
        }
        songListAdapter.setOnItemClickListener {
            mainViewModel.playOrToggleSong(it, true)
        }
    }

    private fun updateSongData(song: Song?) {
        glide.load(song?.bitmapUri).into(binding.imageSong)
        glide.load(song?.bitmapUri).into(binding.imageSongMini)
        binding.apply {
            textSongNameMini.text = song?.title
            textSongArtistMini.text = song?.artist
            textSongName.text = song?.title
            textSongArtist.text = song?.artist
        }
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(this, { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    Log.i("Internet_Status", "SUCCESS")
                    binding.progressBar.isVisible = false
                    binding.layoutPlayer.isVisible = true
                    binding.layoutPlayerMini.isVisible = true
                    result.data?.let { songs ->
                        songListAdapter.songs = songs
                        if (currentPlayingSong == null && songs.isNotEmpty()) {
                            currentPlayingSong = songs[0]
                            updateSongData(songs[0])
                        }
                    }
                }
                Status.LOADING -> {
                    Log.i("Internet_Status", "LOADING")
                    binding.progressBar.isVisible = true
                    binding.layoutPlayer.isVisible = true
                    binding.layoutPlayerMini.isVisible = true
                }
                Status.ERROR -> {
                    Log.i("Internet_Status", "ERROR")
                }
            }
        })

        mainViewModel.currentPlayingSong.observe(this, {
            if (it != null) {
                val currentSong = it.toSong()
                updateSongData(currentSong)
                songListAdapter.currentPlayingSongId = currentSong?.id ?: 0
                currentPlayingSong = currentSong
            }
        })

        mainViewModel.playbackState.observe(this, {
            binding.buttonPause.setIconResource(if (it?.isPlaying == true) R.drawable.ic_baseline_pause_24
            else R.drawable.ic_baseline_play_arrow_24)
            binding.buttonPauseMini.setImageResource(if (it?.isPlaying == true) R.drawable.ic_baseline_pause_24
            else R.drawable.ic_baseline_play_arrow_24)
        })

        mainViewModel.isConnected.observe(this, { unknownError(it) })
        mainViewModel.networkError.observe(this, { unknownError(it) })

        songViewModel.currentPlayerPosition.observe(this, {
            if (shouldUpdateSeekbar) {
                binding.seekBar.progress = it.toInt()
                binding.musicProgressMini.progress = it.toInt()
                binding.timerCurrent.text = it.toTimeFormat()
            }
        })

        songViewModel.currentSongDuration.observe(this, {
            binding.seekBar.max = it.toInt()
            binding.musicProgressMini.max = it.toInt()
            binding.timerMax.text = "/${it.toTimeFormat()}"
        })
    }

    private fun unknownError(it: Event<Resource<Boolean>>?) {
        it?.getContentForHandled()?.let { result ->
            when (result.status) {
                Status.ERROR -> Snackbar.make(
                    binding.root,
                    result.message ?: getString(R.string.unknown_error_message),
                    Snackbar.LENGTH_LONG
                ).show()
                else -> Unit
            }
        }
    }

}