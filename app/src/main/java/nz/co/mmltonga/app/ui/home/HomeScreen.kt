@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package nz.co.mmltonga.app.ui.home

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import nz.co.mmltonga.app.data.ApiError
import nz.co.mmltonga.app.data.model.Corridor
import nz.co.mmltonga.app.data.model.Transaction
import nz.co.mmltonga.app.ui.BaseViewModel
import nz.co.mmltonga.app.ui.components.*
import nz.co.mmltonga.app.ui.theme.DisplayFamily
import nz.co.mmltonga.app.ui.theme.Mml

class HomeViewModel(app: Application) : BaseViewModel(app) {
    val profile = repo.session.profile
    var corridors by mutableStateOf<List<Corridor>>(emptyList()); private set
    var recent by mutableStateOf<List<Transaction>>(emptyList()); private set
    var drafts by mutableStateOf<List<Transaction>>(emptyList()); private set
    var readyMoney by mutableIntStateOf(0); private set
    var readyGoods by mutableIntStateOf(0); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    init { refresh() }

    fun refresh() {
        if (loading) return
        loading = true
        viewModelScope.launch(safe) {
            // One request at a time: gentle on `php artisan serve`, which handles requests one by one,
            // and a failure in any of them shows a message instead of closing the app.
            try {
                val b = repo.call { bootstrap() }
                repo.session.updateProfile(b.customer)
                corridors = b.corridors
                recent = repo.call { transactions("sent") }.take(5)
                drafts = repo.call { transactions("drafts") }.take(3)
                val ready = repo.call { incoming(null, "open") }.filter { it.status == "ready_for_payout" }
                readyMoney = ready.count { !it.isGoods }
                readyGoods = ready.count { it.isGoods }
                error = null
            } catch (e: ApiError) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }
}

/**
 * After login the customer sees three clear choices:
 *   1) Send money   2) Receive money   3) Receive shopping
 */
@Composable
fun HomeScreen(
    onSend: () -> Unit,
    onReceive: (String) -> Unit,
    onOpenTx: (Transaction) -> Unit,
    onSeeAll: () -> Unit,
    onChat: () -> Unit = {},
    vm: HomeViewModel = viewModel(),
) {
    val me by vm.profile.collectAsState()
    PullToRefreshBox(
        isRefreshing = vm.loading,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
            // ── Greeting header ──────────────────────────────────
            item {
                Column(Modifier.fillMaxWidth().background(Mml.Moana900).statusBarsPadding()) {
                    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Melie Mei Langi", color = Color.White, fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Spacer(Modifier.weight(1f))
                        Avatar(me?.fullName, me?.photoUrl, 36.dp, container = Color.White.copy(alpha = .14f))
                    }
                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 30.dp)) {
                        Text("Mālō e lelei, ${me?.givenName.orEmpty()}", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("What would you like to do today?", color = Color(0xFFA8C6D4), style = MaterialTheme.typography.bodyLarge)
                    }
                    KupesiBand(height = 14.dp)
                }
            }

            // ── New message from MML staff ───────────────────────
            val unread = me?.unreadMessages ?: 0
            if (unread > 0) item {
                Surface(
                    onClick = onChat,
                    color = Mml.Lagoon100,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, null, tint = Mml.Lagoon600)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (unread == 1) "1 new message from MML" else "$unread new messages from MML",
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text("Tap to read and reply", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = Mml.Lagoon600)
                    }
                }
            }

            // ── The three options ────────────────────────────────
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OptionCard(
                        icon = Icons.AutoMirrored.Outlined.Send,
                        title = "Send money",
                        subtitle = "Send cash or shopping to family. Pay by internet banking or cash at an MML office.",
                        container = Mml.Kula600, content = Color.White,
                        iconBg = Color.White.copy(alpha = .18f), iconTint = Color.White,
                        band = true, onClick = onSend,
                    )
                    OptionCard(
                        icon = Icons.Outlined.Payments,
                        title = "Receive money",
                        subtitle = "See money sent to you and collect it at an MML office.",
                        badge = vm.readyMoney.takeIf { it > 0 }?.let { "$it ready to collect" },
                        iconBg = Mml.Lagoon100, iconTint = Mml.Lagoon600,
                        onClick = { onReceive("money") },
                    )
                    OptionCard(
                        icon = Icons.Outlined.ShoppingBasket,
                        title = "Receive shopping",
                        subtitle = "See shopping sent to you and pick it up from an MML store.",
                        badge = vm.readyGoods.takeIf { it > 0 }?.let { "$it ready to collect" },
                        iconBg = Mml.Sun100, iconTint = Color(0xFF9A5B00),
                        onClick = { onReceive("goods") },
                    )
                }
            }

            vm.error?.let { item { ErrorBanner(it, Modifier.padding(horizontal = 16.dp)) } }

            // ── Today's rates ────────────────────────────────────
            if (vm.corridors.isNotEmpty()) {
                item { SectionTitle("Today's rates") }
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        vm.corridors.forEach { c -> RateChip(c) }
                    }
                }
            }

            // ── Saved for later ──────────────────────────────────
            if (vm.drafts.isNotEmpty()) {
                item { SectionTitle("Saved for later") }
                items(vm.drafts, key = { "d${it.id}" }) { t ->
                    TxRow(t, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { onOpenTx(t) }, leadingIcon = Icons.Outlined.Bookmark)
                }
            }

            // ── Recent ───────────────────────────────────────────
            item { SectionTitle("Recent transfers", action = "See all", onAction = onSeeAll) }
            if (vm.recent.isEmpty() && !vm.loading) item {
                EmptyState(Icons.AutoMirrored.Outlined.Send, "No transfers yet", "Your transfers will appear here after you send.")
            }
            items(vm.recent, key = { it.id }) { t ->
                TxRow(t, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { onOpenTx(t) })
            }
        }
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconBg: Color,
    iconTint: Color,
    container: Color = MaterialTheme.colorScheme.surface,
    content: Color = MaterialTheme.colorScheme.onSurface,
    badge: String? = null,
    band: Boolean = false,
) {
    val outlined = container == MaterialTheme.colorScheme.surface
    Surface(
        onClick = onClick,
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(22.dp),
        border = if (outlined) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        shadowElevation = if (outlined) 0.dp else 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = .78f))
                    if (badge != null) {
                        Spacer(Modifier.height(8.dp))
                        StatusChip("ready_for_payout", badge)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = content.copy(alpha = .6f))
            }
            if (band) KupesiBand(height = 10.dp, red = Color.White.copy(alpha = .25f), other = Color.Transparent)
        }
    }
}

@Composable
private fun RateChip(c: Corridor) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("${c.localCurrency} → ${c.foreignCurrency}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rateText(c.rate), fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
        }
    }
}

@Composable
private fun SectionTitle(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) {
            Text(action)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp))
        }
    }
}

/** One transfer in a list. Used by home, activity and receive screens. */
@Composable
fun TxRow(t: Transaction, modifier: Modifier = Modifier, onClick: () -> Unit, leadingIcon: ImageVector? = null) {
    val incoming = t.direction == "incoming"
    val other = if (incoming) t.sender else t.receiver
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { Icon(leadingIcon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                Avatar(other?.fullName, other?.photoUrl)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(other?.fullName ?: "Receiver not chosen yet", fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    "${if (incoming) "From" else "To"} ${other?.givenName ?: "—"}, ${if (t.isGoods) "shopping" else "money"}, ${prettyDate(t.transactionDate, "d MMM")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (incoming) money(t.convertedAmount, t.foreignCurrency) else money(t.transferAmount, t.localCurrency),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                StatusChip(t.status, t.statusLabel)
            }
        }
    }
}
