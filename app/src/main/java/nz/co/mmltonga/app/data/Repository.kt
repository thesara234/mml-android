package nz.co.mmltonga.app.data

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import nz.co.mmltonga.app.BuildConfig
import nz.co.mmltonga.app.data.model.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import java.util.concurrent.TimeUnit

/** Error shown to the customer, with Laravel's per-field validation messages when present. */
class ApiError(message: String, val fields: Map<String, String> = emptyMap(), val unauthorised: Boolean = false) : Exception(message)

class Repository(val session: Session, val gson: Gson) {

    /** Emits when the server says the token is no longer valid, so the UI can return to sign-in. */
    private val _signedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signedOut: SharedFlow<Unit> = _signedOut

    private val api: ApiService by lazy {
        val auth = Interceptor { chain ->
            val b = chain.request().newBuilder().header("Accept", "application/json")
            session.token?.let { b.header("Authorization", "Bearer $it") }
            chain.proceed(b.build())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .apply { if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)) }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    /** Runs an API call and turns HTTP / network failures into a readable [ApiError]. */
    suspend fun <T> call(block: suspend ApiService.() -> T): T = try {
        api.block()
    } catch (e: HttpException) {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val json = runCatching { gson.fromJson(body, JsonObject::class.java) }.getOrNull()
        val fields = mutableMapOf<String, String>()
        json?.getAsJsonObject("errors")?.entrySet()?.forEach { entry ->
            runCatching { fields[entry.key] = entry.value.asJsonArray.first().asString }
        }
        val msg = fields.values.firstOrNull() ?: json?.get("message")?.takeIf { it.isJsonPrimitive }?.asString ?: "Something went wrong (${e.code()}). Please try again."
        if (e.code() == 401) {
            session.clear()
            _signedOut.tryEmit(Unit)
        }
        throw ApiError(msg, fields, unauthorised = e.code() == 401)
    } catch (e: IOException) {
        throw ApiError("Can't reach MML right now. Check your internet connection and try again.")
    } catch (e: CancellationException) {
        throw e // the screen was closed; let the coroutine stop normally
    } catch (e: ApiError) {
        throw e
    } catch (e: Exception) {
        // e.g. an unexpected reply the app couldn't read — show a message instead of crashing
        throw ApiError("Something went wrong talking to MML. Please try again.")
    }

    suspend fun signIn(identifier: String, password: String): Profile {
        val body = LoginRequest(identifier.trim(), password)
        val res = call { login(body) }
        session.save(res.token, res.customer)
        return res.customer
    }

    suspend fun signOut() {
        runCatching { call { logout() } }
        session.clear()
    }

    companion object {
        fun gson(): Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    }
}
