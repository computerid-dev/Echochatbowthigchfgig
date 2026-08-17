package com.echochat.cid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.echochat.cid.data.Message
import com.echochat.cid.databinding.ItemMessageBinding
import com.echochat.cid.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter : ListAdapter<Message, MessageAdapter.MessageViewHolder>(DIFF_CALLBACK) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            val time = timeFormat.format(message.timestamp)
            if (message.isMine) {
                binding.bubbleMine.visibility = View.VISIBLE
                binding.bubbleFriend.visibility = View.GONE
                bindContent(
                    image = binding.imageMineContent,
                    text = binding.textMineContent,
                    message = message
                )
                binding.textMineTime.text = time
            } else {
                binding.bubbleMine.visibility = View.GONE
                binding.bubbleFriend.visibility = View.VISIBLE
                bindContent(
                    image = binding.imageFriendContent,
                    text = binding.textFriendContent,
                    message = message
                )
                binding.textFriendTime.text = time
            }
        }

        private fun bindContent(image: android.widget.ImageView, text: android.widget.TextView, message: Message) {
            val base64 = message.imageBase64
            if (base64 != null) {
                val bitmap = ImageUtils.base64ToBitmap(base64)
                if (bitmap != null) {
                    image.setImageBitmap(bitmap)
                    image.visibility = View.VISIBLE
                } else {
                    image.visibility = View.GONE
                }
            } else {
                image.visibility = View.GONE
            }

            if (message.content.isNotEmpty()) {
                text.text = message.content
                text.visibility = View.VISIBLE
            } else {
                text.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Message, newItem: Message) =
                oldItem == newItem
        }
    }
}

