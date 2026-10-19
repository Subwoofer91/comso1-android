package com.codexremote.store

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.UserInfo

@SuppressLint("ApplySharedPref")
class TofuHostKeyRepository(context: Context) : HostKeyRepository {
    private val prefs = context.getSharedPreferences("codex_remote_known_hosts", Context.MODE_PRIVATE)

    override fun check(host: String, key: ByteArray): Int {
        val encoded = Base64.encodeToString(key, Base64.NO_WRAP)
        val saved = prefs.getString(host, null)
        return when {
            saved == null -> {
                prefs.edit().putString(host, encoded).commit()
                HostKeyRepository.OK
            }
            saved == encoded -> HostKeyRepository.OK
            else -> HostKeyRepository.CHANGED
        }
    }

    override fun add(hostkey: HostKey, userinfo: UserInfo?) {
        prefs.edit().putString(hostkey.host, hostkey.key).commit()
    }

    override fun remove(host: String, type: String?) {
        prefs.edit().remove(host).apply()
    }

    override fun remove(host: String, type: String?, key: ByteArray?) {
        if (key == null || check(host, key) == HostKeyRepository.OK) {
            prefs.edit().remove(host).apply()
        }
    }

    override fun getKnownHostsRepositoryID(): String = "Android private app storage (TOFU)"

    override fun getHostKey(): Array<HostKey> = prefs.all.mapNotNull { (host, value) ->
        decodeHostKey(host, value as? String)
    }.toTypedArray()

    override fun getHostKey(host: String?, type: String?): Array<HostKey> {
        if (host == null) return getHostKey()
        return listOfNotNull(decodeHostKey(host, prefs.getString(host, null)))
            .filter { type == null || it.type == type }
            .toTypedArray()
    }

    private fun decodeHostKey(host: String, encoded: String?): HostKey? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            HostKey(host, Base64.decode(encoded, Base64.NO_WRAP))
        }.getOrNull()
    }
}
