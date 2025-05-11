package com.example.personaltutorapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState


// 修正導入路徑，指向 ui.screens 包
import com.example.personaltutorapp.ui.screens.LessonCreationScreen
import com.example.personaltutorapp.ui.screens.CourseCreationScreen
import com.example.personaltutorapp.ui.screens.CourseListScreen
import com.example.personaltutorapp.ui.screens.DashboardScreen
import com.example.personaltutorapp.ui.screens.LessonProgressScreen
import com.example.personaltutorapp.ui.screens.LessonScreen
import com.example.personaltutorapp.ui.screens.LoginScreen
import com.example.personaltutorapp.ui.screens.ProfileScreen
import com.example.personaltutorapp.ui.screens.RegisterScreen

@Composable
fun NavigationGraph() {
    val navController: NavHostController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val courseViewModel: CourseViewModel = hiltViewModel()

    // 監聽當前用戶狀態，使用 collectAsState
    val user by authViewModel.currentUser.collectAsState(initial = null) // 確保 currentUser 是 StateFlow

    // 動態設置起始頁面
    LaunchedEffect(user) {
        user?.let { firebaseUser ->  // 確保 user 不是 null
            authViewModel.isTutor(firebaseUser.uid) { isTutor ->
                if (isTutor) {
                    navController.navigate("dashboard") {
                        popUpTo("register") { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate("courseList") {
                        popUpTo("register") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController, authViewModel)
        }
        composable(route = "register") {
            RegisterScreen(navController, authViewModel)
        }
        composable(route = "courseList") {
            CourseListScreen(courseViewModel, navController, isStudent = true) // 確保順序：CourseViewModel, NavController
        }
        composable(route = "dashboard") {
            DashboardScreen(navController, courseViewModel)
        }
        composable(route = "courseCreation") {
            CourseCreationScreen(courseViewModel, navController) // 確保順序：CourseViewModel, NavController
        }
        composable(route = "lessonCreation/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull() ?: 1
            LessonCreationScreen(navController = navController, viewModel = courseViewModel, courseId = courseId)
        }

        composable(route = "lessonCreation/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull() ?: 1
            LessonCreationScreen(navController = navController, viewModel = courseViewModel, courseId = courseId)
        }


        composable(route = "lessonProgress/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull() ?: 1
            LessonProgressScreen(navController = navController, viewModel = courseViewModel, courseId = courseId)
        }

        composable(route = "profile") {
            ProfileScreen(authViewModel)
        }

    }
}