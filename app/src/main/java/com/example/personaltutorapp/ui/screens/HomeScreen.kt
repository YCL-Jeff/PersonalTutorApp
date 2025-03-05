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
import androidx.compose.material.icons.filled.List
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
fun HomeScreen() {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
        TopBar()
        DateSelector()
        TodayCourses()
        CourseProgress()
        Spacer(modifier = Modifier.weight(1f))
        BottomNavigationBar()
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
        title = { Text("學習平台") },
        actions = {
            IconButton(onClick = {}) { Icon(Icons.Filled.Notifications, contentDescription = "通知") }
            IconButton(onClick = {}) { Icon(Icons.Filled.Search, contentDescription = "搜尋") }
        }
    )
}




@Composable
fun DateSelector() {
    var selectedDay by remember { mutableStateOf("5") } // 確保記住狀態
    val days = listOf("4" to "週一", "5" to "週二", "6" to "週三", "7" to "週四", "8" to "週五")

    Column(modifier = Modifier.padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = {}) { Icon(Icons.Filled.Search, contentDescription = "左") }
            Text(text = "2024年3月$selectedDay 日", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = {}) { Icon(Icons.Filled.Search, contentDescription = "右") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { (day, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedDay = day } // 更新選中的日期
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
        Text("今日課程", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        CourseCard("Python程式設計", "10:00 AM - 11:30 AM", "進行中")
        CourseCard("網頁設計實戰", "2:00 PM - 3:30 PM", "未開始")
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
            Text(status, color = if (status == "進行中") Color.Green else Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun CourseProgress() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("我的課程進度", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        ProgressCard("Python程式設計", 60)
        ProgressCard("網頁設計入門", 30)
    }
}

@Composable
fun ProgressCard(title: String, progress: Int) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth())
            Text("$progress% 完成", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun BottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Filled.Home, contentDescription = "首頁") }, label = { Text("首頁") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Filled.List, contentDescription = "課程") }, label = { Text("課程") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Filled.Person, contentDescription = "個人") }, label = { Text("個人") })
    }
}