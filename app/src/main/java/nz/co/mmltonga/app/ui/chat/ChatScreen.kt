@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package nz.co.mmltonga.app.ui.chat

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nz.co.mmltonga.app.data.ApiError
import nz.co.mmltonga.app.data.model.ChatMessage
import nz.co.mmltonga.app.data.model.ChatSend
import nz.co.mmltonga.app.ui.BaseViewModel
import nz.co.mmltonga.app.ui.components.BrandMark
import nz.co.mmltonga.app.ui.components.EmptyState
import nz.co.mmltonga.app.ui.theme.Mml

class ChatViewModel(app: Application) : BaseViewModel(app) {
    val messages = mutableStateListOf<ChatMessage>()
    var draft by mutableStateOf("")
    var sending by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var pollMs = 5000L; private set
    private var lastId = 0L

    suspend fun poll() {
        try {
            val page = repo.call { chat(lastId) }
            pollMs = page.pollMs.coerceAtLeast(2000)
            page.messages.filter { m -> messages.none { it.id == m.id } }.let { messages.addAll(it) }
            messages.lastOrNull()?.let { lastId = it.id }
            error = null
        } catch (e: ApiError) { error = "Reconnecting…" }
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        viewModelScope.launch(safe) {
            try {
                val m = repo.call { sendChat(ChatSend(text)) }
                draft = ""
                if (messages.none { it.id == m.id }) messages.add(m)
                lastId = maxOf(lastId, m.id)
            } catch (e: ApiError) { error = e.message }
            sending = false
        }
    }
}

/** Chat with MML staff. Polls while the screen is visible. */
@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { vm.poll(); delay(vm.pollMs) }
        }
    }
    val list = rememberLazyListState()
    LaunchedEffect(vm.messages.size) { if (vm.messages.isNotEmpty()) list.animateScrollToItem(vm.messages.lastIndex) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().imePadding()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark(38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("MML customer support", style = MaterialTheme.typography.titleMedium)
                Text(vm.error ?: "We reply during office hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider()
        LazyColumn(state = list, modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vm.messages.isEmpty()) item { EmptyState(Icons.Outlined.Forum, "Ask us anything", "Rates, opening hours, or a transfer you've sent. Mālō!") }
            items(vm.messages, key = { it.id }) { m -> Bubble(m) }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = vm.draft, onValueChange = { if (it.length <= 2000) vm.draft = it },
                    placeholder = { Text("Write a message") }, maxLines = 5, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = vm::send, enabled = vm.draft.isNotBlank() && !vm.sending, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Send, "Send")
                }
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage) {
    val mine = m.senderType == "customer"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (mine) Mml.Moana800 else MaterialTheme.colorScheme.surface,
            contentColor = if (mine) Color.White else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(18.dp, 18.dp, if (mine) 4.dp else 18.dp, if (mine) 18.dp else 4.dp),
            border = if (mine) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(m.body)
                Text(
                    listOfNotNull(if (mine) null else m.senderName, m.time).joinToString(", "),
                    style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(alpha = .65f),
                )
            }
        }
    }
}
