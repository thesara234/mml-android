@file:OptIn(ExperimentalMaterial3Api::class)

package nz.co.mmltonga.app.ui.activity

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import nz.co.mmltonga.app.data.ApiError
import nz.co.mmltonga.app.data.model.CancelBody
import nz.co.mmltonga.app.data.model.Transaction
import nz.co.mmltonga.app.ui.BaseViewModel
import nz.co.mmltonga.app.ui.components.*
import nz.co.mmltonga.app.ui.theme.LocalDark
import nz.co.mmltonga.app.ui.theme.Mml
import nz.co.mmltonga.app.ui.theme.MoneyStyle
import nz.co.mmltonga.app.ui.theme.statusTone

class TransactionViewModel(app: Application) : BaseViewModel(app) {
    var tx by mutableStateOf<Transaction?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf(false); private set

    fun load(id: Long) = viewModelScope.launch(safe) {
        try { tx = repo.call { transaction(id) }; error = null } catch (e: ApiError) { error = e.message }
    }

    fun cancel() {
        val t = tx ?: return
        busy = true
        viewModelScope.launch(safe) {
            try { tx = repo.call { cancel(t.id, CancelBody()) } } catch (e: ApiError) { error = e.message }
            busy = false
        }
    }
}

@Composable
fun TransactionScreen(
    id: Long,
    onBack: () -> Unit,
    onContinueDraft: (Long) -> Unit,
    onSendAgain: (Long) -> Unit,
    onChat: () -> Unit,
    vm: TransactionViewModel = viewModel(),
) {
    LaunchedEffect(id) { vm.load(id) }
    var confirmCancel by remember { mutableStateOf(false) }
    val t = vm.tx
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { Box(Modifier.statusBarsPadding()) { MmlTopBar(t?.referenceNo ?: "Transfer", onBack) } },
    ) { pad ->
        if (t == null) {
            Box(Modifier.padding(pad)) { if (vm.error != null) ErrorState(vm.error!!) { vm.load(id) } else LoadingBox() }
            return@Scaffold
        }
        val outgoing = t.direction == "outgoing"
        val tone = statusTone(t.status, LocalDark.current)
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ErrorBanner(vm.error)
            // Status header
            Surface(color = tone.bg, contentColor = tone.fg, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(t.statusLabel, style = MaterialTheme.typography.headlineSmall, color = tone.fg)
                    Text(
                        when (t.status) {
                            "awaiting_payment" -> if (outgoing) "Pay ${money(t.totalPayable, t.localCurrency)} to send it on its way." else "The sender is paying. Your code appears once it's ready."
                            "ready_for_payout" -> "${if (outgoing) t.receiver?.givenName ?: "The receiver" else "You"} can collect at ${t.receiverOffice?.name}."
                            "on_hold" -> t.holdReason ?: "MML is checking this transfer."
                            "completed" -> "Collected. Thank you for sending with MML."
                            "cancelled" -> "This transfer was cancelled. No money was taken."
                            "refunded" -> "The money was returned to the sender."
                            else -> ""
                        },
                    )
                }
            }

            // Payment instructions (sender, awaiting payment)
            t.paymentInstructions?.let { p ->
                SectionCard {
                    Text(if (p.method == "cash") "Pay in cash" else "Pay by internet banking", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    if (p.method == "cash") {
                        SummaryLine("Office", p.officeName ?: "—")
                        SummaryLine("Address", p.officeAddress ?: "—")
                        SummaryLine("Hours", p.openingHours ?: "—")
                        SummaryLine("Quote reference", p.reference, strong = true)
                    } else {
                        SummaryLine("Bank", p.bankName ?: "—")
                        SummaryLine("Account name", p.accountName ?: "—")
                        CopyLine("Account number", p.accountNumber ?: "—")
                        CopyLine("Reference", p.reference)
                    }
                    SummaryLine("Amount", money(p.amount, p.currency), strong = true)
                    Text("Use the reference exactly, so we can match your payment.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Collection code
            t.payoutCode?.let { code ->
                Surface(color = Mml.Moana900, contentColor = Color.White, shape = MaterialTheme.shapes.large) {
                    Column {
                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (outgoing) "Collection code: share it only with ${t.receiver?.givenName}" else "Your collection code", color = Color(0xFFBFD6E0), style = MaterialTheme.typography.labelLarge)
                            Text(code.chunked(3).joinToString(" "), style = MoneyStyle.copy(fontSize = 40.sp, letterSpacing = 4.sp, color = Color.White))
                        }
                        KupesiBand(height = 10.dp)
                    }
                }
            }

            // People & money
            SectionCard {
                t.sender?.let { PersonRow(it.fullName, "Sender, ${t.senderOffice?.name ?: ""}", it.photoUrl) }
                Spacer(Modifier.height(10.dp))
                t.receiver?.let { PersonRow(it.fullName, "Receiver, collects at ${t.receiverOffice?.name ?: ""}", it.photoUrl) }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                SummaryLine("Amount sent", money(t.transferAmount, t.localCurrency))
                SummaryLine("Rate", "1 ${t.localCurrency} = ${rateText(t.exchangeRate)} ${t.foreignCurrency}")
                if (outgoing) {
                    SummaryLine("Service fee", money(t.serviceFee, t.localCurrency))
                    if (t.discount > 0) SummaryLine("Discount", "− ${money(t.discount, t.localCurrency)}")
                    SummaryLine("Total", money(t.totalPayable, t.localCurrency), strong = true)
                }
                if (t.isGoods) SummaryLine("Shopping", money(t.goodsTotal, t.foreignCurrency))
                SummaryLine(if (t.isGoods) "Cash with shopping" else "Cash to collect", money(t.cashPayout, t.foreignCurrency), strong = true)
                t.purpose?.let { SummaryLine("Purpose", it) }
                t.paymentMethod?.let { SummaryLine("Payment", if (it == "cash") "Cash at office" else "Internet banking") }
            }

            if (t.items.isNotEmpty()) SectionCard {
                Text("Shopping list", style = MaterialTheme.typography.titleMedium)
                t.items.forEach { SummaryLine("${it.quantity} × ${it.name}", money(it.lineTotal)) }
            }

            // Timeline
            if (t.events.isNotEmpty()) SectionCard {
                Text("Progress", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                t.events.forEachIndexed { i, e ->
                    Row {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(statusTone(e.status).fg))
                            if (i < t.events.lastIndex) Box(Modifier.width(2.dp).height(38.dp).background(MaterialTheme.colorScheme.outline))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.padding(bottom = 10.dp)) {
                            Text(e.label, fontWeight = FontWeight.SemiBold)
                            Text(prettyDate(e.at), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Actions
            if (t.isDraft) Button(onClick = { onContinueDraft(t.id) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Continue this transfer") }
            if (outgoing && !t.isDraft && t.receiver != null && t.status in listOf("completed", "cancelled", "refunded")) {
                OutlinedButton(onClick = { onSendAgain(t.receiver.id) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Replay, null); Spacer(Modifier.width(8.dp)); Text("Send to ${t.receiver.givenName} again")
                }
            }
            if (outgoing && t.canCancel) {
                TextButton(onClick = { confirmCancel = true }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Cancel transfer")
                }
            }
            TextButton(onClick = onChat, modifier = Modifier.fillMaxWidth()) { Icon(Icons.AutoMirrored.Outlined.Chat, null); Spacer(Modifier.width(8.dp)); Text("Ask MML about this transfer") }
        }
    }

    if (confirmCancel) AlertDialog(
        onDismissRequest = { confirmCancel = false },
        title = { Text("Cancel this transfer?") },
        text = { Text("You haven't paid yet, so nothing needs refunding. This can't be undone.") },
        confirmButton = { TextButton(onClick = { confirmCancel = false; vm.cancel() }) { Text("Cancel transfer") } },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep it") } },
    )
}

@Composable
private fun CopyLine(label: String, value: String) {
    val clip = LocalClipboardManager.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { clip.setText(AnnotatedString(value)) }) { Icon(Icons.Outlined.ContentCopy, "Copy $label", Modifier.size(18.dp)) }
    }
}
