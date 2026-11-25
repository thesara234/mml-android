package nz.co.mmltonga.app.data

import nz.co.mmltonga.app.data.model.*
import retrofit2.http.*

interface ApiService {
    @POST("login") suspend fun login(@Body body: LoginRequest): LoginResponse
    @POST("logout") suspend fun logout(): ApiMessage
    @GET("me") suspend fun me(): Profile

    @GET("bootstrap") suspend fun bootstrap(): Bootstrap
    @POST("quote") suspend fun quote(@Body body: QuoteRequest): Quote
    @GET("stores") suspend fun stores(): List<Office>
    @GET("stores/{office}/items") suspend fun storeItems(@Path("office") officeId: Long): StoreItems

    @GET("receivers") suspend fun receivers(): List<Person>
    @GET("receivers/search") suspend fun searchReceivers(@Query("q") q: String): List<Person>
    @POST("receivers/{id}") suspend fun linkReceiver(@Path("id") id: Long, @Body body: LinkReceiver): Person

    @GET("transactions") suspend fun transactions(@Query("box") box: String): List<Transaction>
    @GET("transactions/{id}") suspend fun transaction(@Path("id") id: Long): Transaction
    @POST("transactions") suspend fun create(@Body body: TransferRequest): Transaction
    @PUT("transactions/{id}") suspend fun update(@Path("id") id: Long, @Body body: TransferRequest): Transaction
    @POST("transactions/{id}/cancel") suspend fun cancel(@Path("id") id: Long, @Body body: CancelBody): Transaction
    @DELETE("transactions/{id}") suspend fun deleteDraft(@Path("id") id: Long): ApiMessage

    /** type = "money" | "goods"; scope = "open" | "all" */
    @GET("incoming") suspend fun incoming(@Query("type") type: String?, @Query("scope") scope: String = "open"): List<Transaction>

    @GET("chat/messages") suspend fun chat(@Query("after_id") afterId: Long): ChatPage
    @POST("chat/messages") suspend fun sendChat(@Body body: ChatSend): ChatMessage
}
