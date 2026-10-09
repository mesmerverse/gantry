package com.mesmerverse.gantry.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.LauncherUserInfo
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * [entryHidden] mirrors the system "Hide private space when locked" setting.
 * While it's on and the space is sealed, nothing may hint the space exists.
 */
data class PrivateSpace(
    val profile: UserHandle,
    val unlocked: Boolean,
    val entryHidden: Boolean = false,
) {
    /** Sealed with the system hide setting on: show no trace of it. */
    val concealed: Boolean get() = entryHidden && !unlocked
}

/**
 * Android 15+ private space. Only the default launcher (HOME role holder)
 * can see the private profile and toggle its quiet mode; for everyone else
 * [state] is null.
 */
class PrivateSpaceRepository(private val context: Context) {

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(UserManager::class.java)

    val state: Flow<PrivateSpace?> = callbackFlow {
        fun query(): PrivateSpace? {
            // Private space arrived in Android 15.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null
            val result = runCatching {
                launcherApps.profiles
                    .firstOrNull { profile ->
                        profile != Process.myUserHandle() && runCatching {
                            // UserManager.USER_TYPE_PROFILE_PRIVATE is not public API.
                            launcherApps.getLauncherUserInfo(profile)?.userType == PRIVATE_TYPE
                        }.getOrDefault(false)
                    }
                    ?.let {
                        PrivateSpace(
                            profile = it,
                            unlocked = userManager?.isQuietModeEnabled(it) == false,
                            entryHidden = entryPointHidden(it),
                        )
                    }
            }
            val debuggable = context.applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
            if (debuggable) {
                Log.d(
                    TAG,
                    "query -> ${result.getOrNull()} " +
                        "(profiles=${runCatching { launcherApps.profiles }.getOrNull()}, " +
                        "error=${result.exceptionOrNull()})"
                )
            }
            return result.getOrNull()
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                trySend(query())
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_PROFILE_ADDED)
                addAction(Intent.ACTION_PROFILE_REMOVED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        trySend(query())
        awaitClose { context.unregisterReceiver(receiver) }
    }

    // Android 16 exposes the hide-when-locked setting to the HOME role;
    // older releases (or a denied lookup) keep the entry point visible.
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun entryPointHidden(profile: UserHandle): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        launcherApps.getLauncherUserInfo(profile)?.userConfig
            ?.getBoolean(LauncherUserInfo.PRIVATE_SPACE_ENTRYPOINT_HIDDEN, false) == true
    }.getOrDefault(false)

    /**
     * Lock (quiet mode on) or unlock the private space. Unlocking triggers
     * the system authentication prompt. Requires the HOME role.
     */
    suspend fun setLocked(locked: Boolean, profile: UserHandle) = withContext(Dispatchers.IO) {
        runCatching { userManager?.requestQuietModeEnabled(locked, profile) }
            .onFailure { Log.w(TAG, "requestQuietModeEnabled($locked) failed", it) }
    }

    private companion object {
        const val PRIVATE_TYPE = "android.os.usertype.profile.PRIVATE"
        const val TAG = "GantryPrivateSpace"
    }
}
