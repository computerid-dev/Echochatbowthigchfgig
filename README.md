# EchoChat

Aplikasi chat pribadi ala WhatsApp — akun tamu (tanpa OTP/OAuth), UID acak per perangkat, sinkron real-time lewat Firestore.

**Slogan:** Ngobrol tanpa jejak, sepenuhnya milikmu
**Package:** `com.echochat.cid`
**Versi:** v1.7

## Fitur v1.7

- Chat teks 1-on-1 real-time + validasi UID aktif sebelum tambah teman
- **Kirim gambar di chat** (baru): foto dikompres jadi base64 (maks ~1080px, kualitas diturunkan otomatis kalau perlu) lalu dikirim lewat dokumen Firestore yang sama seperti pesan teks — konsisten dengan cara avatar bekerja. Ikut tersimpan di cache lokal, notifikasi latar belakang, preview daftar chat ("📷 Foto"), dan backup/restore.
- **Avatar**: foto profil sendiri (di tab Info Akun) maupun avatar teman (otomatis tersinkron & tampil di daftar chat, daftar kontak, dan info teman)
- Blokir & hapus pertemanan (otomatis hapus riwayat chat)
- Navbar bawah 4 tab: **Chat** (badge belum-dibaca & pesan terakhir), **Kontak**, **Info Akun**, **Opsi Developer**
- **Dark mode** — toggle di Pengaturan
- **Desain "Professional Polish"**: palet hijau tua + putih + hitam + abu-abu, bentuk geometris tegas (kotak = sudut tajam, avatar = lingkaran sempurna, badge notifikasi = pil/lonjong sempurna)
- **Wallpaper chat** custom per gambar, dengan 4 mode cakupan (semua/kontak tersimpan/kontak tertentu/tidak ada) — wallpaper tidak ikut ke-backup
- **UID bisa disembunyikan** dari tampilan
- **Link otomatis** biru+underline di chat
- **Notifikasi latar belakang 24/7** via Foreground Service (mirip app musik/VPN) — user perlu nonaktifkan optimasi baterai manual di HP-nya
- **Backup/restore** jadi satu file `.zip`: `profile.json`, `contacts.json`, dan per kontak `<nama>-<uid>-avatar.json` (base64) + `<nama>-<uid>-fullchat.json` (termasuk gambar chat, base64). Bisa dipulihkan (termasuk UID lama) lewat tombol "Impor data" di pojok kiri atas layar setup — berguna setelah install ulang.

> **Catatan v1.6:** fitur **grup chat sudah dicabut total** dari aplikasi ini. Alasannya: bug-nya berlapis (gagal tambah/hapus anggota, anggota baru tidak muncul, teks pesan grup tidak muncul, info grup gagal dimuat) dan root cause-nya melibatkan banyak bagian sekaligus (aturan Firestore, sinkron lokal, dsb) sehingga diputuskan lebih baik dihapus daripada terus jadi sumber crash. Aplikasi sekarang fokus penuh ke chat 1-on-1 yang stabil.

Tidak ada fitur video/audio maupun telepon/panggilan video — chat sengaja teks + gambar saja.

## Setup Firebase (wajib sebelum build)

1. `app/google-services.json` sudah disertakan (project Firebase milik pembuat proyek).
2. Firebase Console → **Firestore Database** → aktifkan (mode Native).
3. Tab **Rules** → tempel isi [`firestore.rules`](./firestore.rules) → **Publish**.
4. **Composite index**: fitur notifikasi latar belakang memakai collection-group query (`messages` + `array-contains participants` + `orderBy timestamp`). Firestore akan menolak query ini sampai index-nya dibuat. Cara termudah: aktifkan dulu toggle "Notifikasi latar belakang" di Pengaturan lewat app yang sudah running (misal via `adb logcat` atau Android Studio Logcat), lalu cari error dari Firestore yang berisi link `https://console.firebase.google.com/.../create_composite_index?...` — klik link itu sekali, tunggu index selesai dibangun (beberapa menit), habis itu fitur ini akan langsung jalan otomatis untuk semua user selanjutnya.
5. Tidak perlu SHA-1 apa pun — tidak pakai Firebase Auth/Google Sign-In.

## Build APK

### Lewat Android Studio
Buka folder ini → Run/Build → `app-debug.apk` muncul di `app/build/outputs/apk/debug/`.

### Lewat GitHub Actions
Push ke branch `main`, atau jalankan manual lewat tab **Actions** → **Build APK** → *Run workflow*. Unduh `app-debug` dari bagian **Artifacts**.

## Catatan & keterbatasan v1.7

- Grup chat sudah tidak ada (lihat catatan di atas). Data grup lama di Firestore (kalau ada) tidak lagi dipakai aplikasi dan aman untuk dihapus manual dari Firebase Console.
- Karena skema database lokal berubah beberapa kali (kolom avatar teman, kolom gambar chat, hapus tabel grup), **data lokal lama akan ter-reset otomatis** saat pertama kali update ke versi ini (fallback destructive migration Room) — sebaiknya ekspor backup dulu sebelum update kalau datanya penting.
- **Kirim gambar**: satu gambar per pesan (belum bisa multi-pilih), tidak ada video/GIF/dokumen, dan tidak ada kompresi ulang di sisi penerima — gambar dikirim apa adanya hasil kompresi otomatis di HP pengirim. Kalau gambar sumbernya sangat detail (misal hasil scan dokumen), hasil kompresi bisa terlihat sedikit buram karena batas ukuran dokumen Firestore (1MB).
- HP dengan battery-optimizer agresif (MIUI/ColorOS/Vivo) bisa tetap mematikan service walau sudah di-whitelist manual — ini kebiasaan pabrikan, bukan bug aplikasi.
- Foreground service berhenti otomatis saat HP di-restart; user perlu buka app lagi untuk mengaktifkan ulang.

## Struktur proyek

```
app/src/main/java/com/echochat/cid/
├── data/       Entity, DAO, Room database, FirestoreRepository, BackupManager
├── ui/         Activity, Fragment (navbar bawah), adapter RecyclerView
├── service/    NotificationListenerService (foreground service notifikasi)
└── util/       SessionManager, UidGenerator, ImageUtils
```
