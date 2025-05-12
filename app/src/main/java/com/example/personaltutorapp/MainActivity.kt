package com.example.personaltutorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.example.personaltutorapp.ui.NavigationGraph
import com.example.personaltutorapp.ui.theme.PersonalTutorAppTheme
import com.google.firebase.FirebaseApp
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
            Log.d("Firebase", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("Firebase", "Firebase initialization failed: ${e.message}")
        }
        setContent {
            PersonalTutorAppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NavigationGraph()
                }
            }
        }
    }
}

@Composable
fun PersonalTutorAppTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface {
            content()
        }
    }
}