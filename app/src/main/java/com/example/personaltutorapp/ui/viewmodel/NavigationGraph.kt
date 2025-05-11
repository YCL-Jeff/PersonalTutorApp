package com.example.personaltutorapp.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.personaltutorapp.ui.screens.CourseCreationScreen
import com.example.personaltutorapp.ui.screens.CourseListScreen
import com.example.personaltutorapp.ui.screens.DashboardScreen
import com.example.personaltutorapp.ui.screens.HomeScreen
import com.example.personaltutorapp.ui.screens.LessonCreationScreen // 添加導入
import com.example.personaltutorapp.ui.screens.LessonProgressScreen
import com.example.personaltutorapp.ui.screens.LoginScreen
import com.example.personaltutorapp.ui.screens.ProfileScreen
import com.example.personaltutorapp.ui.screens.RegisterScreen
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import androidx.navigation.navArgument
import androidx.navigation.NavType

// 將底部導覽列項目定義移到這裡
data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun NavigationGraph() {
    val navController: NavHostController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val courseViewModel: CourseViewModel = hiltViewModel()
    val user by authViewModel.currentUser.collectAsState(initial = null)

    // 獲取當前的後退堆疊條目狀態，用於判斷路由和更新 UI
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 定義底部導覽列項目
    val bottomNavItems = listOf(
        BottomNavItem("首頁", Icons.Filled.Home, "Home"),
        BottomNavItem("課程", Icons.Filled.List, "courseList"),
        BottomNavItem("個人", Icons.Filled.Person, "profile")
    )

    // 判斷是否需要顯示底部導覽列
    val shouldShowBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    // 使用 LaunchedEffect 處理登入/登出後的自動導航
    LaunchedEffect(user, currentDestination) {
        val currentRoute = currentDestination?.route
        user?.uid?.let { uid ->
            // --- 用戶已登入 ---
            authViewModel.isTutor(uid) { isTutor ->
                val targetDestination = "Home"
                if (currentRoute != targetDestination && (currentRoute == "login" || currentRoute == "register")) {
                    navController.navigate(targetDestination) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        } ?: run {
            // --- 用戶未登入 ---
            if (currentRoute != "login" && currentRoute != "register") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // 使用 Scaffold 包裹 NavHost
    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                HomeBottomNavigationBar(
                    items = bottomNavItems,
                    currentDestination = currentDestination,
                    navController = navController
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(navController = navController, viewModel = authViewModel)
            }
            composable("register") {
                RegisterScreen(navController = navController, viewModel = authViewModel)
            }
            composable("courseList") {
                CourseListScreen(navController = navController, viewModel = courseViewModel, isStudent = true)
            }
            composable("Home") {
                HomeScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    courseViewModel = courseViewModel
                )
            }
            composable("dashboard") {
                DashboardScreen(navController = navController, viewModel = courseViewModel)
            }
            composable("courseCreation") {
                CourseCreationScreen(navController = navController, viewModel = courseViewModel)
            }
            composable(
                "lessonCreation/{courseId}",
                arguments = listOf(navArgument("courseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                LessonCreationScreen(
                    navController = navController,
                    viewModel = courseViewModel,
                    courseId = courseId
                )
            }
            composable(
                "lessonProgress/{courseId}",
                arguments = listOf(navArgument("courseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                LessonProgressScreen(
                    navController = navController,
                    viewModel = courseViewModel,
                    courseId = courseId
                )
            }
            composable("profile") {
                ProfileScreen(navController = navController, viewModel = authViewModel)
            }
            composable("search") {
                Text("搜尋畫面 (待實現)")
            }
            composable("notifications") {
                Text("通知畫面 (待實現)")
            }
        }
    }
}

@Composable
fun HomeBottomNavigationBar(
    items: List<BottomNavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    navController: NavHostController
) {
    NavigationBar {
        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}