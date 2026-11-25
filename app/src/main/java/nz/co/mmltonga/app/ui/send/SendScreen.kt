@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package nz.co.mmltonga.app.ui.send

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import nz.co.mmltonga.app.data.model.Person
import nz.co.mmltonga.app.data.model.StockItem
import nz.co.mmltonga.app.ui.components.*
import nz.co.mmltonga.app.ui.theme.DisplayFamily
import nz.co.mmltonga.app.ui.theme.Mml
import nz.co.mmltonga.app.ui.theme.MoneyStyle

/**
 * Send money, following the business's steps:
 *   1. Fill the amount (NZD or TOP)        2. Currency details with fees (same screen, live)
 *   3. Money or shopping?                  4. Money → receiver; shopping → shopping list, then receiver
 *   5. Receiver (choose someone already registered, confirm their details)
 *   6. Shopping (choose items)             7. Send / process — or save for later
 */
@Composable
fun SendScreen(
    draftId: Long?,
    receiverId: Long?,
    onClose: () -> Unit,
    onFinished: (Long) -> Unit,
    vm: SendViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.start(draftId, receiverId) }
    BackHandler { if (!vm.back()) onClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                MmlTopBar(
                    title = if (vm.draftId != null) "Finish your transfer" else "Send money",
                    onBack = { if (!vm.back()) onClose() },
                )
                if (!vm.loading && vm.loadError == null) StepHeader(vm)
            }
        },
        bottomBar = {
            if (!vm.loading && vm.loadError == null) BottomBar(
                vm,
                onSend = { vm.submit("send") { onFinished(it.id) } },
                onSave = { vm.submit("draft") { onFinished(it.id) } },
            )
        },
    ) { pad ->
        when {
            vm.loading -> LoadingBox(Modifier.padding(pad))
            vm.loadError != null -> Box(Modifier.padding(pad)) { ErrorState(vm.loadError ?: "") { onClose() } }
            else -> AnimatedContent(
                targetState = vm.step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "send-step",
                modifier = Modifier.padding(pad),
            ) { s ->
                Column(Modifier.fillMaxSize()) {
                    vm.banner?.let { ErrorBanner(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    when (s) {
                        Step.AMOUNT -> AmountStep(vm)
                        Step.DELIVERY -> DeliveryStep(vm)
                        Step.SHOPPING -> ShoppingStep(vm)
                        Step.RECEIVER -> ReceiverStep(vm)
                        Step.REVIEW -> ReviewStep(vm)
                    }
                }
            }
        }
    }
}

// ── progress header ──────────────────────────────────────────────

@Composable
private fun StepHeader(vm: SendViewModel) {
    val f = vm.flow
    val i = f.indexOf(vm.step)
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            f.forEachIndexed { idx, s ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (idx <= i) Mml.Kula600 else MaterialTheme.colorScheme.outline)
                        .clickable(enabled = idx < i) { vm.goTo(s) },
                )
            }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Step ${i + 1} of ${f.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(vm.step.title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── bottom bar: Continue  /  Save for later + Send ───────────────

@Composable
private fun BottomBar(vm: SendViewModel, onSend: () -> Unit, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 10.dp) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            vm.quote?.let { q ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("You pay", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(money(q.totalPayable, q.localCurrency), fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (vm.type == "goods") "Shopping + cash" else "Receiver gets",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(money(q.convertedAmount, q.foreignCurrency), fontWeight = FontWeight.Bold, color = Mml.Lagoon600)
                    }
                }
            }
            if (vm.step == Step.REVIEW) {
                // Step 7: send / process, or save for later
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onSave,
                        enabled = !vm.submitting,
                        modifier = Modifier.weight(1f).height(54.dp),
                    ) {
                        Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save for later")
                    }
                    Button(
                        onClick = onSend,
                        enabled = !vm.submitting,
                        modifier = Modifier.weight(1.3f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mml.Kula600, contentColor = Color.White),
                    ) {
                        if (vm.submitting) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send")
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (vm.quote != null) {
                        TextButton(onClick = onSave, enabled = !vm.submitting) { Text("Save for later") }
                    }
                    Button(onClick = vm::next, modifier = Modifier.weight(1f).height(54.dp)) {
                        Text("Continue")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepTitle(text: String, hint: String? = null) {
    Column(Modifier.padding(bottom = 4.dp)) {
        Text(text, style = MaterialTheme.typography.headlineSmall)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── 1 & 2. Amount + currency details with fees ───────────────────

@Composable
private fun AmountStep(vm: SendViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepTitle("How much would you like to send?", "Type the amount in ${vm.local} or in ${vm.foreign}, whichever you know.")

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = vm.amountMode == "send",
                onClick = { vm.setMode("send") },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("${vm.local} (I pay)") }
            SegmentedButton(
                selected = vm.amountMode == "receive",
                onClick = { vm.setMode("receive") },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("${vm.foreign} (they get)") }
        }

        AmountField(vm)

        if (vm.locals.size > 1 || vm.foreigns.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CurrencyMenu("From", vm.local, vm.locals) { vm.setCurrencies(it) }
                Icon(Icons.Outlined.East, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                CurrencyMenu("To", vm.foreign, vm.foreigns) { vm.setCurrencies(vm.local, it) }
            }
        }

        Text("Currency details & fees", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
        QuoteCard(vm)
        vm.quoteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun AmountField(vm: SendViewModel) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(1.5.dp, cs.primary, MaterialTheme.shapes.large)
            .background(cs.surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (vm.amountMode == "send") "You send" else "They receive",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
            BasicTextField(
                value = vm.amountText,
                onValueChange = vm::setAmount,
                singleLine = true,
                textStyle = MoneyStyle.copy(fontSize = 36.sp, color = cs.onSurface),
                cursorBrush = SolidColor(Mml.Kula600),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                decorationBox = { inner ->
                    if (vm.amountText.isEmpty()) {
                        Text("0.00", style = MoneyStyle.copy(fontSize = 36.sp, color = cs.onSurfaceVariant.copy(alpha = .4f)))
                    }
                    inner()
                },
            )
        }
        Text(
            vm.inputCurrency,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(cs.surfaceVariant).padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun CurrencyMenu(label: String, value: String, options: List<String>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text("$label $value") },
            trailingIcon = { Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp)) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onPick(option) })
            }
        }
    }
}

@Composable
private fun QuoteCard(vm: SendViewModel) {
    val q = vm.quote
    Surface(color = Mml.Moana900, contentColor = Color.White, shape = MaterialTheme.shapes.large) {
        Column {
            Column(Modifier.padding(18.dp)) {
                if (q == null) {
                    Text(
                        if (vm.quoting) "Checking today's rate…" else "Enter an amount to see the exchange rate and fee.",
                        color = Color(0xFFBFD6E0),
                    )
                } else {
                    QuoteRow("Amount sent", money(q.transferAmount, q.localCurrency))
                    QuoteRow("Exchange rate", "1 ${q.localCurrency} = ${rateText(q.rate)} ${q.foreignCurrency}")
                    QuoteRow("Service fee (${q.feeDescription})", money(q.serviceFee, q.localCurrency))
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = .15f))
                    QuoteRow("Total to pay", money(q.totalPayable, q.localCurrency), big = true)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .08f)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Receiver gets", color = Color(0xFFBFD6E0), modifier = Modifier.weight(1f))
                        Text(money(q.convertedAmount, q.foreignCurrency), style = MoneyStyle.copy(fontSize = 24.sp, color = Color.White))
                    }
                }
            }
            KupesiBand(height = 10.dp)
        }
    }
}

@Composable
private fun QuoteRow(label: String, value: String, big: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFBFD6E0), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold, fontFamily = if (big) DisplayFamily else null, fontSize = if (big) 20.sp else 14.sp)
    }
}

// ── 3. Money or shopping ─────────────────────────────────────────

@Composable
private fun DeliveryStep(vm: SendViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle("Do you want the receiver to get money or shopping?")
        ChoiceTile(
            selected = vm.type == "money",
            icon = Icons.Outlined.Payments,
            title = "Money",
            subtitle = "They collect ${vm.quote?.let { money(it.convertedAmount, it.foreignCurrency) } ?: "cash"} at an MML office",
            onClick = { vm.chooseType("money") },
        )
        if (vm.storesForCurrency.isNotEmpty()) {
            ChoiceTile(
                selected = vm.type == "goods",
                icon = Icons.Outlined.ShoppingBasket,
                title = "Shopping",
                subtitle = "You choose goods from an MML store. Anything left over is paid to them in cash.",
                onClick = { vm.chooseType("goods") },
            )
        }
        InfoNote(Icons.Outlined.Info, "Money and shopping are always collected in person at an MML office, with photo ID.")
    }
}

// ── 6. Shopping details ──────────────────────────────────────────

@Composable
private fun ShoppingStep(vm: SendViewModel) {
    val store = vm.store
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StepTitle("Choose the shopping")
            if (vm.storesForCurrency.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.storesForCurrency.forEach { s ->
                        FilterChip(
                            selected = vm.storeId == s.id,
                            onClick = { vm.chooseStore(s.id) },
                            label = { Text(s.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Outlined.Storefront, null, Modifier.size(18.dp)) },
                        )
                    }
                }
            } else {
                vm.storesForCurrency.firstOrNull()?.let { InfoNote(Icons.Outlined.Storefront, "Collected at ${it.name}") }
            }
            BudgetMeter(vm)
            val cats = store?.categories.orEmpty()
            if (cats.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = vm.category == null, onClick = { vm.category = null }, label = { Text("All") })
                    cats.forEach { c ->
                        FilterChip(selected = vm.category == c, onClick = { vm.category = c }, label = { Text(c) })
                    }
                }
            }
        }
        when {
            vm.loadingItems -> LoadingBox()
            store == null -> EmptyState(Icons.Outlined.Storefront, "Choose a store")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(store.items.filter { vm.category == null || it.category == vm.category }, key = { it.id }) { item ->
                    ProductCard(item, vm.qty(item.id), onAdd = { vm.add(item) }, onRemove = { vm.remove(item) })
                }
            }
        }
    }
}

@Composable
private fun BudgetMeter(vm: SendViewModel) {
    val over = vm.overBudget
    val pct = if (vm.budget > 0) (vm.cartTotal / vm.budget).toFloat().coerceIn(0f, 1f) else 0f
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${vm.cartCount} items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${money(vm.cartTotal, vm.foreign)} of ${money(vm.budget)}", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = vm::matchAmountToCart, enabled = vm.cartCount > 0) { Text("Match amount") }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (over) Mml.Kula600 else Mml.Lagoon600,
                trackColor = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (over) "Over by ${money(vm.cartTotal - vm.budget, vm.foreign)}. Remove items or tap Match amount."
                else "Cash paid with the shopping: ${money(vm.cashWithShopping, vm.foreign)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProductCard(item: StockItem, qty: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = cs.surface,
        border = BorderStroke(if (qty > 0) 2.dp else 1.dp, if (qty > 0) cs.primary else cs.outlineVariant),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(cs.surfaceVariant), contentAlignment = Alignment.Center) {
                if (item.imageUrl != null) {
                    AsyncImage(model = item.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Outlined.Inventory2, null, tint = cs.primary.copy(alpha = .6f), modifier = Modifier.size(36.dp))
                }
                if (qty > 0) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).clip(CircleShape).background(Mml.Kula600),
                        contentAlignment = Alignment.Center,
                    ) { Text("$qty", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Text("${money(item.price, item.currency)} / ${item.unit}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (item.inStock > 0) "${item.inStock} in stock" else "Out of stock",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (qty == 0) {
                    FilledTonalButton(
                        onClick = onAdd,
                        enabled = item.inStock > 0,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Text("Add")
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(cs.primaryContainer),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onRemove) { Icon(Icons.Outlined.Remove, "Remove one") }
                        Text("$qty", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        IconButton(onClick = onAdd, enabled = qty < item.inStock) { Icon(Icons.Outlined.Add, "Add one") }
                    }
                }
            }
        }
    }
}

// ── 5. Receiver details ──────────────────────────────────────────

@Composable
private fun ReceiverStep(vm: SendViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepTitle("Who is the receiver?", "Choose someone already registered with MML.")

        // Selected receiver — full personal details to confirm
        vm.receiver?.let { r -> ReceiverDetailsCard(r) }

        if (vm.receivers.isNotEmpty()) {
            Text("My receivers", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            vm.receivers.forEach { p -> ReceiverOption(p, selected = vm.receiverId == p.id) { vm.chooseReceiver(p) } }
        }

        Text("Find someone else", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
        OutlinedTextField(
            value = vm.query,
            onValueChange = vm::search,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            label = { Text("Customer code, phone number or full name") },
            trailingIcon = { if (vm.searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) },
        )
        vm.results.forEach { p -> ReceiverOption(p, selected = vm.receiverId == p.id, badge = "Choose") { vm.chooseReceiver(p) } }
        if (vm.query.length >= 3 && !vm.searching && vm.results.isEmpty()) {
            Text(
                "No one found with those details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        InfoNote(
            Icons.Outlined.PersonAdd,
            "Sending to someone new? Register them with their photo on the MML web portal, or at any MML office. They'll then appear here.",
        )

        // Where they collect
        if (vm.type != "goods") {
            Text("Collect at", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            vm.collectOffices.forEach { o ->
                Surface(
                    onClick = { vm.collectOfficeId = o.id },
                    shape = MaterialTheme.shapes.medium,
                    color = if (vm.collectOfficeId == o.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (vm.collectOfficeId == o.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = vm.collectOfficeId == o.id, onClick = { vm.collectOfficeId = o.id })
                        Column(Modifier.weight(1f)) {
                            Text(o.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(o.address, o.openingHours).joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            vm.collectOffices.firstOrNull { it.id == vm.collectOfficeId }?.let {
                InfoNote(Icons.Outlined.Storefront, "The shopping is collected at ${it.name}.")
            }
        }
    }
}

@Composable
private fun ReceiverDetailsCard(r: Person) {
    Surface(color = Mml.Moana900, contentColor = Color.White, shape = MaterialTheme.shapes.large) {
        Column {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(r.fullName, r.photoUrl, 52.dp, container = Color.White.copy(alpha = .14f))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Receiver", style = MaterialTheme.typography.labelMedium, color = Color(0xFFBFD6E0))
                        Text(r.fullName, style = MaterialTheme.typography.titleLarge)
                    }
                    if (r.isVerified) Icon(Icons.Outlined.Verified, "ID verified", tint = Color(0xFF8FE3C6))
                }
                Spacer(Modifier.height(12.dp))
                DetailRow("Customer code", r.customerCode)
                r.relationship?.let { DetailRow("Relationship", it) }
                r.dateOfBirth?.let { DetailRow("Date of birth", prettyDob(it)) }
                r.gender?.let { DetailRow("Gender", it.replaceFirstChar { c -> c.uppercase() }) }
                r.phone?.let { DetailRow("Phone", it) }
                DetailRow("Address", listOfNotNull(r.residentialAddress, r.city, r.country).joinToString(", ").ifBlank { "—" })
                Text(
                    "Check these match their photo ID. They'll need it to collect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBFD6E0),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            KupesiBand(height = 10.dp)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color(0xFFBFD6E0), modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

private fun prettyDob(iso: String): String =
    runCatching {
        java.time.LocalDate.parse(iso.take(10)).format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH))
    }.getOrDefault(iso)

@Composable
private fun ReceiverOption(p: Person, selected: Boolean, badge: String? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) cs.primaryContainer.copy(alpha = .5f) else cs.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant),
    ) {
        PersonRow(
            name = p.fullName,
            subtitle = listOfNotNull(p.relationship, p.city ?: p.country, p.customerCode).joinToString(", "),
            photoUrl = p.photoUrl,
            modifier = Modifier.padding(12.dp),
        ) {
            when {
                selected -> Icon(Icons.Outlined.CheckCircle, "Selected", tint = cs.primary)
                badge != null -> Text(badge, color = cs.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Review & pay ─────────────────────────────────────────────────

@Composable
private fun ReviewStep(vm: SendViewModel) {
    val b = vm.boot ?: return
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepTitle("Check, then send or save for later")

        SectionCard {
            vm.receiver?.let { r -> PersonRow(r.fullName, listOfNotNull(r.relationship, r.customerCode).joinToString(", "), r.photoUrl) }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            vm.quote?.let { q ->
                SummaryLine("You send", money(q.transferAmount, q.localCurrency))
                SummaryLine("Exchange rate", "1 ${q.localCurrency} = ${rateText(q.rate)} ${q.foreignCurrency}")
                SummaryLine("Service fee", money(q.serviceFee, q.localCurrency))
                SummaryLine("Total to pay", money(q.totalPayable, q.localCurrency), strong = true)
            }
            if (vm.type == "goods") {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                vm.cartLines.forEach { (i, q) -> SummaryLine("$q × ${i.name}", money(i.price * q)) }
                SummaryLine("Cash with the shopping", money(vm.cashWithShopping, vm.foreign))
            } else {
                vm.quote?.let { SummaryLine("They collect", money(it.convertedAmount, it.foreignCurrency), strong = true) }
            }
            SummaryLine("Collect at", vm.collectOffices.firstOrNull { it.id == vm.collectOfficeId }?.name ?: "—")
        }

        Dropdown("Purpose of transfer", vm.purpose, b.purposes, error = vm.fieldErrors["purpose"]) { vm.purpose = it }
        Dropdown("Source of the money", vm.incomeSource, b.incomeSources, error = vm.fieldErrors["sender_income_source"]) { vm.incomeSource = it }

        Text("How will you pay?", style = MaterialTheme.typography.titleMedium)
        ChoiceTile(
            selected = vm.paymentMethod == "internet_banking",
            icon = Icons.Outlined.AccountBalance,
            title = "Internet banking",
            subtitle = "Pay into the MML bank account. We'll show the account and your reference.",
            onClick = { vm.paymentMethod = "internet_banking" },
        )
        ChoiceTile(
            selected = vm.paymentMethod == "cash",
            icon = Icons.Outlined.AccountBalanceWallet,
            title = "Cash at the office",
            subtitle = "Pay in cash at your MML office. Quote your reference number.",
            onClick = { vm.paymentMethod = "cash" },
        )

        OutlinedTextField(
            value = vm.notes,
            onValueChange = { if (it.length <= 500) vm.notes = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note for the office (optional)") },
            minLines = 2,
        )
        InfoNote(
            Icons.Outlined.Info,
            "Once your payment is confirmed, ${vm.receiver?.givenName ?: "the receiver"} collects in person at the MML office with photo ID and a 6-digit code.",
        )
    }
}

@Composable
private fun Dropdown(label: String, value: String?, options: List<String>, error: String? = null, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = value ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            isError = error != null,
            supportingText = if (error != null) { { Text(error) } } else null,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onPick(option); open = false })
            }
        }
    }
}

@Composable
private fun InfoNote(icon: ImageVector, text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
