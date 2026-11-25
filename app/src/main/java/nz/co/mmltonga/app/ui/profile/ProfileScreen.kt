@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package nz.co.mmltonga.app.ui.profile

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import nz.co.mmltonga.app.data.ApiError
import nz.co.mmltonga.app.ui.BaseViewModel
import nz.co.mmltonga.app.ui.components.*
import nz.co.mmltonga.app.ui.theme.Mml

class ProfileViewModel(app: Application) : BaseViewModel(app) {
    val profile = repo.session.profile

    init {
        viewModelScope.launch(safe) { try { repo.session.updateProfile(repo.call { me() }) } catch (_: ApiError) {} }
    }

    fun logout(done: () -> Unit) = viewModelScope.launch(safe) { repo.signOut(); done() }
}

@Composable
fun ProfileScreen(onSignedOut: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val me by vm.profile.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(Mml.Moana900).statusBarsPadding()) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(me?.fullName, me?.photoUrl, 92.dp, container = Color.White.copy(alpha = .14f))
                Spacer(Modifier.height(12.dp))
                Text(me?.fullName.orEmpty(), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(me?.customerCode.orEmpty(), color = Color(0xFFA8C6D4))
                if (me?.isVerified == true) AssistChip(onClick = {}, label = { Text("ID verified") }, leadingIcon = { Icon(Icons.Outlined.Verified, null) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color.White, leadingIconContentColor = Color.White), modifier = Modifier.padding(top = 8.dp))
            }
            KupesiBand(height = 14.dp)
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                SummaryLine("Phone", me?.phone ?: "—")
                SummaryLine("Email", me?.email ?: "—")
                SummaryLine("Date of birth", me?.dateOfBirth ?: "—")
                SummaryLine("Address", listOfNotNull(me?.residentialAddress, me?.city, me?.country).joinToString(", "))
                SummaryLine("Home office", me?.homeOffice ?: "—")
            }
            Text(
                "To change your details or add a new receiver, visit your MML office with photo ID, or use the customer portal on the web.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sign out")
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Sign out?") },
        confirmButton = { TextButton(onClick = { confirm = false; vm.logout(onSignedOut) }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Stay") } },
    )
}
