@file:OptIn(ExperimentalMaterial3Api::class)

package nz.co.mmltonga.app.ui.receive

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import nz.co.mmltonga.app.data.ApiError
import nz.co.mmltonga.app.data.model.Transaction
import nz.co.mmltonga.app.ui.BaseViewModel
import nz.co.mmltonga.app.ui.components.*
import nz.co.mmltonga.app.ui.home.TxRow
import nz.co.mmltonga.app.ui.theme.Mml
import nz.co.mmltonga.app.ui.theme.MoneyStyle

class ReceiveViewModel(app: Application) : BaseViewModel(app) {
    var type by mutableStateOf("money"); private set
    var open by mutableStateOf<List<Transaction>>(emptyList()); private set
    var history by mutableStateOf<List<Transaction>>(emptyList()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set

    fun load(t: String) {
        type = t; loading = true
        viewModelScope.launch(safe) {
            try {
                val all = repo.call { incoming(t, "all") }
                open = all.filter { it.status in listOf("ready_for_payout", "awaiting_payment", "on_hold") }
                history = all.filter { it.status !in listOf("ready_for_payout", "awaiting_payment", "on_hold") }
                error = null
            } catch (e: ApiError) { error = e.message }
            loading = false
        }
    }
}

/**
 * "Receive money" and "Receive shopping". Receivers can only collect at an MML office,
 * so this screen shows what's waiting, where, and the code to give the teller.
 */
@Composable
fun ReceiveScreen(type: String, onBack: () -> Unit, onOpenTx: (Long) -> Unit, onChat: () -> Unit, vm: ReceiveViewModel = viewModel()) {
    LaunchedEffect(type) { vm.load(type) }
    val goods = type == "goods"
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { Box(Modifier.statusBarsPadding()) { MmlTopBar(if (goods) "Receive shopping" else "Receive money", onBack) } },
    ) { pad ->
        PullToRefreshBox(isRefreshing = vm.loading, onRefresh = { vm.load(type) }, modifier = Modifier.padding(pad)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    // Business rule: receiving is only ever in person at an MML office.
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.Storefront, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (goods) "Shopping is collected in person at the MML store shown. Bring your photo ID and your collection code."
                                else "Money is collected in person at the MML office shown. Bring your photo ID and your collection code.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                vm.error?.let { item { ErrorBanner(it) } }
                if (!vm.loading && vm.open.isEmpty()) item {
                    EmptyState(
                        if (goods) Icons.Outlined.ShoppingBasket else Icons.Outlined.Payments,
                        if (goods) "No shopping waiting for you" else "No money waiting for you",
                        "When family sends you ${if (goods) "shopping" else "money"}, it shows here with your collection code.",
                    )
                }
                items(vm.open, key = { it.id }) { t -> CollectCard(t, onOpen = { onOpenTx(t.id) }) }
                if (vm.history.isNotEmpty()) {
                    item { Text("Collected before", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
                    items(vm.history, key = { "h${it.id}" }) { t -> TxRow(t, onClick = { onOpenTx(t.id) }) }
                }
                item {
                    TextButton(onClick = onChat, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, null); Spacer(Modifier.width(8.dp)); Text("Questions? Message MML")
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectCard(t: Transaction, onOpen: () -> Unit) {
    val ready = t.status == "ready_for_payout"
    Surface(onClick = onOpen, shape = MaterialTheme.shapes.large, color = if (ready) Mml.Moana900 else MaterialTheme.colorScheme.surface,
        contentColor = if (ready) Color.White else MaterialTheme.colorScheme.onSurface,
        border = if (ready) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(t.sender?.fullName, t.sender?.photoUrl, 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("From ${t.sender?.fullName ?: "—"}", fontWeight = FontWeight.SemiBold)
                        Text(prettyDate(t.transactionDate, "d MMM yyyy"), style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = .7f))
                    }
                    StatusChip(t.status, t.statusLabel)
                }
                Spacer(Modifier.height(14.dp))
                if (t.isGoods) {
                    Text("Shopping worth ${money(t.goodsTotal, t.foreignCurrency)}", style = MaterialTheme.typography.titleMedium)
                    t.items.take(4).forEach { Text("${it.quantity} × ${it.name}", style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = .8f)) }
                    if (t.items.size > 4) Text("+ ${t.items.size - 4} more", style = MaterialTheme.typography.bodySmall)
                    if (t.cashPayout > 0) Text("Plus ${money(t.cashPayout, t.foreignCurrency)} cash", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                } else {
                    Text("You collect", style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = .7f))
                    Text(money(t.cashPayout, t.foreignCurrency), style = MoneyStyle.copy(color = LocalContentColor.current))
                }

                Spacer(Modifier.height(14.dp))
                if (ready && t.payoutCode != null) {
                    Surface(color = Color.White.copy(alpha = .1f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Collection code", style = MaterialTheme.typography.labelMedium, color = Color(0xFFBFD6E0))
                            Text(t.payoutCode.chunked(3).joinToString(" "), style = MoneyStyle.copy(fontSize = 38.sp, letterSpacing = 4.sp, color = Color.White))
                            Text("Show this and your photo ID at the counter", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFD6E0), textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    Text(
                        when (t.status) {
                            "awaiting_payment" -> "On its way. The sender is paying at the MML office. We'll show your code here once it's ready."
                            "on_hold" -> "On hold. ${t.holdReason ?: "MML is checking this transfer."}"
                            else -> t.statusLabel
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                t.receiverOffice?.let { o ->
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Place, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(listOfNotNull(o.name, o.openingHours).joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (ready) KupesiBand(height = 10.dp)
        }
    }
}
