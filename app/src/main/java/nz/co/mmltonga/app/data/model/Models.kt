package nz.co.mmltonga.app.data.model

/*
 * JSON models for the Laravel API (/api/v1). Gson is configured with
 * LOWER_CASE_WITH_UNDERSCORES, so `fullName` ⇄ "full_name".
 * Every property has a default so Gson can use the no-arg constructor.
 */

data class LoginRequest(val login: String = "", val password: String = "", val deviceName: String = "android")
data class LoginResponse(val token: String = "", val customer: Profile = Profile())

data class Profile(
    val id: Long = 0,
    val customerCode: String = "",
    val givenName: String = "",
    val familyName: String = "",
    val fullName: String = "",
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val residentialAddress: String? = null,
    val city: String? = null,
    val country: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val isVerified: Boolean = false,
    val homeOffice: String? = null,
    val homeCurrency: String? = null,
    /** Messages from MML staff the customer hasn't read yet. */
    val unreadMessages: Int = 0,
)

data class Person(
    val id: Long = 0,
    val customerCode: String = "",
    val fullName: String = "",
    val givenName: String = "",
    val familyName: String = "",
    val city: String? = null,
    val country: String? = null,
    val phone: String? = null,
    val photoUrl: String? = null,
    val isVerified: Boolean = false,
    val relationship: String? = null,
    val preferredOffice: Office? = null,
    // only sent for the customer's own saved receivers
    val residentialAddress: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
)

data class Office(
    val id: Long = 0,
    val code: String = "",
    val name: String = "",
    val city: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val currency: String = "",
    val hasStore: Boolean = false,
    val officeType: String = "both",
)

data class FeeBand(val min: Double = 0.0, val max: Double? = null, val type: String = "flat", val value: Double = 0.0)
data class Corridor(
    val localCurrency: String = "",
    val foreignCurrency: String = "",
    val rate: Double = 0.0,
    val effectiveDate: String? = null,
    val fees: List<FeeBand> = emptyList(),
)
data class Currency(val code: String = "", val name: String = "", val symbol: String = "", val countryCode: String = "")
data class PaymentMethod(val key: String = "", val label: String = "")

data class Bootstrap(
    val customer: Profile = Profile(),
    val corridors: List<Corridor> = emptyList(),
    val currencies: List<Currency> = emptyList(),
    val sendingOffices: List<Office> = emptyList(),
    val receivingOffices: List<Office> = emptyList(),
    val purposes: List<String> = emptyList(),
    val incomeSources: List<String> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val maxTransferAmount: Double = 10000.0,
)

data class QuoteRequest(
    val amount: Double = 0.0,
    val localCurrency: String = "",
    val foreignCurrency: String = "",
    val amountMode: String = "send",
)

data class Quote(
    val localCurrency: String = "",
    val foreignCurrency: String = "",
    val rate: Double = 0.0,
    val transferAmount: Double = 0.0,
    val convertedAmount: Double = 0.0,
    val serviceFee: Double = 0.0,
    val discount: Double = 0.0,
    val totalPayable: Double = 0.0,
    val feeDescription: String = "",
)

data class StockItem(
    val id: Long = 0,
    val sku: String? = null,
    val name: String = "",
    val category: String? = null,
    val description: String? = null,
    val unit: String = "each",
    val price: Double = 0.0,
    val currency: String = "",
    val inStock: Int = 0,
    val imageUrl: String? = null,
)
data class StoreItems(val office: Office = Office(), val categories: List<String> = emptyList(), val items: List<StockItem> = emptyList())

data class ItemLine(val stockItemId: Long = 0, val quantity: Int = 0)

/** Body for POST /transactions and PUT /transactions/{id}. action = "draft" | "send" */
data class TransferRequest(
    val action: String = "send",
    val transferType: String = "money",
    val amount: Double = 0.0,
    val amountMode: String = "send",
    val localCurrency: String = "NZD",
    val foreignCurrency: String = "TOP",
    val receiverCustomerId: Long? = null,
    val receiverOfficeId: Long? = null,
    val items: List<ItemLine> = emptyList(),
    val purpose: String? = null,
    val senderIncomeSource: String? = null,
    val paymentMethod: String? = null,
    val notes: String? = null,
)

data class TxItem(val stockItemId: Long = 0, val name: String = "", val unit: String = "", val quantity: Int = 0, val unitPrice: Double = 0.0, val lineTotal: Double = 0.0)
data class TxEvent(val status: String = "", val label: String = "", val remarks: String? = null, val at: String? = null)
data class PaymentInstructions(
    val method: String? = null,
    val amount: Double = 0.0,
    val currency: String = "",
    val reference: String = "",
    val bankName: String? = null,
    val accountName: String? = null,
    val accountNumber: String? = null,
    val officeName: String? = null,
    val officeAddress: String? = null,
    val openingHours: String? = null,
)

data class Transaction(
    val id: Long = 0,
    val referenceNo: String = "",
    val transactionDate: String? = null,
    val transferType: String = "money",
    val direction: String = "outgoing",
    val status: String = "",
    val statusLabel: String = "",
    val localCurrency: String = "",
    val foreignCurrency: String = "",
    val exchangeRate: Double = 0.0,
    val transferAmount: Double = 0.0,
    val convertedAmount: Double = 0.0,
    val serviceFee: Double = 0.0,
    val discount: Double = 0.0,
    val totalPayable: Double = 0.0,
    val goodsTotal: Double = 0.0,
    val cashPayout: Double = 0.0,
    val purpose: String? = null,
    val senderIncomeSource: String? = null,
    val paymentMethod: String? = null,
    val paymentStatus: String = "unpaid",
    val holdReason: String? = null,
    val payoutCode: String? = null,
    val sender: Person? = null,
    val receiver: Person? = null,
    val senderOffice: Office? = null,
    val receiverOffice: Office? = null,
    val items: List<TxItem> = emptyList(),
    val events: List<TxEvent> = emptyList(),
    val paymentInstructions: PaymentInstructions? = null,
) {
    val isGoods get() = transferType == "goods"
    val isDraft get() = status == "draft"
    val canCancel get() = (status == "draft" || status == "awaiting_payment") && paymentStatus != "paid"
}

data class ChatMessage(
    val id: Long = 0,
    val senderType: String = "customer",
    val senderName: String? = null,
    val body: String = "",
    val read: Boolean = false,
    val createdAt: String? = null,
    val time: String? = null,
)
data class ChatPage(val conversationId: Long = 0, val messages: List<ChatMessage> = emptyList(), val pollMs: Long = 5000)
data class ChatSend(val body: String = "")
data class LinkReceiver(val relationship: String? = null, val preferredOfficeId: Long? = null)
data class CancelBody(val reason: String? = null)
data class ApiMessage(val message: String? = null)
