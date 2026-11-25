package nz.co.mmltonga.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nz.co.mmltonga.app.data.Repository
import nz.co.mmltonga.app.ui.activity.ActivityScreen
import nz.co.mmltonga.app.ui.activity.TransactionScreen
import nz.co.mmltonga.app.ui.chat.ChatScreen
import nz.co.mmltonga.app.ui.home.HomeScreen
import nz.co.mmltonga.app.ui.login.LoginScreen
import nz.co.mmltonga.app.ui.profile.ProfileScreen
import nz.co.mmltonga.app.ui.receive.ReceiveScreen
import nz.co.mmltonga.app.ui.send.SendScreen
import nz.co.mmltonga.app.ui.send.SentScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val CHAT = "chat"
    const val PROFILE = "profile"
    const val SEND = "send?draft={draft}&receiver={receiver}"
    const val RECEIVE = "receive/{type}"
    const val TX = "tx/{id}"
    const val SENT = "sent/{id}"

    fun send(draftId: Long? = null, receiverId: Long? = null) = "send?draft=${draftId ?: -1}&receiver=${receiverId ?: -1}"
    fun receive(type: String) = "receive/$type"
    fun tx(id: Long) = "tx/$id"
    fun sent(id: Long) = "sent/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)
private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home),
    Tab(Routes.ACTIVITY, "Activity", Icons.Outlined.History),
    Tab(Routes.CHAT, "Chat", Icons.AutoMirrored.Outlined.Chat),
    Tab(Routes.PROFILE, "Profile", Icons.Outlined.Person),
)

@Composable
fun MmlNavHost(repo: Repository) {
    val nav = rememberNavController()
    val start = remember { if (repo.session.isSignedIn) Routes.HOME else Routes.LOGIN }

    // If the token expires anywhere in the app, go back to sign in.
    LaunchedEffect(Unit) {
        repo.signedOut.collect {
            nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val showTabs = tabs.any { it.route == current }

    // Badge on the Chat tab when MML staff have written. Opening the chat marks them read.
    val profile by repo.session.profile.collectAsState()
    val unread = profile?.unreadMessages ?: 0
    LaunchedEffect(current) {
        if (current == Routes.CHAT) repo.session.profile.value?.let {
            if (it.unreadMessages > 0) repo.session.updateProfile(it.copy(unreadMessages = 0))
        }
    }

    Scaffold(
        bottomBar = {
            if (showTabs) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { nav.goTab(tab.route) },
                        icon = {
                            if (tab.route == Routes.CHAT && unread > 0) {
                                BadgedBox(badge = { Badge { Text(if (unread > 9) "9+" else unread.toString()) } }) { Icon(tab.icon, null) }
                            } else {
                                Icon(tab.icon, null)
                            }
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { pad ->
        NavHost(nav, startDestination = start, modifier = Modifier.padding(pad)) {
            composable(Routes.LOGIN) {
                LoginScreen(onSignedIn = { nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onSend = { nav.navigate(Routes.send()) },
                    onReceive = { type -> nav.navigate(Routes.receive(type)) },
                    onOpenTx = { t -> nav.navigate(if (t.isDraft) Routes.send(draftId = t.id) else Routes.tx(t.id)) },
                    onSeeAll = { nav.goTab(Routes.ACTIVITY) },
                    onChat = { nav.goTab(Routes.CHAT) },
                )
            }
            composable(Routes.ACTIVITY) {
                ActivityScreen(onOpenTx = { t -> nav.navigate(if (t.isDraft) Routes.send(draftId = t.id) else Routes.tx(t.id)) })
            }
            composable(Routes.CHAT) { ChatScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(onSignedOut = { nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } })
            }
            composable(
                Routes.SEND,
                arguments = listOf(
                    navArgument("draft") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("receiver") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { e ->
                val draft = e.arguments?.getLong("draft")?.takeIf { it > 0 }
                val receiver = e.arguments?.getLong("receiver")?.takeIf { it > 0 }
                SendScreen(
                    draftId = draft,
                    receiverId = receiver,
                    onClose = { nav.popBackStack() },
                    // after "Send" or "Save for later" show the confirmation screen
                    onFinished = { id -> nav.navigate(Routes.sent(id)) { popUpTo(Routes.HOME) } },
                )
            }
            composable(Routes.SENT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                val id = e.arguments?.getLong("id") ?: 0L
                SentScreen(
                    id = id,
                    onHome = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                    onView = { nav.navigate(Routes.tx(it)) { popUpTo(Routes.HOME) } },
                    onContinueDraft = { nav.navigate(Routes.send(draftId = it)) { popUpTo(Routes.HOME) } },
                )
            }
            composable(Routes.RECEIVE, arguments = listOf(navArgument("type") { type = NavType.StringType })) { e ->
                ReceiveScreen(
                    type = e.arguments?.getString("type") ?: "money",
                    onBack = { nav.popBackStack() },
                    onOpenTx = { nav.navigate(Routes.tx(it)) },
                    onChat = { nav.goTab(Routes.CHAT) },
                )
            }
            composable(Routes.TX, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                TransactionScreen(
                    id = e.arguments?.getLong("id") ?: 0,
                    onBack = { nav.popBackStack() },
                    onContinueDraft = { nav.navigate(Routes.send(draftId = it)) },
                    onSendAgain = { receiverId -> nav.navigate(Routes.send(receiverId = receiverId)) },
                    onChat = { nav.goTab(Routes.CHAT) },
                )
            }
        }
    }
}

private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
