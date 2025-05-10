package com.example.personaltutorapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.personaltutorapp.ui.theme.PersonalTutorAppTheme
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var profilePictureUri by remember { mutableStateOf<Uri?>(null) }
    var currentProfilePictureUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.uid?.let { uid ->
            viewModel.getUserProfile(uid) { userProfile ->
                isLoading = false
                if (userProfile != null) {
                    displayName = userProfile["displayName"] as? String ?: user?.displayName ?: ""
                    bio = userProfile["bio"] as? String ?: ""
                    currentProfilePictureUrl = userProfile["profilePictureUrl"] as? String
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Failed to load user profile") }
                }
            }
        } ?: run { isLoading = false }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.signOut {
                            navController.navigate("login") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        showLogoutDialog = false
                    }
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF3F4F6)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProfileImagePicker(
                            profilePictureUri = profilePictureUri,
                            currentProfilePictureUrl = currentProfilePictureUrl,
                            isSaving = isSaving,
                            onImagePicked = { profilePictureUri = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { if (it.length <= 50) displayName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isSaving,
                            shape = RoundedCornerShape(8.dp),
                            supportingText = { Text("${displayName.length}/50") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { if (it.length <= 200) bio = it },
                            label = { Text("Bio") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(8.dp),
                            supportingText = { Text("${bio.length}/200") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        user?.uid?.let { uid ->
                            isSaving = true
                            viewModel.updateProfile(uid, displayName, bio, profilePictureUri) { success, errorMessage ->
                                isSaving = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) "Profile saved successfully!" else errorMessage ?: "Failed to save profile"
                                    )
                                }
                                if (success && profilePictureUri != null) {
                                    profilePictureUri = null
                                    viewModel.getUserProfile(uid) { updatedProfile ->
                                        currentProfilePictureUrl = updatedProfile?.get("profilePictureUrl") as? String
                                    }
                                }
                            }
                        } ?: scope.launch { snackbarHostState.showSnackbar("Unable to retrieve user ID") }
                    },
                    enabled = !isSaving && user != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Save Changes")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { showLogoutDialog = true },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            }
        }
    }
}

@Composable
fun ProfileImagePicker(
    profilePictureUri: Uri?,
    currentProfilePictureUrl: String?,
    isSaving: Boolean,
    onImagePicked: (Uri?) -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onImagePicked(uri)
    }

    val imageModifier = Modifier
        .size(120.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        .clickable(enabled = !isSaving) { imagePickerLauncher.launch("image/*") }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(profilePictureUri ?: currentProfilePictureUrl)
            .crossfade(true)
            .build(),
        placeholder = rememberVectorPainter(Icons.Filled.AccountCircle),
        error = rememberVectorPainter(Icons.Filled.AccountCircle)
    )

    Box(contentAlignment = Alignment.Center, modifier = imageModifier) {
        Image(
            painter = painter,
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (painter.state is coil.compose.AsyncImagePainter.State.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 2.dp
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = { imagePickerLauncher.launch("image/*") }, enabled = !isSaving) {
        Text("Change Profile Picture")
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    PersonalTutorAppTheme {
        ProfileScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}