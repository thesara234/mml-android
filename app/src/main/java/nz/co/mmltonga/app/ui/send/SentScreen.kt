@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package nz.co.mmltonga.app.ui.send

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import nz.co.mmltonga.app.ui.theme.Mml
import nz.co.mmltonga.app.ui.theme.MoneyStyle

class SentViewModel(app: Application) : BaseViewModel(app) {
    var tx by mutableStateOf<Transaction?>(null); private set
    var error by mutableStateOf<String?>(null); private set

    fun load(id: Long) {
        viewModelScope.launch(safe) {
            try {
                tx = repo.call { transaction(id) }
                error = null
            } catch (e: ApiError) {
                error = e.message
            }
        }
    }
}

/** Shown right after "Send" or "Save for later". */
@Composable
fun SentScreen(
    id: Long,
    onHome: () -> Unit,
    onView: (Long) -> Unit,
    onContinueDraft: (Long) -> Unit,
    vm: SentViewModel = viewModel(),
) {
    LaunchedEffect(id) { vm.load(id) }
    BackHandler { onHome() }
    val t = vm.tx

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        if (t == null) {
            if (vm.error != null) ErrorState(vm.error ?: "") { vm.load(id) } else LoadingBox()
            return@Column
        }
        val draft = t.isDraft

        // ── Headline ─────────────────────────────────────────────
        Column(
            Modifier.fillMaxWidth().padding(top = 36.dp, start = 24.dp, end = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(if (draft) MaterialTheme.colorScheme.surfaceVariant else Mml.Lagoon100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (draft) Icons.Outlined.BookmarkAdded else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (draft) MaterialTheme.colorScheme.primary else Mml.Lagoon600,
                    modifier = Modifier.size(46.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(if (draft) "Saved for later" else "Transfer sent", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                if (draft) "You can finish it any time from Activity → Saved."
                else "${money(t.transferAmount, t.localCurrency)} to ${t.receiver?.fullName ?: "your receiver"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (!draft) {
                Spacer(Modifier.height(10.dp))
                StatusChip(t.status, t.statusLabel)
            }
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // ── How to pay ───────────────────────────────────────
            t.paymentInstructions?.let { p ->
                SectionCard {
                    Text(
                        if (p.method == "cash") "Next: pay in cash at the office" else "Next: pay by internet banking",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (p.method == "cash") {
                        SummaryLine("Office", p.officeName ?: "—")
                        SummaryLine("Address", p.officeAddress ?: "—")
                        SummaryLine("Opening hours", p.openingHours ?: "—")
                        CopyRow("Your reference", p.reference)
                    } else {
                        SummaryLine("Bank", p.bankName ?: "—")
                        SummaryLine("Account name", p.accountName ?: "—")
                        CopyRow("Account number", p.accountNumber ?: "—")
                        CopyRow("Reference", p.reference)
                    }
                    SummaryLine("Amount to pay", money(p.amount, p.currency), strong = true)
                    Text(
                        if (p.method == "cash") "Quote your reference at the counter."
                        else "Use the reference exactly so we can match your payment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Collection code (sender can pass it on) ──────────
            t.payoutCode?.let { code ->
                Surface(color = Mml.Moana900, contentColor = Color.White, shape = MaterialTheme.shapes.large) {
                    Column {
                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Collection code for ${t.receiver?.givenName ?: "the receiver"}", color = Color(0xFFBFD6E0), style = MaterialTheme.typography.labelLarge)
                            Text(code.chunked(3).joinToString(" "), style = MoneyStyle.copy(fontSize = 40.sp, letterSpacing = 4.sp, color = Color.White))
                            Text(
                                "They show this and photo ID at ${t.receiverOffice?.name ?: "the MML office"}. It works once your payment is confirmed.",
                                color = Color(0xFFBFD6E0),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                        KupesiBand(height = 10.dp)
                    }
                }
            }

            // ── What happens next ────────────────────────────────
            if (!draft) SectionCard {
                Text("What happens next", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                NextStep(1, "You pay", if (t.paymentMethod == "cash") "Cash at your MML office." else "Internet banking with your reference.")
                NextStep(2, "MML confirms your payment", "The transfer becomes ready to collect.")
                NextStep(3, "${t.receiver?.givenName ?: "The receiver"} collects", "In person at ${t.receiverOffice?.name ?: "the MML office"}, with photo ID and the code.")
            }

            Spacer(Modifier.height(4.dp))
            if (draft) {
                Button(onClick = { onContinueDraft(t.id) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Continue now") }
            } else {
                Button(onClick = { onView(t.id) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("View transfer") }
            }
            OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Back to home") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NextStep(n: Int, title: String, body: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Mml.Kula600), contentAlignment = Alignment.Center) {
            Text("$n", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CopyRow(label: String, value: String) {
    val clipboard = LocalClipboardManager.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }) {
            Icon(Icons.Outlined.ContentCopy, "Copy $label", Modifier.size(18.dp))
        }
    }
}
