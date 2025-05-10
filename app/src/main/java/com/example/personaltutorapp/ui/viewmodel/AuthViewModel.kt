package com.example.personaltutorapp.ui.viewmodel // 確保包名正確

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage // <<< 導入 FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // <<< 導入 await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage // <<< 注入 FirebaseStorage
) : ViewModel() {

    private val _registerState = MutableStateFlow(RegisterState())
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _loginState = MutableStateFlow(LoginState(isLoading = false, error = null))
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    data class LoginState(
        val isLoading: Boolean = false,
        val error: String? = null
    )

    data class RegisterState(
        val isLoading: Boolean = false,
        val error: String? = null
    )

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loginState.value = _loginState.value.copy(isLoading = true, error = null) // 重置錯誤訊息
            try {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _loginState.value = _loginState.value.copy(isLoading = false, error = null)
                        onResult(true)
                    } else {
                        _loginState.value = _loginState.value.copy(isLoading = false, error = task.exception?.message ?: "登入失敗，請檢查您的帳號密碼。")
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                _loginState.value = _loginState.value.copy(isLoading = false, error = e.message ?: "發生未知錯誤。")
                onResult(false)
            }
        }
    }

    fun register(email: String, password: String, name: String, isTutor: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _registerState.value = RegisterState(isLoading = true, error = null) // 重置錯誤訊息
            try {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: run {
                                _registerState.value = RegisterState(isLoading = false, error = "無法獲取用戶ID。")
                                onResult(false)
                                return@addOnCompleteListener
                            }
                            // *** 修改這裡：使用 "displayName" 作為鍵名 ***
                            val user = hashMapOf(
                                "displayName" to name, // 確保與 ProfileScreen 使用的欄位名一致
                                "email" to email,
                                "isTutor" to isTutor,
                                "bio" to "", // 初始化空的個人簡介
                                "profilePictureUrl" to null // 初始化頭像URL為null
                            )
                            firestore.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener {
                                    _registerState.value = RegisterState(isLoading = false)
                                    onResult(true)
                                }
                                .addOnFailureListener { e ->
                                    _registerState.value = RegisterState(isLoading = false, error = e.message ?: "儲存用戶資料失敗。")
                                    onResult(false)
                                }
                        } else {
                            _registerState.value = RegisterState(isLoading = false, error = task.exception?.message ?: "註冊失敗。")
                            onResult(false)
                        }
                    }
            } catch (e: Exception) {
                _registerState.value = RegisterState(isLoading = false, error = e.message ?: "發生未知錯誤。")
                onResult(false)
            }
        }
    }


    fun isTutor(userId: String, onResult: (Boolean) -> Unit) {
        // viewModelScope.launch { // 可以不在 viewModelScope 中，因為 Firestore SDK 會處理線程
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val isTutor = document.getBoolean("isTutor") ?: false
                onResult(isTutor)
            }
            .addOnFailureListener {
                // 發生錯誤時，可以預設為非Tutor或處理錯誤
                onResult(false)
            }
        // }
    }

    fun getUserProfile(uid: String, callback: (Map<String, Any>?) -> Unit) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    callback(document.data)
                } else {
                    callback(null) // 用戶資料不存在
                }
            }
            .addOnFailureListener {
                callback(null) // 獲取失敗
            }
    }

    fun updateProfile(
        uid: String,
        displayName: String,
        bio: String,
        profilePictureUri: Uri?, // 本地圖片 URI
        callback: (Boolean, String?) -> Unit // 返回成功狀態和可選的錯誤訊息
    ) {
        viewModelScope.launch {
            try {
                var profilePictureUrlToSave: String? = null

                // 1. 如果有新的 profilePictureUri，則上傳圖片到 Firebase Storage
                if (profilePictureUri != null) {
                    val storageRef = storage.reference.child("profile_pictures/$uid/${profilePictureUri.lastPathSegment}")
                    val uploadTask = storageRef.putFile(profilePictureUri).await() // 使用 await 等待上傳完成
                    profilePictureUrlToSave = uploadTask.storage.downloadUrl.await().toString() // 獲取下載 URL
                }

                // 2. 準備要更新到 Firestore 的資料
                val userUpdates = mutableMapOf<String, Any>()
                userUpdates["displayName"] = displayName
                userUpdates["bio"] = bio
                if (profilePictureUrlToSave != null) {
                    userUpdates["profilePictureUrl"] = profilePictureUrlToSave // 使用 "profilePictureUrl"
                } else {
                    // 如果用戶沒有選擇新圖片，我們需要檢查是否要刪除舊圖片或保留現有圖片
                    // 這裡的邏輯是：如果 profilePictureUri 是 null，則不更新 Firestore 中的 profilePictureUrl 欄位
                    // 如果你想實現刪除圖片的功能，需要額外邏輯
                }


                // 3. 更新 Firestore (使用 merge 避免覆蓋其他欄位，如果只想更新特定欄位)
                // 如果 profilePictureUrlToSave 為 null 且用戶之前有頭像，SetOptions.merge() 會保留舊的 URL。
                // 如果 profilePictureUrlToSave 有值，它會更新/新增該欄位。
                firestore.collection("users").document(uid)
                    .set(userUpdates, SetOptions.merge())
                    .addOnSuccessListener { callback(true, null) }
                    .addOnFailureListener { e -> callback(false, e.message ?: "更新資料失敗。") }

            } catch (e: Exception) {
                // 處理圖片上傳或 Firestore 更新過程中的異常
                callback(false, e.message ?: "更新個人資料時發生錯誤。")
            }
        }
    }


    fun signOut(onResult: () -> Unit) {
        auth.signOut()
        _currentUser.value = null
        _loginState.value = LoginState() // 重置登入狀態
        _registerState.value = RegisterState() // 重置註冊狀態
        onResult()
    }
}
