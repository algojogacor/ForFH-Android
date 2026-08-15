package com.aryariap.forfh.data.prefs

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Bungkus cookie sesi: AES-256-GCM, kunci non-exportable di Android Keystore.
 * Cookie diberikan ke OkHttp CookieJar HANYA dalam memori saat runtime (§7).
 */
class SecureCookieStore(
    private val dataStore: DataStore<Preferences>,
) {
    private val keyCipher = stringPreferencesKey("cookie_cipher")
    private val keyIv = stringPreferencesKey("cookie_iv")

    private val keyAlias = "forfh_session_key"

    suspend fun writeAll(map: Map<String, String>) {
        if (map.isEmpty()) { clear(); return }
        val (iv, ct) = runCatching { encrypt(CookiePayloadCodec.encode(map)) }
            .getOrElse { return } // key bermasalah → jangan simpan plaintext, jaga invariant
        dataStore.edit { p -> p[keyIv] = iv; p[keyCipher] = ct }
    }

    /** null = cookie tidak ada ATAU tak terbaca (key Keystore hilang) → alur auto-logout menangani. */
    suspend fun readAll(): Map<String, String>? {
        val p = dataStore.data.first()
        val iv = p[keyIv] ?: return null
        val ct = p[keyCipher] ?: return null
        val plain = runCatching { decrypt(iv, ct) }.getOrNull() ?: return null
        return CookiePayloadCodec.decode(plain)
    }

    suspend fun clear() {
        dataStore.edit { p -> p.remove(keyCipher); p.remove(keyIv) }
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) to
            Base64.encodeToString(ct, Base64.NO_WRAP)
    }

    private fun decrypt(ivB64: String, ctB64: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP)),
        )
        return String(cipher.doFinal(Base64.decode(ctB64, Base64.NO_WRAP)), Charsets.UTF_8)
    }
}
