package com.example.musicplayer.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.musicplayer.R
import com.example.musicplayer.data.Song
import com.example.musicplayer.databinding.ItemTrackBinding
import javax.inject.Inject

class SongListAdapter @Inject constructor(
    private val context: Context,
    private val glide: RequestManager
) : RecyclerView.Adapter<SongListAdapter.SongViewHolder>() {

    var songs: List<Song>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    private val differ = AsyncListDiffer(this, DIFF_CALLBACK)

    var currentPlayingSongId: Int = 0

    private var onItemClickListener: ((Song) -> Unit)? = null
    fun setOnItemClickListener(listener: (Song) -> Unit) {
        onItemClickListener = listener
    }

    inner class SongViewHolder(private val binding: ItemTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song) {
            with(binding) {
                itemTextLabel.text = song.title
                itemTextArtist.text = song.artist
                glide.load(song.bitmapUri).into(itemSongImage)

                val backgroundColor = if (song.id == currentPlayingSongId) R.color.green_100 else R.color.white

                itemBackground.setCardBackgroundColor(ContextCompat.getColor(context, backgroundColor))

                itemBackground.setOnClickListener {
                    onItemClickListener?.let { click ->
                        currentPlayingSongId = song.id
                        notifyDataSetChanged()
                        click(song)
                    }
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemTrackBinding.inflate(layoutInflater, parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }
        }
    }
}
