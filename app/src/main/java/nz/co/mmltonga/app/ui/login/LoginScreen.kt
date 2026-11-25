package nz.co.mmltonga.app.ui.login

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import nz.co.mmltonga.app.data.ApiError
import nz.co.mmltonga.app.ui.BaseViewModel
import nz.co.mmltonga.app.ui.components.BrandMark
import nz.co.mmltonga.app.ui.components.ErrorBanner
import nz.co.mmltonga.app.ui.components.KupesiBand
import nz.co.mmltonga.app.ui.theme.Mml

class LoginViewModel(app: Application) : BaseViewModel(app) {
    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    fun submit(onDone: () -> Unit) {
        if (login.isBlank() || password.isBlank()) { error = "Enter your phone number or email, and your password."; return }
        busy = true; error = null
        viewModelScope.launch(safe) {
            try { repo.signIn(login, password); onDone() } catch (e: ApiError) { error = e.message }
            busy = false
        }
    }
}

@Composable
fun LoginScreen(onSignedIn: () -> Unit, vm: LoginViewModel = viewModel()) {
    var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        // Header
        Column(Modifier.fillMaxWidth().background(Mml.Moana900).statusBarsPadding()) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                BrandMark(48.dp)
                Spacer(Modifier.height(36.dp))
                Text("Mālō e lelei", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text("Send money and shopping home, and collect what family sends you.", color = Color(0xFFA8C6D4), style = MaterialTheme.typography.bodyLarge)
            }
            KupesiBand(height = 18.dp)
        }
        Column(Modifier.padding(24.dp)) {
            ErrorBanner(vm.error)
            if (vm.error != null) Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = vm.login, onValueChange = { vm.login = it },
                label = { Text("Phone number or email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = vm.password, onValueChange = { vm.password = it },
                label = { Text("Password") }, singleLine = true,
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (show) "Hide password" else "Show password") } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { vm.submit(onSignedIn) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { vm.submit(onSignedIn) }, enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mml.Kula600, contentColor = Color.White),
            ) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("Sign in")
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "New to MML? Visit an MML office with your photo ID. We'll register you and set up your sign-in there.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
