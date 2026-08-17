package com.echochat.cid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.echochat.cid.data.Friend
import com.echochat.cid.databinding.ItemFriendBinding
import com.echochat.cid.util.ImageUtils

class FriendListAdapter(
    private val onFriendClicked: (Friend) -> Unit
) : ListAdapter<Friend, FriendListAdapter.FriendViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(
        private val binding: ItemFriendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: Friend) {
            binding.textNickname.text = friend.nickname
            binding.textAvatarInitial.text = friend.nickname
                .trim()
                .firstOrNull()
                ?.uppercaseChar()
                ?.toString()
                ?: "?"
            binding.textLastMessage.text = if (friend.isBlocked) {
                "${friend.friendUid} • Diblokir"
            } else {
                friend.friendUid
            }
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
            binding.root.setOnClickListener { onFriendClicked(friend) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Friend>() {
            override fun areItemsTheSame(oldItem: Friend, newItem: Friend) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Friend, newItem: Friend) =
                oldItem == newItem
        }
    }
}
