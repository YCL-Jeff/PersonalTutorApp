package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.google.firebase.auth.FirebaseUser // 確保 FirebaseUser 導入
import kotlinx.coroutines.flow.StateFlow // 確保 StateFlow 導入

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 監聽 loginState 的變化
    val loginState by viewModel.loginState.collectAsState(initial = AuthViewModel.LoginState())

    // 監聽當前用戶狀態
    val currentUser by viewModel.currentUser.collectAsState(null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loginState.isLoading) {
            CircularProgressIndicator()
        } else if (loginState.error != null) {
            Text(text = loginState.error ?: "", color = Color.Red)
        }

        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            viewModel.login(email, password) { success ->
                if (success) {
                    val uid = currentUser?.uid ?: ""
                    viewModel.isTutor(uid) { isTutor ->
                        if (isTutor) {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate("courseList") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }) {
            Text("Login")
        }

        TextButton(onClick = { navController.navigate("register") }) {
            Text("Register")
        }
    }
}
