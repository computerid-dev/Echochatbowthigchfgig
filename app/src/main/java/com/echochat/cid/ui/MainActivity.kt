package com.echochat.cid.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.echochat.cid.R
import com.echochat.cid.data.FirestoreRepository
import com.echochat.cid.databinding.ActivityMainBinding
import com.echochat.cid.service.NotificationListenerService
import com.echochat.cid.util.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Kalau ditolak, notifikasi lokal cuma tidak akan tampil - tidak menghentikan apapun. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = SessionManager(this)

        // Daftarkan/perbarui keberadaan UID setiap kali aplikasi dibuka,
        // supaya teman bisa menemukan kita lewat kode ID.
        FirestoreRepository().registerPresence(
            session.myUid, session.displayName, session.avatarBase64
        )

        ensureNotificationPermission()
        if (session.isBackgroundServiceEnabled) {
            NotificationListenerService.start(this)
        }

        if (savedInstanceState == null) {
            showFragment(ChatsFragment())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.tabChats -> ChatsFragment()
                R.id.tabContacts -> ContactsFragment()
                R.id.tabAccount -> AccountFragment()
                R.id.tabDeveloper -> DeveloperFragment()
                else -> return@setOnItemSelectedListener false
            }
            showFragment(fragment)
            true
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
