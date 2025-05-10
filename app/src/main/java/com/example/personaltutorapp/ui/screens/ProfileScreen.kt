package com.example.personaltutorapp.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.navigation.NavController

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    val user by viewModel.currentUser.collectAsState(initial = null)

    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    var bio by remember { mutableStateOf("") }
    var profilePictureUri by remember { mutableStateOf<Uri?>(null) }

    // ✅ 正確調用 getUserProfile
    LaunchedEffect(user) {
        user?.let { firebaseUser ->
            viewModel.getUserProfile(firebaseUser.uid) { userProfile ->
                if (userProfile != null) {
                    bio = userProfile["bio"] as? String ?: ""
                    profilePictureUri = userProfile["profilePictureUri"]?.let { Uri.parse(it as String) }
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        profilePictureUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Profile", style = MaterialTheme.typography.headlineLarge)

        TextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") })
        TextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") })

        Button(onClick = { launcher.launch("image/*") }) {
            Text(text = "Update Profile Picture")
        }

        Button(onClick = {
            user?.let { firebaseUser ->
                viewModel.updateProfile(firebaseUser.uid, displayName, bio, profilePictureUri) { success ->
                    if (success) {
                        // ✅ 更新成功處理
                    }
                }
            }
        }) {
            Text(text = "Save Profile")
        }
        
        // Add Logout Button
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(
            onClick = {
                viewModel.signOut { success ->
                    if (success) {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Logout",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Logout")
        }
    }
}
