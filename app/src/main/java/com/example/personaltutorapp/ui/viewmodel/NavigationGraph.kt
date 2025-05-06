package com.example.personaltutorapp.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding // 導入 padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar // 導入 NavigationBar
import androidx.compose.material3.NavigationBarItem // 導入 NavigationBarItem
import androidx.compose.material3.Scaffold // 導入 Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf // 導入 mutableIntStateOf
import androidx.compose.runtime.remember // 導入 remember
import androidx.compose.runtime.setValue // 導入 setValue
import androidx.compose.ui.Modifier // 導入 Modifier
import androidx.compose.ui.graphics.vector.ImageVector // 導入 ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy // 導入 hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState // 導入 currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.personaltutorapp.ui.screens.CourseCreationScreen
import com.example.personaltutorapp.ui.screens.CourseListScreen
import com.example.personaltutorapp.ui.screens.DashboardScreen
import com.example.personaltutorapp.ui.screens.HomeScreen
import com.example.personaltutorapp.ui.screens.LessonCreationScreen
import com.example.personaltutorapp.ui.screens.LessonProgressScreen
// import com.example.personaltutorapp.ui.screens.LessonScreen
import com.example.personaltutorapp.ui.screens.LoginScreen
import com.example.personaltutorapp.ui.screens.ProfileScreen
import com.example.personaltutorapp.ui.screens.RegisterScreen
// import com.example.personaltutorapp.ui.screens.SearchScreen
// import com.example.personaltutorapp.ui.screens.NotificationsScreen
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

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
    LaunchedEffect(user, currentDestination) { // 監聽用戶和當前目標
        val currentRoute = currentDestination?.route
        user?.uid?.let { uid ->
            // --- 用戶已登入 ---
            // 異步檢查用戶角色 (如果不需要區分，可以移除 isTutor 檢查)
            authViewModel.isTutor(uid) { isTutor ->
                // *** 修改這裡：目標統一設為 Home ***
                val targetDestination = "Home"

                // 只有當我們不在目標頁面，並且目前在登入/註冊頁時才導航
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
            // 只有在需要顯示的頁面才創建底部導覽列
            if (shouldShowBottomBar) {
                HomeBottomNavigationBar(
                    items = bottomNavItems,
                    currentDestination = currentDestination,
                    navController = navController
                )
            }
        }
    ) { innerPadding -> // Scaffold 提供內邊距
        // NavHost 放在 Scaffold 的 content lambda 中
        NavHost(
            navController = navController,
            startDestination = "login",
            // 將 Scaffold 提供的內邊距應用到 NavHost 的 Modifier
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- 定義路由 ---
            // 確保 Screen Composable 接受 modifier 並應用它

            composable("login") {
                LoginScreen(navController = navController, viewModel = authViewModel)
            }
            composable(route = "register") {
                RegisterScreen(navController = navController, viewModel = authViewModel)
            }
            composable(route = "courseList") {
                CourseListScreen(navController = navController, viewModel = courseViewModel, isStudent = true /* modifier = Modifier */) // 假設已添加 modifier
            }
            composable(route = "Home") {
                HomeScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    courseViewModel = courseViewModel
                    /* modifier = Modifier */ // 假設已添加 modifier
                )
            }
            composable(route = "dashboard") {
                // Dashboard 仍然可訪問，但不是登入後目標
                DashboardScreen(navController = navController, viewModel = courseViewModel /* modifier = Modifier */)
            }
            composable(route = "courseCreation") {
                CourseCreationScreen(navController = navController, viewModel = courseViewModel /* modifier = Modifier */)
            }
            composable(route = "lessonCreation/{courseId}") { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull() ?: -1
                if (courseId != -1) {
                    LessonCreationScreen(navController = navController, viewModel = courseViewModel, courseId = courseId)
                } else {
                    Text("無效的課程 ID")
                }
            }
            composable(route = "lessonProgress/{courseId}") { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull() ?: -1
                if (courseId != -1) {
                    LessonProgressScreen(navController = navController, viewModel = courseViewModel, courseId = courseId)
                } else {
                    Text("無效的課程 ID")
                }
            }
            composable(route = "profile") {
                ProfileScreen(navController = navController, viewModel = authViewModel /* modifier = Modifier */) // 假設已添加 modifier
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

// 將底部導覽列 Composable 移到這裡
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
