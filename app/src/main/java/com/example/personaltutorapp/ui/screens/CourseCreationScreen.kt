package com.example.personaltutorapp.ui.screens

import android.util.Log // 新增 Log import
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import androidx.compose.foundation.background
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCreationScreen(
    viewModel: CourseViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    navController: NavController
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val subjects = listOf("Programming", "Math", "Science", "English", "History")

    val firebaseCurrentUser by authViewModel.currentUser.collectAsState()
    // 用於存儲從 Firestore users 集合獲取的用戶自定義 ID
    var userCustomId by remember { mutableStateOf<String?>(null) }
    // 用於追蹤自定義 ID 是否正在加載
    var isLoadingCustomId by remember { mutableStateOf(false) }

    var creationResult by remember { mutableStateOf<Boolean?>(null) }

    // 當 firebaseCurrentUser (Firebase Auth 用戶狀態) 改變時，
    // 或 userCustomId 尚未加載時，嘗試獲取用戶的自定義 ID
    LaunchedEffect(firebaseCurrentUser) {
        firebaseCurrentUser?.uid?.let { uid ->
            if (userCustomId == null) { // 僅在尚未獲取到 custom ID 時加載
                isLoadingCustomId = true
                authViewModel.getUserProfile(uid) { userProfile ->
                    isLoadingCustomId = false
                    val fetchedCustomId = userProfile?.get("id") as? String
                    if (fetchedCustomId != null) {
                        userCustomId = fetchedCustomId
                        Log.d("CourseCreationScreen", "Fetched custom user ID: $userCustomId")
                    } else {
                        errorMessage = "Failed to retrieve user profile ID."
                        Log.e("CourseCreationScreen", "Custom user ID is null or not found for UID: $uid. Profile: $userProfile")
                    }
                }
            }
        } ?: run {
            // 如果沒有登入用戶，清空自定義 ID 並停止加載
            userCustomId = null
            isLoadingCustomId = false
        }
    }

    LaunchedEffect(creationResult) {
        creationResult?.let { success ->
            if (success) {
                snackbarHostState.showSnackbar("Course created successfully")
                navController.popBackStack()
            } else {
                // 保持之前的錯誤訊息，或由 createCourse 的回調更新
                if (errorMessage == null) { // 避免覆蓋 LaunchedEffect 中設定的 Profile ID 錯誤
                    errorMessage = "Failed to create course"
                }
            }
            creationResult = null // 重置結果，以便下次觸發
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create New Course") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(Color(0xFFF3F4F6))
        ) {
            Text(
                text = "Create New Course",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Course title input" },
                isError = title.isBlank() && errorMessage != null, // 考慮 isError 條件
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Course description input" },
                isError = description.isBlank() && errorMessage != null, // 考慮 isError 條件
                maxLines = 4,
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = subject,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Subject") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .semantics { contentDescription = "Subject dropdown" },
                    isError = subject.isBlank() && errorMessage != null, // 考慮 isError 條件
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    subjects.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                subject = option
                                expanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = {
                    errorMessage = null // 清除之前的錯誤訊息
                    if (title.isBlank() || description.isBlank() || subject.isBlank()) {
                        errorMessage = "All fields are required"
                        return@Button
                    }

                    val currentTutorCustomId = userCustomId // 使用從 Firestore users 獲取的自定義 ID
                    if (isLoadingCustomId) {
                        errorMessage = "Fetching user information, please wait..."
                        return@Button
                    }
                    if (currentTutorCustomId == null || currentTutorCustomId.isEmpty()) {
                        errorMessage = "Tutor ID not found. Cannot create course. Please ensure your profile is complete or try logging in again."
                        Log.e("CourseCreationScreen", "Attempted to create course but userCustomId is null or empty.")
                        return@Button
                    }

                    isCreating = true
                    // courseId 生成邏輯：若 title 為 "1"，則 courseId 為 "1"
                    // 若 title 為 "English Course"，則 courseId 為 "english_course"
                    // 這與您範例中的 courseId:1, title:1 的情況相符
                    val courseIdToCreate = title.lowercase().replace(" ", "_")

                    // 根據您的 Firebase 數據，科目似乎是直接存儲選擇時的大小寫 (例如 "English", "Math")
                    // 因此，這裡不對 subject 進行 .lowercase() 轉換
                    viewModel.createCourse(
                        courseIdToCreate,
                        title,
                        description,
                        subject, // 直接使用選擇的 subject
                        currentTutorCustomId // 使用用戶的自定義 ID
                    ) { success ->
                        isCreating = false
                        // creationResult 的 LaunchedEffect 會處理 snackbar 和導航
                        // 這裡只設定結果，讓 LaunchedEffect 反應
                        if (!success && errorMessage == null) { // 如果 viewModel 回調失敗且沒有特定錯誤
                            errorMessage = "Failed to create course (from callback)"
                        }
                        creationResult = success
                    }
                },
                // 按鈕在獲取到 customId 前或正在創建時不可用
                enabled = !isCreating && !isLoadingCustomId,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = "Create course button" },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                if (isCreating || isLoadingCustomId) { // 也在加載自定義 ID 時顯示進度
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Create Course",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}