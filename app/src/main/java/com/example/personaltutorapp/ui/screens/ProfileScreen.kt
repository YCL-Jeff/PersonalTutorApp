package com.example.personaltutorapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // 添加滾動
import androidx.compose.foundation.verticalScroll // 添加滾動
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle // Placeholder icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter // <<< 導入 rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview // 導入 Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController // 為了預覽
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.personaltutorapp.ui.theme.PersonalTutorAppTheme // 為了預覽
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// --- 確保這個檔案裡只有 ProfileScreen 相關的程式碼 ---

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel(),
    modifier: Modifier = Modifier // 添加 Modifier 參數
) {
    val user by viewModel.currentUser.collectAsState(initial = null)
    // SnackbarHostState 需要提升到 NavigationGraph 層級的 Scaffold
    // val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope() // 保留用於可能的協程操作

    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var profilePictureUri by remember { mutableStateOf<Uri?>(null) }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.uid?.let { uid ->
            isLoading = true
            viewModel.getUserProfile(uid) { userProfile ->
                isLoading = false
                if (userProfile != null) {
                    displayName = userProfile["displayName"] as? String ?: user?.displayName ?: ""
                    bio = userProfile["bio"] as? String ?: ""
                    profilePictureUrl = userProfile["profilePictureUri"] as? String
                } else {
                    displayName = user?.displayName ?: ""
                    // scope.launch { snackbarHostState.showSnackbar("無法載入詳細資料...") } // Snackbar 需提升
                }
            }
        } ?: run {
            isLoading = false
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profilePictureUri = uri
        if (uri != null) {
            profilePictureUrl = null
        }
    }

    // 根 Composable (Column) 使用傳入的 modifier
    Column(
        modifier = modifier // 使用傳入的 modifier
            .fillMaxSize() // 保持填滿大小
            .padding(16.dp) // 保持內邊距
            .verticalScroll(rememberScrollState()), // 添加滾動
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp)) // 添加間距避免加載指示器觸碰頂部
        } else {
            // --- 頭像顯示 ---
            val imageModifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(enabled = !isSaving) { imagePickerLauncher.launch("image/*") }

            val imageToLoad: Any? = profilePictureUri ?: profilePictureUrl

            // *** 修改這裡：將 error 和 placeholder 移出 Builder，傳給 painter ***
            val placeholderPainter = rememberVectorPainter(image = Icons.Filled.AccountCircle) // 創建 Painter
            val errorPainter = rememberVectorPainter(image = Icons.Filled.AccountCircle) // 創建 Painter

            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageToLoad)
                    .crossfade(true)
                    // .error(...) // <<< 從 Builder 中移除
                    // .placeholder(...) // <<< 從 Builder 中移除
                    .build(),
                placeholder = placeholderPainter, // <<< 在這裡傳遞 Painter
                error = errorPainter // <<< 在這裡傳遞 Painter
            )

            Box(contentAlignment = Alignment.Center, modifier = imageModifier) {
                Image(
                    painter = painter,
                    contentDescription = "個人頭像",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // 如果需要在加載時顯示進度圈，可以覆蓋在 Image 上
                if (painter.state is AsyncImagePainter.State.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                }
                // 注意：如果 painter 處於 error 或 placeholder 狀態，它會自動顯示傳遞給它的 painter
            }


            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { imagePickerLauncher.launch("image/*") }, enabled = !isSaving) {
                Text("更換頭像")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 顯示名稱 ---
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("顯示名稱") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(8.dp))

            // --- 個人簡介 ---
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("個人簡介") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- 儲存按鈕 ---
            Button(
                onClick = {
                    user?.uid?.let { uid ->
                        isSaving = true
                        // *** 注意：這裡的 updateProfile 仍未處理圖片上傳 ***
                        viewModel.updateProfile(uid, displayName, bio, profilePictureUri) { success ->
                            isSaving = false
                            // Snackbar 需要在 Scaffold 中顯示
                            // scope.launch {
                            //     snackbarHostState.showSnackbar(if (success) "已儲存！" else "儲存失敗")
                            // }
                            if (success && profilePictureUri != null) {
                                // profilePictureUri = null // 應在 ViewModel 成功後處理
                            }
                        }
                    } ?: run {
                        // scope.launch { snackbarHostState.showSnackbar("錯誤：無法獲取用戶 ID") }
                    }
                },
                enabled = !isSaving && user != null
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("儲存中...")
                } else {
                    Text("儲存變更")
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // 將登出按鈕推到底部

            // --- 登出按鈕 ---
            Button(
                onClick = {
                    viewModel.signOut {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("登出")
            }
        }
    }
}

// --- 預覽區 ---
//@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    PersonalTutorAppTheme {
        val navController = rememberNavController()
        // 簡單預覽佔位符
        Text("Profile Screen Preview (Requires ViewModel)")
    }
}

// --- 確保這裡沒有 @Composable fun CourseListScreen(...) { ... } 的定義 ---

