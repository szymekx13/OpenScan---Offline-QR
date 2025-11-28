package com.openscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openscan.app.ui.theme.OpenScanOfflineQRTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

val Roboto = FontFamily(
    Font(R.font.roboto, FontWeight.Normal),
    Font(R.font.roboto_bold, FontWeight.Bold)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var darkMode by remember { mutableStateOf(true) }

    OpenScanOfflineQRTheme(darkTheme = darkMode) {
        NavHost(navController = navController, startDestination = "main") {

            composable("main") {
                MainScreen(
                    onScanClicked = { navController.navigate("scan") },
                    onSettingsClicked = { navController.navigate("settings") }
                )
            }

            composable("scan") {
                QRScanScreen(onResult = {
                    navController.popBackStack()
                })
            }

            composable("settings") {
                SettingsScreen(
                    onClose = { navController.popBackStack() },
                    darkMode = darkMode,
                    onDarkModeChange = { darkMode = it }
                )
            }
        }
    }
}


@Composable
fun MainScreen(onScanClicked: () -> Unit, onSettingsClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar with title and settings icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSettingsClicked) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "OpenScan",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                fontFamily = Roboto,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Spacer to perfectly center the title
            Spacer(Modifier.size(48.dp))
        }

        // Centered content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Press the button below to start scanning the QR code.",
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                fontSize = 18.sp
            )
        }

        // Extended FAB
        ExtendedFloatingActionButton(
            onClick = onScanClicked,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            text = { Text("Skanuj QR") },
            icon = {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Skanuj kod QR"
                )
            }
        )
    }
}

@Composable
fun SettingsScreen(onClose: () -> Unit, darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    var autoOpenLinks by remember { mutableStateOf(false) }
    var vibrateOnScan by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        IconButton(
            onClick = { onClose() },
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Settings",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.size(40.dp)) // Spacer to push content down
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingItem("Automatically open links", autoOpenLinks) { enabled ->
                autoOpenLinks = enabled
            }

            SettingItem("Dark mode", darkMode) { enabled ->
                onDarkModeChange(enabled)
            }

            SettingItem("Vibrate on scan", vibrateOnScan) { enabled ->
                vibrateOnScan = enabled
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://github.com/szymon-tomaszewski/OpenScan-Offline-QR") }
                    .padding(vertical = 16.dp)
            ) {
                Text("Source code", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            }
        }
        Text(
            "Ver: v1.0.0",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
fun SettingItem(name: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(name, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle
        )
    }
}
