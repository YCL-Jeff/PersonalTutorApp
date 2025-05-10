package com.example.personaltutorapp.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearningPlatformApp()
        }
    }
}

@Composable
fun HomeScreen(courseViewModel: CourseViewModel, navController: NavController, isStudent: Boolean) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
        TopBar()
        DateSelector()
        TodayCourses()
        CourseProgress()
        Spacer(modifier = Modifier.weight(1f))
        BottomNavigationBar(navController)
    }
}

@Composable
fun LearningPlatformApp() {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
        TopBar()
        DateSelector()
        TodayCourses()
        CourseProgress()
        Spacer(modifier = Modifier.weight(1f))
        BottomNavigationBar()
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFF1E88E5),
            titleContentColor = Color.White
        ),
        title = { Text("Learning Platform") },
        actions = {
            IconButton(onClick = {}) { Icon(Icons.Filled.Notifications, contentDescription = "Notifications") }
            IconButton(onClick = {}) { Icon(Icons.Filled.Search, contentDescription = "Search") }
        }
    )
}




@Composable
fun DateSelector() {
    var selectedDay by remember { mutableStateOf("5") }
    val days = listOf("4" to "Mon", "5" to "Tue", "6" to "Wed", "7" to "Thu", "8" to "Fri")

    Column(modifier = Modifier.padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = {}) { Icon(Icons.Filled.Search, contentDescription = "Previous") }
            Text(text = "March $selectedDay, 2024", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = {}) { Icon(Icons.Filled.Search, contentDescription = "Next") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { (day, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedDay = day }
                        .padding(8.dp)
                        .background(if (day == selectedDay) Color.Blue else Color.Transparent, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(text = label, color = if (day == selectedDay) Color.White else Color.Gray, fontSize = 12.sp)
                    Text(text = day, fontWeight = FontWeight.Bold, color = if (day == selectedDay) Color.White else Color.Black)
                }
            }
        }
    }
}


@Composable
fun TodayCourses() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Today's Courses", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        CourseCard("Python Programming", "10:00 AM - 11:30 AM", "In Progress")
        CourseCard("Web Development", "2:00 PM - 3:30 PM", "Not Started")
    }
}

@Composable
fun CourseCard(title: String, time: String, status: String) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📚", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(time, fontSize = 12.sp, color = Color.Gray)
            }
            Text(status, color = if (status == "In Progress") Color.Green else Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun CourseProgress() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("My Course Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        ProgressCard("Python Programming", 60)
        ProgressCard("Web Development Basics", 30)
    }
}

@Composable
fun ProgressCard(title: String, progress: Int) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("$progress% Completed", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController? = null) {
    NavigationBar {
        NavigationBarItem(
            selected = true, 
            onClick = {},
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") }, 
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false, 
            onClick = { navController?.navigate("courseList") },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Courses") }, 
            label = { Text("Courses") }
        )
        NavigationBarItem(
            selected = false, 
            onClick = { navController?.navigate("profile") },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") }, 
            label = { Text("Profile") }
        )
    }
}