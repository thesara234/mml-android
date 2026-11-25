@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package nz.co.mmltonga.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import nz.co.mmltonga.app.ui.theme.LocalDark
import nz.co.mmltonga.app.ui.theme.Mml
import nz.co.mmltonga.app.ui.theme.statusTone
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── formatting ────────────────────────────────────────────────────

private val numberFmt = NumberFormat.getNumberInstance(Locale("en", "NZ")).apply {
    minimumFractionDigits = 2; maximumFractionDigits = 2
}

/** "NZD 1,250.00" — currency code first so NZD and pa'anga are never confused. */
fun money(amount: Double, currency: String? = null): String =
    if (currency.isNullOrBlank()) numberFmt.format(amount) else "$currency ${numberFmt.format(amount)}"

fun rateText(rate: Double): String = rate.toBigDecimal().stripTrailingZeros().toPlainString()

fun prettyDate(iso: String?, pattern: String = "d MMM yyyy, h:mm a"): String =
    iso?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)) }.getOrNull() } ?: ""

fun initials(name: String?): String =
    name.orEmpty().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.trimStart('\'', 'ʻ', '‘').take(1).uppercase() }

// ── kupesi band: the triangle border printed on Tongan ngatu ─────

@Composable
fun KupesiBand(
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    red: Color = Mml.Kula600,
    other: Color = Color.White.copy(alpha = .14f),
) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        val h = size.height
        val w = h * 2 // each pair of triangles is 2h wide
        var x = 0f
        while (x < size.width) {
            drawPath(Path().apply { moveTo(x, h); lineTo(x + w / 4, 0f); lineTo(x + w / 2, h); close() }, red)
            drawPath(Path().apply { moveTo(x + w / 2, 0f); lineTo(x + w, 0f); lineTo(x + w * 3 / 4, h); close() }, other)
            x += w
        }
    }
}

// ── building blocks ──────────────────────────────────────────────

@Composable
fun StatusChip(status: String, label: String, modifier: Modifier = Modifier) {
    val t = statusTone(status, LocalDark.current)
    Row(
        modifier.clip(CircleShape).background(t.bg).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(t.fg))
        Spacer(Modifier.width(6.dp))
        Text(label, color = t.fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun Avatar(name: String?, photoUrl: String? = null, size: Dp = 42.dp, container: Color = MaterialTheme.colorScheme.primaryContainer) {
    Box(
        Modifier.size(size).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(model = photoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(initials(name), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = (size.value * .38f).sp)
        }
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, padding: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Column(Modifier.padding(padding), content = content) }
}

@Composable
fun SummaryLine(label: String, value: String, strong: Boolean = false, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Text(value, fontWeight = if (strong) FontWeight.Bold else FontWeight.SemiBold, textAlign = TextAlign.End, color = valueColor, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
    }
}

@Composable
fun PersonRow(name: String, subtitle: String?, photoUrl: String? = null, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Avatar(name, photoUrl)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

@Composable
fun BrandMark(size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * .3f)).background(Mml.Kula600), contentAlignment = Alignment.Center) {
        Text("M", color = Color.White, fontFamily = nz.co.mmltonga.app.ui.theme.DisplayFamily, fontWeight = FontWeight.Bold, fontSize = (size.value * .5f).sp)
        KupesiBand(Modifier.align(Alignment.BottomCenter), height = size * .16f)
    }
}

/** Selectable tile used for money/shopping and payment choices. */
@Composable
fun ChoiceTile(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outline, MaterialTheme.shapes.medium)
            .background(if (selected) cs.primaryContainer.copy(alpha = .5f) else cs.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) cs.primary else cs.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (selected) cs.onPrimary else cs.primary) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String? = null, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f), modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        if (action != null) { Spacer(Modifier.height(14.dp)); action() }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) =
    EmptyState(Icons.Outlined.CloudOff, "Couldn't load", message) { OutlinedButton(onClick = onRetry) { Text("Try again") } }

@Composable
fun ErrorBanner(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmlTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
