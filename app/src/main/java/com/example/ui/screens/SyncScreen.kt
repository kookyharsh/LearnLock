package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppPreferencesManager
import com.example.service.DriveSyncManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.example.BuildConfig
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color

@Composable
fun SyncScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { AppPreferencesManager(context) }
    val syncManager = remember { DriveSyncManager(context, prefsManager) }

    var isDriveSyncEnabled by remember { mutableStateOf(prefsManager.isDriveSyncEnabled()) }
    var lastSyncTime by remember { mutableLongStateOf(prefsManager.getLastSyncTime()) }
    var isSyncing by remember { mutableStateOf(false) }
    var userEmail by remember { mutableStateOf(prefsManager.getGoogleUserEmail()) }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    val credentialManager = remember { CredentialManager.create(context) }

    fun handleGoogleSignIn() {
        val clientId = BuildConfig.GOOGLE_CLIENT_ID
        if (clientId.isBlank()) {
            Toast.makeText(context, "Please configure GOOGLE_CLIENT_ID in Secrets.", Toast.LENGTH_LONG).show()
            return
        }

        coroutineScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is GoogleIdTokenCredential) {
                    // Assuming GoogleIdTokenCredential parsing succeeds
                    val idToken = credential.idToken
                    // Parse email from JWT or set a dummy one for now if actual payload parsing isn't present
                    userEmail = credential.id
                    prefsManager.setGoogleUserEmail(userEmail ?: "Signed In")
                    Toast.makeText(context, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                } else if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        userEmail = googleIdTokenCredential.id
                        prefsManager.setGoogleUserEmail(userEmail ?: "Signed In")
                        Toast.makeText(context, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("Auth", "Failed to parse Google ID Token", e)
                        Toast.makeText(context, "Failed to parse credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: GetCredentialException) {
                Log.e("Auth", "Sign-in failed", e)
                Toast.makeText(context, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = ElegantPrimary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Drive & Database Sync",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Cloud Sync Status Card
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Google Drive Auto-Sync",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (lastSyncTime > 0) "Last synced: ${sdf.format(Date(lastSyncTime))}" else "Not synced yet",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isDriveSyncEnabled,
                        onCheckedChange = { checked ->
                            isDriveSyncEnabled = checked
                            prefsManager.setDriveSyncEnabled(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElegantOnPrimary,
                            checkedTrackColor = ElegantPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSyncing = true
                            val result = syncManager.performCloudSync()
                            if (result) {
                                lastSyncTime = prefsManager.getLastSyncTime()
                                Toast.makeText(context, "Google Drive Sync Completed!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Sync encountered an issue.", Toast.LENGTH_SHORT).show()
                            }
                            isSyncing = false
                        }
                    },
                    enabled = !isSyncing,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantPrimary,
                        contentColor = ElegantOnPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = ElegantOnPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing to Google Drive...")
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SYNC TO GOOGLE DRIVE NOW", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Google Login Section
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = CodeBlue
                    )
                    Text(
                        text = "Google Account Connection",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in to securely backup your generated CS concepts and question history directly to Google Drive.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (userEmail.isNullOrBlank()) {
                    Button(
                        onClick = { handleGoogleSignIn() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign in with Google", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Signed in as", color = TextMuted, fontSize = 12.sp)
                                Text(userEmail ?: "", color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    userEmail = null
                                    prefsManager.setGoogleUserEmail("")
                                },
                                shape = CircleShape
                            ) {
                                Text("Sign Out", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
