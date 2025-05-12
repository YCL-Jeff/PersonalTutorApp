package com.example.personaltutorapp.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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

    fun login(idOrEmail: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loginState.value = _loginState.value.copy(isLoading = true)
            try {
                // 檢查輸入是否為電子郵件
                val email = if (idOrEmail.contains("@")) {
                    idOrEmail
                } else {
                    // 透過 ID 查詢 Firestore 以獲取 Email
                    val query = firestore.collection("users")
                        .whereEqualTo("id", idOrEmail)
                        .limit(1)
                        .get()
                        .await()
                    if (query.documents.isNotEmpty()) {
                        query.documents.first().getString("email") ?: throw Exception("Email not found for ID")
                    } else {
                        throw Exception("ID not found")
                    }
                }

                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _loginState.value = _loginState.value.copy(isLoading = false, error = null)
                        onResult(true)
                    } else {
                        _loginState.value = _loginState.value.copy(isLoading = false, error = task.exception?.message)
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                _loginState.value = _loginState.value.copy(isLoading = false, error = e.message)
                onResult(false)
            }
        }
    }

    fun register(
        email: String,
        password: String,
        id: String,
        displayName: String,
        isTutor: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _registerState.value = RegisterState(isLoading = true)

                // 直接創建用戶，跳過 ID 檢查
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId == null) {
                                _registerState.value = RegisterState(isLoading = false, error = "Failed to get user ID")
                                onResult(false)
                                return@addOnCompleteListener
                            }

                            // 創建用戶文檔
                            val userData = hashMapOf(
                                "id" to id,
                                "displayName" to displayName,
                                "email" to email,
                                "isTutor" to isTutor,
                                "createdAt" to System.currentTimeMillis()
                            )

                            // 嘗試設置文檔
                            firestore.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener {
                                    _registerState.value = RegisterState(isLoading = false)
                                    onResult(true)
                                }
                                .addOnFailureListener { e ->
                                    // 如果設置文檔失敗，但用戶已創建，仍然返回成功
                                    _registerState.value = RegisterState(
                                        isLoading = false,
                                        error = "Account created but profile setup failed: ${e.message}"
                                    )
                                    onResult(true)
                                }
                        } else {
                            _registerState.value = RegisterState(
                                isLoading = false,
                                error = authTask.exception?.message ?: "Registration failed"
                            )
                            onResult(false)
                        }
                    }
            } catch (e: Exception) {
                _registerState.value = RegisterState(isLoading = false, error = e.message ?: "Registration failed")
                onResult(false)
            }
        }
    }


    fun isTutor(uid: String, onResult: (Boolean) -> Unit) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val isTutor = document.getBoolean("isTutor") ?: false
                Log.d("AuthViewModel", "User $uid isTutor: $isTutor")
                onResult(isTutor)
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Error fetching user: ${e.message}", e)
                onResult(false)
            }
    }

    fun getUserProfile(uid: String, callback: (Map<String, Any>?) -> Unit) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    callback(document.data)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun updateProfile(uid: String, displayName: String, bio: String, profilePictureUri: Uri?, callback: (Boolean, String?) -> Unit) {
        val userUpdates = mapOf(
            "displayName" to displayName,
            "bio" to bio,
            "profilePictureUri" to profilePictureUri?.toString()
        )

        firestore.collection("users").document(uid).set(userUpdates, SetOptions.merge())
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun signOut(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                auth.signOut()
                _currentUser.value = null
                _loginState.value = LoginState(isLoading = false, error = null)
                _registerState.value = RegisterState(isLoading = false, error = null)
                onResult(true)
            } catch (e: Exception) {
                _loginState.value = LoginState(isLoading = false, error = e.message)
                _registerState.value = RegisterState(isLoading = false, error = e.message)
                onResult(false)
            }
        }
    }
}