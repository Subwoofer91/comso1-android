package com.codexremote.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.codexremote.domain.PhoneLocation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class PhoneLocationProvider(private val context: Context) {
    suspend fun current(): PhoneLocation {
        checkPermission()
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val candidates = if (hasFine) {
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        val enabledProviders = candidates
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (enabledProviders.isEmpty()) error("请先开启手机定位服务")

        enabledProviders.forEach { provider ->
            val timeoutMillis = when {
                enabledProviders.size == 1 -> 12_000L
                provider == LocationManager.NETWORK_PROVIDER -> 6_000L
                else -> 10_000L
            }
            val fresh = runCatching {
                withTimeoutOrNull(timeoutMillis) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        currentModern(manager, provider)
                    } else {
                        currentLegacy(manager, provider)
                    }
                }
            }.getOrNull()
            if (fresh != null) return fresh.toDomain(isCached = false)
        }

        val cached = lastKnown(manager, enabledProviders)
            ?: error("暂时无法获取位置，请到室外重试或检查系统定位设置")
        return cached.toDomain(isCached = true)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager, providers: List<String>): Location? = providers
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }

    private fun checkPermission() {
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        require(fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            "尚未授予定位权限"
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private suspend fun currentModern(manager: LocationManager, provider: String): Location =
        suspendCancellableCoroutine { continuation ->
            manager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                if (!continuation.isActive) return@getCurrentLocation
                if (location != null) continuation.resume(location)
                else continuation.resumeWithException(IllegalStateException("暂时无法获取当前位置"))
            }
        }

    @android.annotation.SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private suspend fun currentLegacy(manager: LocationManager, provider: String): Location =
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }

                override fun onProviderDisabled(provider: String) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("定位服务已关闭"))
                    }
                }

                override fun onProviderEnabled(provider: String) = Unit
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        }

    private fun Location.toDomain(isCached: Boolean) = PhoneLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        capturedAt = time.takeIf { it > 0L } ?: System.currentTimeMillis(),
        isCached = isCached,
    )
}
