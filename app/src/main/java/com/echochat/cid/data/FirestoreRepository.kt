package com.echochat.cid.data

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class RemoteUser(
    val uid: String,
    val displayName: String,
    val avatarBase64: String?
)

data class RemoteMessage(
    val remoteId: String,
    val senderUid: String,
    val content: String,
    val imageBase64: String?,
    val timestampMillis: Long
)

/**
 * Lapisan sinkron lewat Cloud Firestore. Tidak pakai Firebase Auth sama sekali —
 * UID akun tamu dipakai langsung sebagai document id, jadi tidak butuh SHA-1/login apa pun.
 */
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")
    private val chatsRef = db.collection("chats")

    // ---------- Presence & profil ----------

    fun registerPresence(uid: String, displayName: String, avatarBase64: String?) {
        val data = hashMapOf<String, Any>(
            "displayName" to displayName,
            "lastSeen" to System.currentTimeMillis()
        )
        if (avatarBase64 != null) data["avatarBase64"] = avatarBase64
        usersRef.document(uid).set(data, SetOptions.merge())
    }

    suspend fun uidExists(uid: String): Boolean {
        val snapshot = usersRef.document(uid).get().await()
        return snapshot.exists()
    }

    suspend fun fetchUser(uid: String): RemoteUser? {
        val snapshot = usersRef.document(uid).get().await()
        if (!snapshot.exists()) return null
        return RemoteUser(
            uid = uid,
            displayName = snapshot.getString("displayName") ?: "",
            avatarBase64 = snapshot.getString("avatarBase64")
        )
    }

    // ---------- Chat 1-on-1 ----------

    fun chatIdFor(uidA: String, uidB: String): String {
        return listOf(uidA, uidB).sorted().joinToString("_")
    }

    fun sendMessage(
        chatId: String,
        senderUid: String,
        receiverUid: String,
        content: String,
        imageBase64: String? = null
    ): String {
        val docRef = chatsRef.document(chatId).collection("messages").document()
        val data = hashMapOf<String, Any>(
            "senderUid" to senderUid,
            "content" to content,
            "timestamp" to System.currentTimeMillis(),
            "participants" to listOf(senderUid, receiverUid)
        )
        if (imageBase64 != null) data["imageBase64"] = imageBase64
        docRef.set(data)
        return docRef.id
    }

    fun listenMessages(chatId: String, onNewMessage: (RemoteMessage) -> Unit): ListenerRegistration {
        return chatsRef.document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                for (change in snapshot.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) continue
                    val doc = change.document
                    onNewMessage(
                        RemoteMessage(
                            remoteId = doc.id,
                            senderUid = doc.getString("senderUid") ?: continue,
                            content = doc.getString("content") ?: "",
                            imageBase64 = doc.getString("imageBase64"),
                            timestampMillis = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    )
                }
            }
    }

    /**
     * Dengarkan SEMUA pesan (chat 1-on-1) di mana UID ini jadi partisipan, lintas percakapan.
     * Dipakai oleh service latar belakang untuk notifikasi. Butuh composite index di Firestore
     * (collection group "messages" + array-contains "participants" + orderBy "timestamp") —
     * Firestore akan kasih link otomatis untuk bikin index itu di kali pertama query dijalankan.
     */
    fun listenAllIncomingMessages(myUid: String, onNewMessage: (chatId: String, RemoteMessage) -> Unit): ListenerRegistration {
        return db.collectionGroup("messages")
            .whereArrayContains("participants", myUid)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                for (change in snapshot.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) continue
                    val doc = change.document
                    val chatId = doc.reference.parent.parent?.id ?: continue
                    onNewMessage(
                        chatId,
                        RemoteMessage(
                            remoteId = doc.id,
                            senderUid = doc.getString("senderUid") ?: continue,
                            content = doc.getString("content") ?: "",
                            imageBase64 = doc.getString("imageBase64"),
                            timestampMillis = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    )
                }
            }
    }
}
