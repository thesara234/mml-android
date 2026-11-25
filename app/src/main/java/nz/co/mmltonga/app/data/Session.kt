package nz.co.mmltonga.app.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import nz.co.mmltonga.app.data.model.Profile

/** Stores the Sanctum token and the signed-in customer's profile. */
class Session(context: Context, private val gson: Gson) {
    private val prefs = context.getSharedPreferences("mml_session", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(readProfile())
    val profile: StateFlow<Profile?> = _profile

    val token: String? get() = prefs.getString(KEY_TOKEN, null)
    val isSignedIn: Boolean get() = token != null

    fun save(token: String, profile: Profile) {
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_PROFILE, gson.toJson(profile)).apply()
        _profile.value = profile
    }

    fun updateProfile(profile: Profile) {
        prefs.edit().putString(KEY_PROFILE, gson.toJson(profile)).apply()
        _profile.value = profile
    }

    fun clear() {
        prefs.edit().clear().apply()
        _profile.value = null
    }

    private fun readProfile(): Profile? =
        prefs.getString(KEY_PROFILE, null)?.let { runCatching { gson.fromJson(it, Profile::class.java) }.getOrNull() }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_PROFILE = "profile"
    }
}
