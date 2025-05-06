package com.example.personaltutorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.personaltutorapp.ui.NavigationGraph
import com.example.personaltutorapp.ui.theme.PersonalTutorAppTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.Composable

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalTutorAppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NavigationGraph() // 直接調用 NavigationGraph
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