@file:OptIn(ExperimentalMaterial3Api::class)

package nz.co.mmltonga.app.ui.activity

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import nz.co.mmltonga.app.data.ApiError
import nz.co.mmltonga.app.data.model.Transaction
import nz.co.mmltonga.app.ui.BaseViewModel
import nz.co.mmltonga.app.ui.components.EmptyState
import nz.co.mmltonga.app.ui.components.ErrorBanner
import nz.co.mmltonga.app.ui.home.TxRow

class ActivityViewModel(app: Application) : BaseViewModel(app) {
    val tabs = listOf("sent" to "Sent", "incoming" to "Receiving", "drafts" to "Saved")
    var tab by mutableIntStateOf(0)
    var items by mutableStateOf<List<Transaction>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    fun load() {
        loading = true
        viewModelScope.launch(safe) {
            try {
                items = when (tabs[tab].first) {
                    "incoming" -> repo.call { incoming(null, "all") }
                    else -> repo.call { transactions(tabs[tab].first) }
                }
                error = null
            } catch (e: ApiError) { error = e.message }
            loading = false
        }
    }
}

@Composable
fun ActivityScreen(onOpenTx: (Transaction) -> Unit, vm: ActivityViewModel = viewModel()) {
    LaunchedEffect(vm.tab) { vm.load() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Activity", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
        PrimaryTabRow(selectedTabIndex = vm.tab, containerColor = MaterialTheme.colorScheme.background) {
            vm.tabs.forEachIndexed { i, (_, label) -> Tab(selected = vm.tab == i, onClick = { vm.tab = i }, text = { Text(label) }) }
        }
        PullToRefreshBox(isRefreshing = vm.loading, onRefresh = vm::load, modifier = Modifier.weight(1f)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                vm.error?.let { item { ErrorBanner(it) } }
                if (!vm.loading && vm.items.isEmpty()) item {
                    EmptyState(Icons.Outlined.Inbox, when (vm.tabs[vm.tab].first) {
                        "drafts" -> "Nothing saved for later"
                        "incoming" -> "Nothing sent to you yet"
                        else -> "You haven't sent anything yet"
                    })
                }
                items(vm.items, key = { it.id }) { t -> TxRow(t, onClick = { onOpenTx(t) }) }
            }
        }
    }
}
