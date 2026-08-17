package com.echochat.cid.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.echochat.cid.R
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.data.FirestoreRepository
import com.echochat.cid.data.Friend
import com.echochat.cid.databinding.ActivityFriendDetailBinding
import com.echochat.cid.util.ImageUtils
import kotlinx.coroutines.launch

class FriendDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFriendDetailBinding
    private lateinit var friendUid: String
    private var currentFriend: Friend? = null
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        friendUid = intent.getStringExtra(EXTRA_FRIEND_UID).orEmpty()

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.textFriendUid.text = friendUid

        binding.buttonStartChat.setOnClickListener { openChat() }
        binding.buttonToggleBlock.setOnClickListener { toggleBlock() }
        binding.buttonDeleteFriend.setOnClickListener { confirmDelete() }

        observeFriend()
        refreshAvatarFromServer()
    }

    private fun observeFriend() {
        val friendDao = AppDatabase.getInstance(this).friendDao()
        lifecycleScope.launch {
            friendDao.observeByUid(friendUid).collect { friend ->
                if (friend == null) {
                    finish()
                    return@collect
                }
                currentFriend = friend
                binding.textNickname.text = friend.nickname
                binding.textAvatarInitial.text = friend.nickname.trim().firstOrNull()
                    ?.uppercaseChar()?.toString() ?: "?"
                binding.buttonToggleBlock.text = getString(
                    if (friend.isBlocked) R.string.action_unblock else R.string.action_block
                )
                binding.buttonStartChat.isEnabled = !friend.isBlocked
                showAvatar(friend.avatarBase64)
            }
        }
    }

    private fun showAvatar(base64: String?) {
        if (base64 == null) {
            binding.imageAvatar.visibility = View.GONE
            return
        }
        val bitmap = ImageUtils.base64ToBitmap(base64)
        if (bitmap != null) {
            binding.imageAvatar.setImageBitmap(bitmap)
            binding.imageAvatar.visibility = View.VISIBLE
        } else {
            binding.imageAvatar.visibility = View.GONE
        }
    }

    /**
     * Ambil avatar terbaru teman dari Firestore (siapa tahu dia baru ganti foto),
     * lalu perbarui cache lokal supaya list chat & kontak ikut ke-update.
     */
    private fun refreshAvatarFromServer() {
        lifecycleScope.launch {
            try {
                val remoteUser = firestoreRepository.fetchUser(friendUid) ?: return@launch
                val friendDao = AppDatabase.getInstance(this@FriendDetailActivity).friendDao()
                friendDao.updateAvatar(friendUid, remoteUser.avatarBase64)
            } catch (error: Exception) {
                // Offline atau gagal - biarkan saja pakai avatar cache lokal yang sudah ada.
            }
        }
    }

    private fun openChat() {
        val friend = currentFriend ?: return
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(ChatActivity.EXTRA_FRIEND_UID, friend.friendUid)
        intent.putExtra(ChatActivity.EXTRA_FRIEND_NICKNAME, friend.nickname)
        startActivity(intent)
    }

    private fun toggleBlock() {
        val friend = currentFriend ?: return
        val newBlockedState = !friend.isBlocked

        if (!newBlockedState) {
            applyBlockedState(friend, false)
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_block_title)
            .setMessage(R.string.confirm_block_message)
            .setPositiveButton(R.string.action_yes) { _, _ -> applyBlockedState(friend, true) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun applyBlockedState(friend: Friend, isBlocked: Boolean) {
        val friendDao = AppDatabase.getInstance(this).friendDao()
        lifecycleScope.launch {
            friendDao.setBlocked(friend.friendUid, isBlocked)
        }
    }

    private fun confirmDelete() {
        val friend = currentFriend ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.action_yes) { _, _ -> deleteFriendAndChat(friend) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun deleteFriendAndChat(friend: Friend) {
        val database = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            database.messageDao().deleteAllForChat(friend.friendUid)
            database.friendDao().delete(friend)
            finish()
        }
    }

    companion object {
        const val EXTRA_FRIEND_UID = "extra_friend_uid"
    }
}

