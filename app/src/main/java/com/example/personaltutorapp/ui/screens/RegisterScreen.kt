package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.theme.PersonalTutorAppTheme
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") } // 新增 displayName
    var isTutor by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var idError by remember { mutableStateOf<String?>(null) }
    var displayNameError by remember { mutableStateOf<String?>(null) } // 新增 displayNameError

    val registerState by viewModel.registerState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(registerState.error) {
        registerState.error?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(Color(0xFFF3F4F6))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Register",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (registerState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    OutlinedTextField(
                        value = id,
                        onValueChange = {
                            id = it
                            idError = if (it.length > 50) "ID must be 50 characters or less" else null
                        },
                        label = { Text("ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = idError != null,
                        supportingText = { if (idError != null) Text(idError!!, color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !registerState.isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = displayName, // 新增 displayName 輸入欄位
                        onValueChange = {
                            displayName = it
                            displayNameError = if (it.length > 50) "Display Name must be 50 characters or less" else null
                        },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = displayNameError != null,
                        supportingText = { if (displayNameError != null) Text(displayNameError!!, color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !registerState.isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = if (it.isNotEmpty() && !it.contains("@")) "Please enter a valid email" else null
                        },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = emailError != null,
                        supportingText = { if (emailError != null) Text(emailError!!, color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !registerState.isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = if (it.length < 6) "Password must be at least 6 characters" else null
                        },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = passwordError != null,
                        supportingText = { if (passwordError != null) Text(passwordError!!, color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !registerState.isLoading
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register as Tutor", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = isTutor,
                            onCheckedChange = { isTutor = it },
                            enabled = !registerState.isLoading,
                            modifier = Modifier.semantics { contentDescription = "Register as Tutor Switch" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (idError == null && displayNameError == null && emailError == null && passwordError == null) {
                        viewModel.register(email, password, id, displayName, isTutor) { success -> // 添加 displayName
                            if (success) {
                                scope.launch { snackbarHostState.showSnackbar("Registration successful!") }
                                navController.navigate("login")
                            }
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Please fix input errors") }
                    }
                },
                enabled = !registerState.isLoading && id.isNotEmpty() && displayName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Register")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { navController.navigate("login") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !registerState.isLoading
            ) {
                Text("Back to Login")
            }
        }
    }
}