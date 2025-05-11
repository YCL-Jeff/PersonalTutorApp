package com.example.personaltutorapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.personaltutorapp.ui.screens.CourseCreationScreen
import com.example.personaltutorapp.ui.screens.CourseListScreen
import com.example.personaltutorapp.ui.screens.DashboardScreen
import com.example.personaltutorapp.ui.screens.EnrollmentManagementScreen
import com.example.personaltutorapp.ui.screens.HomeScreen
import com.example.personaltutorapp.ui.screens.LessonCreationScreen
import com.example.personaltutorapp.ui.screens.LessonProgressScreen
import com.example.personaltutorapp.ui.screens.LessonScreen
import com.example.personaltutorapp.ui.screens.LoginScreen
import com.example.personaltutorapp.ui.screens.ProfileScreen
import com.example.personaltutorapp.ui.screens.RegisterScreen
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem("首頁", Icons.Filled.Home, "Home"),
        BottomNavItem("課程", Icons.Filled.List, "courseList"),
        BottomNavItem("個人", Icons.Filled.Person, "profile")
    )

    val shouldShowBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    LaunchedEffect(user) {
        val currentRoute = currentDestination?.route
        user?.uid?.let { uid ->
            authViewModel.isTutor(uid) { isTutor ->
                val targetDestination = "Home"
                if (currentRoute != targetDestination && (currentRoute == "login" || currentRoute == "register" || currentRoute == "dashboard")) {
                    navController.navigate(targetDestination) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        } ?: run {
            if (currentRoute != "login" && currentRoute != "register") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

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
                val localAuthViewModel: AuthViewModel = hiltViewModel()
                val currentUser by localAuthViewModel.currentUser.collectAsState()
                var isStudent by remember { mutableStateOf(true) }
                LaunchedEffect(currentUser) {
                    currentUser?.uid?.let { uid ->
                        localAuthViewModel.isTutor(uid) { isTutor ->
                            isStudent = !isTutor
                        }
                    }
                }
                CourseListScreen(
                    navController = navController,
                    viewModel = courseViewModel,
                    isStudent = isStudent
                )
            }
            composable("Home") {
                val localAuthViewModel: AuthViewModel = hiltViewModel()
                val currentUser by localAuthViewModel.currentUser.collectAsState()
                var isTutorState by remember { mutableStateOf(false) }
                var isLoadingTutorStatus by remember { mutableStateOf(true) }

                LaunchedEffect(currentUser) {
                    isLoadingTutorStatus = true
                    currentUser?.uid?.let { uid ->
                        localAuthViewModel.isTutor(uid) { tutorStatus ->
                            isTutorState = tutorStatus
                            isLoadingTutorStatus = false
                        }
                    } ?: run {
                        isLoadingTutorStatus = false
                    }
                }

                if (isLoadingTutorStatus) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (isTutorState) {
                        DashboardScreen(
                            navController = navController,
                            viewModel = courseViewModel
                        )
                    } else {
                        HomeScreen(
                            navController = navController,
                            authViewModel = localAuthViewModel,
                            courseViewModel = courseViewModel
                        )
                    }
                }
            }
            composable("courseCreation") {
                CourseCreationScreen(navController = navController, viewModel = courseViewModel, authViewModel = authViewModel)
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
            composable(
                "lessonScreen/{courseId}",
                arguments = listOf(navArgument("courseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                LessonScreen(
                    navController = navController,
                    viewModel = courseViewModel,
                    authViewModel = hiltViewModel(),
                    courseId = courseId
                )
            }
            composable("profile") {
                ProfileScreen(navController = navController, viewModel = authViewModel)
            }
            // Enrollment Management Screen Route
            composable(
                route = "enrollmentManagement/{courseId}/{courseTitle}", // Added courseTitle
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType },
                    navArgument("courseTitle") { type = NavType.StringType } // Argument for courseTitle
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId")
                // Decode the courseTitle as it might contain spaces or special characters
                val encodedCourseTitle = backStackEntry.arguments?.getString("courseTitle")
                val courseTitle = encodedCourseTitle?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }


                if (courseId != null && courseTitle != null) {
                    EnrollmentManagementScreen(
                        navController = navController,
                        courseId = courseId,
                        courseTitle = courseTitle, // Pass decoded courseTitle
                        viewModel = courseViewModel
                    )
                } else {
                    Text("錯誤：找不到課程 ID 或標題")
                    // Optionally, navigate back or show a more user-friendly error
                    // LaunchedEffect(Unit) { navController.popBackStack() }
                }
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
