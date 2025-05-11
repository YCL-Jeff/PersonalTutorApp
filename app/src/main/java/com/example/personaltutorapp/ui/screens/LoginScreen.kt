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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.theme.PersonalTutorAppTheme
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    val loginState by viewModel.loginState.collectAsState(initial = AuthViewModel.LoginState())
    val user by viewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var idOrEmail by remember { mutableStateOf("") } // 改為 idOrEmail
    var password by remember { mutableStateOf("") }
    var idOrEmailError by remember { mutableStateOf<String?>(null) } // 改為 idOrEmailError
    var passwordError by remember { mutableStateOf<String?>(null) }

    // 監聽用戶狀態，根據角色導航
    LaunchedEffect(user) {
        user?.let { currentUser ->
            viewModel.isTutor(currentUser.uid) { isTutor ->
                val destination = if (isTutor) "dashboard" else "home"
                if (navController.currentDestination?.route != destination) {
                    navController.navigate(destination) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    // 顯示錯誤訊息
    LaunchedEffect(loginState.error) {
        loginState.error?.let {
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
                text = "Personal Tutor App",
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
                    OutlinedTextField(
                        value = idOrEmail, // 改為 idOrEmail
                        onValueChange = {
                            idOrEmail = it
                            idOrEmailError = if (it.isNotEmpty() && !it.contains("@") && it.length > 50) "ID must be 50 characters or less" else null
                        },
                        label = { Text("ID or Email") }, // 改為 ID or Email
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = idOrEmailError != null,
                        supportingText = { if (idOrEmailError != null) Text(idOrEmailError!!, color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !loginState.isLoading
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
                        enabled = !loginState.isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (loginState.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (idOrEmailError == null && passwordError == null) {
                        viewModel.login(idOrEmail, password) { success ->
                            if (!success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Login failed")
                                }
                            }
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Please fix input errors") }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !loginState.isLoading && idOrEmail.isNotEmpty() && password.isNotEmpty(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Log In")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { navController.navigate("register") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Don't have an account? Register")
            }
        }
    }
}