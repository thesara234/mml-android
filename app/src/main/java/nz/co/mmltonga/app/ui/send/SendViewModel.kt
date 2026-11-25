package nz.co.mmltonga.app.ui.send

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nz.co.mmltonga.app.data.ApiError
import nz.co.mmltonga.app.data.model.*
import nz.co.mmltonga.app.ui.BaseViewModel
import kotlin.math.max
import kotlin.math.round

/** The steps of "Send money", in the order the business described. */
enum class Step(val title: String) {
    AMOUNT("Amount"),
    DELIVERY("Money or shopping"),
    SHOPPING("Shopping"),
    RECEIVER("Receiver"),
    REVIEW("Review & pay"),
}

class SendViewModel(app: Application) : BaseViewModel(app) {

    // reference data
    var boot by mutableStateOf<Bootstrap?>(null); private set
    var loading by mutableStateOf(true); private set
    var loadError by mutableStateOf<String?>(null); private set

    // navigation
    var step by mutableStateOf(Step.AMOUNT); private set
    val flow: List<Step> get() = if (type == "goods") Step.entries else Step.entries - Step.SHOPPING

    // 1. amount — entered in what they pay (send) or what the receiver gets (receive)
    var local by mutableStateOf("NZD"); private set
    var foreign by mutableStateOf("TOP"); private set
    var amountMode by mutableStateOf("send"); private set
    var amountText by mutableStateOf(""); private set
    var quote by mutableStateOf<Quote?>(null); private set
    var quoting by mutableStateOf(false); private set
    var quoteError by mutableStateOf<String?>(null); private set
    private var quoteJob: Job? = null

    // 2. money or shopping
    var type by mutableStateOf<String?>(null); private set

    // 3. shopping
    var stores by mutableStateOf<List<Office>>(emptyList()); private set
    var storeId by mutableStateOf<Long?>(null); private set
    var store by mutableStateOf<StoreItems?>(null); private set
    var category by mutableStateOf<String?>(null)
    var cart by mutableStateOf<Map<Long, Int>>(emptyMap()); private set
    var loadingItems by mutableStateOf(false); private set

    // 4. receiver
    var receivers by mutableStateOf<List<Person>>(emptyList()); private set
    var receiverId by mutableStateOf<Long?>(null); private set
    var picked by mutableStateOf<Person?>(null); private set
    var query by mutableStateOf(""); private set
    var results by mutableStateOf<List<Person>>(emptyList()); private set
    var searching by mutableStateOf(false); private set
    var collectOfficeId by mutableStateOf<Long?>(null)
    private var searchJob: Job? = null

    // 5. review
    var purpose by mutableStateOf<String?>(null)
    var incomeSource by mutableStateOf<String?>(null)
    var paymentMethod by mutableStateOf("internet_banking")
    var notes by mutableStateOf("")

    // submit
    var draftId by mutableStateOf<Long?>(null); private set
    var submitting by mutableStateOf(false); private set
    var banner by mutableStateOf<String?>(null)
    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap()); private set

    private var started = false

    fun start(draft: Long?, receiver: Long?) {
        if (started) return
        started = true
        viewModelScope.launch(safe) {
            try {
                val b = repo.call { bootstrap() }
                boot = b
                receivers = repo.call { receivers() }
                stores = b.receivingOffices.filter { it.hasStore }
                val home = b.customer.homeCurrency
                if (home != null && b.corridors.any { it.localCurrency == home }) local = home
                foreign = b.corridors.firstOrNull { it.localCurrency == local }?.foreignCurrency ?: foreign
                if (receiver != null) chooseReceiver(receivers.firstOrNull { it.id == receiver })
                if (draft != null) loadDraft(draft)
                loading = false
            } catch (e: ApiError) {
                loadError = e.message; loading = false
            }
        }
    }

    private suspend fun loadDraft(id: Long) {
        val t = repo.call { transaction(id) }
        draftId = t.id
        local = t.localCurrency; foreign = t.foreignCurrency
        amountMode = "send"; amountText = plain(t.transferAmount)
        type = t.transferType
        t.receiver?.let { r -> receiverId = r.id; if (receivers.none { it.id == r.id }) picked = r }
        collectOfficeId = t.receiverOffice?.id
        purpose = t.purpose; incomeSource = t.senderIncomeSource
        t.paymentMethod?.let { paymentMethod = it }
        if (t.isGoods && t.receiverOffice != null) {
            storeId = t.receiverOffice.id
            cart = t.items.associate { it.stockItemId to it.quantity }
            loadItems()
        }
        requote(immediate = true)
    }

    // ── corridor & amount ─────────────────────────────────────────

    val locals: List<String> get() = boot?.corridors?.map { it.localCurrency }?.distinct().orEmpty()
    val foreigns: List<String> get() = boot?.corridors?.filter { it.localCurrency == local }?.map { it.foreignCurrency }.orEmpty()
    /** The currency the amount field is in right now. */
    val inputCurrency: String get() = if (amountMode == "send") local else foreign

    fun setAmount(text: String) {
        val clean = text.filter { it.isDigit() || it == '.' }
        if (clean.count { it == '.' } > 1 || (clean.substringAfter('.', "").length > 2)) return
        amountText = clean
        requote()
    }

    /** "Fill the amount (NZD or TOP)": switch which side the customer is typing. */
    fun setMode(mode: String) {
        if (mode == amountMode) return
        quote?.let { amountText = plain(if (mode == "send") it.transferAmount else it.convertedAmount) }
        amountMode = mode
        requote(immediate = true)
    }

    fun setCurrencies(newLocal: String, newForeign: String? = null) {
        local = newLocal
        foreign = newForeign ?: foreigns.firstOrNull() ?: foreign
        if (type == "goods" && stores.none { it.currency == foreign }) type = null
        storeId = null; store = null; cart = emptyMap(); collectOfficeId = null
        requote(immediate = true)
    }

    private fun requote(immediate: Boolean = false) {
        quoteJob?.cancel()
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) { quote = null; quoteError = null; return }
        quoteJob = viewModelScope.launch(safe) {
            if (!immediate) delay(350)
            quoting = true
            try {
                quote = repo.call { quote(QuoteRequest(amount, local, foreign, amountMode)) }
                quoteError = null
            } catch (e: ApiError) { quote = null; quoteError = e.message }
            quoting = false
        }
    }

    // ── money or shopping ─────────────────────────────────────────

    fun chooseType(t: String) {
        type = t
        if (t == "goods") {
            if (storeId == null) storeId = storesForCurrency.firstOrNull()?.id
            collectOfficeId = storeId
        } else if (collectOfficeId != null && collectOffices.none { it.id == collectOfficeId }) collectOfficeId = null
    }

    val storesForCurrency: List<Office> get() = stores.filter { it.currency == foreign }

    // ── shopping ──────────────────────────────────────────────────

    fun chooseStore(id: Long) {
        if (id == storeId && store != null) return
        storeId = id; collectOfficeId = id; cart = emptyMap(); category = null
        loadItems()
    }

    fun loadItems() {
        val id = storeId ?: return
        loadingItems = true
        viewModelScope.launch(safe) {
            try { store = repo.call { storeItems(id) } } catch (e: ApiError) { banner = e.message }
            loadingItems = false
        }
    }

    fun qty(id: Long) = cart[id] ?: 0
    fun add(item: StockItem) { if (qty(item.id) < item.inStock) cart = cart + (item.id to qty(item.id) + 1) }
    fun remove(item: StockItem) { val q = qty(item.id); cart = if (q <= 1) cart - item.id else cart + (item.id to q - 1) }

    val cartLines: List<Pair<StockItem, Int>> get() = store?.items.orEmpty().mapNotNull { i -> cart[i.id]?.let { i to it } }
    val cartCount: Int get() = cart.values.sum()
    val cartTotal: Double get() = round(cartLines.sumOf { (i, q) -> i.price * q } * 100) / 100
    val budget: Double get() = quote?.convertedAmount ?: 0.0
    val overBudget: Boolean get() = cartTotal > budget + 0.001
    val cashWithShopping: Double get() = max(0.0, budget - cartTotal)

    /** Make the amount sent exactly cover the shopping. */
    fun matchAmountToCart() {
        if (cartTotal <= 0) return
        amountMode = "receive"
        amountText = plain(cartTotal)
        requote(immediate = true)
    }

    // ── receiver ──────────────────────────────────────────────────

    val collectOffices: List<Office>
        get() = boot?.receivingOffices.orEmpty().filter { it.currency == foreign && (type != "goods" || it.hasStore) }

    val receiver: Person? get() = receivers.firstOrNull { it.id == receiverId } ?: picked?.takeIf { it.id == receiverId }

    fun chooseReceiver(p: Person?) {
        p ?: return
        receiverId = p.id
        if (receivers.none { it.id == p.id }) picked = p
        if (type != "goods") {
            val pref = p.preferredOffice?.id
            collectOfficeId = if (pref != null && collectOffices.any { it.id == pref }) pref else collectOfficeId ?: collectOffices.firstOrNull()?.id
        }
        query = ""; results = emptyList()
    }

    fun search(text: String) {
        query = text
        searchJob?.cancel()
        if (text.trim().length < 3) { results = emptyList(); return }
        searchJob = viewModelScope.launch(safe) {
            delay(300)
            searching = true
            results = try { repo.call { searchReceivers(text.trim()) } } catch (_: ApiError) { emptyList() }
            searching = false
        }
    }

    // ── step control ──────────────────────────────────────────────

    private fun problem(s: Step): String? = when (s) {
        Step.AMOUNT -> when {
            amountText.toDoubleOrNull()?.let { it > 0 } != true -> "Enter how much you'd like to send."
            quoteError != null -> quoteError
            quote == null -> "Getting today's rate… try again in a moment."
            else -> null
        }
        Step.DELIVERY -> if (type == null) "Choose money or shopping." else null
        Step.SHOPPING -> when {
            cartCount == 0 -> "Add at least one item to the shopping list."
            overBudget -> "The shopping is ${String.format(java.util.Locale.US, "%.2f", cartTotal - budget)} $foreign more than you're sending. Remove items or tap “Match amount to shopping”."
            else -> null
        }
        Step.RECEIVER -> when {
            receiverId == null -> "Choose who will collect."
            collectOfficeId == null -> "Choose where they'll collect."
            else -> null
        }
        Step.REVIEW -> when {
            purpose == null -> "Choose the purpose of this transfer."
            incomeSource == null -> "Choose where the money comes from."
            else -> null
        }
    }

    fun next() {
        problem(step)?.let { banner = it; return }
        banner = null
        val f = flow
        step = f[minOf(f.indexOf(step) + 1, f.lastIndex)]
        if (step == Step.SHOPPING && store == null) {
            if (storeId == null) storeId = storesForCurrency.firstOrNull()?.id
            collectOfficeId = storeId
            loadItems()
        }
    }

    /** @return false when already at the first step (caller should close the screen) */
    fun back(): Boolean {
        banner = null
        val i = flow.indexOf(step)
        if (i <= 0) return false
        step = flow[i - 1]
        return true
    }

    fun goTo(s: Step) { if (flow.indexOf(s) < flow.indexOf(step)) { step = s; banner = null } }

    // ── submit ────────────────────────────────────────────────────

    private fun body(action: String) = TransferRequest(
        action = action,
        transferType = type ?: "money",
        amount = amountText.toDoubleOrNull() ?: 0.0,
        amountMode = amountMode,
        localCurrency = local,
        foreignCurrency = foreign,
        receiverCustomerId = receiverId,
        receiverOfficeId = collectOfficeId,
        items = if (type == "goods") cart.map { (id, q) -> ItemLine(id, q) } else emptyList(),
        purpose = purpose,
        senderIncomeSource = incomeSource,
        paymentMethod = paymentMethod,
        notes = notes.ifBlank { null },
    )

    fun submit(action: String, onDone: (Transaction) -> Unit) {
        if (action == "send") problem(Step.REVIEW)?.let { banner = it; return }
        if (action == "draft" && quote == null) { banner = "Enter an amount before saving."; return }
        submitting = true; banner = null; fieldErrors = emptyMap()
        viewModelScope.launch(safe) {
            try {
                val id = draftId
                val t = if (id != null) repo.call { update(id, body(action)) } else repo.call { create(body(action)) }
                onDone(t)
            } catch (e: ApiError) {
                fieldErrors = e.fields
                banner = e.message
                // jump back to the step that needs fixing
                val k = e.fields.keys.firstOrNull()
                when {
                    k == null -> {}
                    k.startsWith("items") -> if (Step.SHOPPING in flow) step = Step.SHOPPING
                    k.startsWith("receiver") -> step = Step.RECEIVER
                    k in listOf("amount", "transfer_amount", "local_currency", "foreign_currency") -> step = Step.AMOUNT
                }
            }
            submitting = false
        }
    }

    private fun plain(d: Double) = if (d % 1.0 == 0.0) d.toLong().toString() else String.format(java.util.Locale.US, "%.2f", d)
}
