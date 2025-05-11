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
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isTutor by remember { mutableStateOf(false) }

    val registerState by viewModel.registerState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (registerState.isLoading) {
            CircularProgressIndicator()
        } else if (registerState.error != null) {
            Text(text = registerState.error ?: "", color = Color.Red)
        }

        // Name Input
        TextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") })
        Spacer(modifier = Modifier.height(8.dp))

        // Email Input
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(8.dp))

        // Password Input
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Switch for Tutor Role
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Register as Tutor")
            Switch(checked = isTutor, onCheckedChange = { isTutor = it })
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Register Button
        Button(onClick = {
            viewModel.register(email, password, name, isTutor) { success ->
                if (success) navController.navigate("login")
            }
        }) {
            Text(text = "Register")
        }

        // Back to Login Button
        TextButton(onClick = { navController.navigate("login") }) {
            Text(text = "Back to Login")
        }
    }
}
