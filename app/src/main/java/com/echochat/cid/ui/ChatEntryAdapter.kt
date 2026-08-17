package com.echochat.cid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.echochat.cid.data.ChatListItem
import com.echochat.cid.databinding.ItemChatEntryBinding
import com.echochat.cid.util.ImageUtils

class ChatEntryAdapter(
    private val onEntryClicked: (ChatListItem) -> Unit
) : ListAdapter<ChatListItem, ChatEntryAdapter.ChatEntryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatEntryViewHolder {
        val binding = ItemChatEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatEntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatEntryViewHolder(
        private val binding: ItemChatEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: ChatListItem) {
            val friend = entry.friend
            binding.textNickname.text = friend.nickname
            binding.textAvatarInitial.text = friend.nickname.trim().firstOrNull()
                ?.uppercaseChar()?.toString() ?: "?"
            binding.textLastMessage.text = entry.lastMessage ?: friend.friendUid
            binding.root.setOnClickListener { onEntryClicked(entry) }

            val avatar = friend.avatarBase64
            if (avatar != null) {
                val bitmap = ImageUtils.base64ToBitmap(avatar)
                if (bitmap != null) {
                    binding.imageAvatar.setImageBitmap(bitmap)
                    binding.imageAvatar.visibility = View.VISIBLE
                } else {
                    binding.imageAvatar.visibility = View.GONE
                }
            } else {
                binding.imageAvatar.visibility = View.GONE
            }

            val unread = entry.unreadCount
            if (unread > 0) {
                binding.textUnreadBadge.visibility = View.VISIBLE
                binding.textUnreadBadge.text = if (unread > 99) "99+" else unread.toString()
            } else {
                binding.textUnreadBadge.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatListItem>() {
            override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem) =
                oldItem.friend.id == newItem.friend.id

            override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem) =
                oldItem == newItem
        }
    }
}
