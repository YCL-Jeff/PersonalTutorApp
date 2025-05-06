package com.example.personaltutorapp.ui.viewmodel // 確保包名正確

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
import javax.inject.Inject

// --- 確保這個檔案裡只有 ViewModel 相關代碼 ---

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

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loginState.value = _loginState.value.copy(isLoading = true)
            try {
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

    fun register(email: String, password: String, name: String, isTutor: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _registerState.value = RegisterState(isLoading = true)

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                            val user = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "isTutor" to isTutor
                            )
                            firestore.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener {
                                    _registerState.value = RegisterState(isLoading = false)
                                    onResult(true)
                                }
                                .addOnFailureListener { e ->
                                    _registerState.value = RegisterState(isLoading = false, error = e.message)
                                    onResult(false)
                                }
                        } else {
                            _registerState.value = RegisterState(isLoading = false, error = task.exception?.message)
                            onResult(false)
                        }
                    }
            } catch (e: Exception) {
                _registerState.value = RegisterState(isLoading = false, error = e.message)
                onResult(false)
            }
        }
    }


    fun isTutor(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val isTutor = document.getBoolean("isTutor") ?: false
                    onResult(isTutor)
                }
                .addOnFailureListener {
                    onResult(false)
                }
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

    fun updateProfile(
        uid: String,
        displayName: String,
        bio: String,
        profilePictureUri: Uri?,
        callback: (Boolean) -> Unit
    ) {
        val userUpdates = mapOf(
            "displayName" to displayName,
            "bio" to bio,
            "profilePictureUri" to profilePictureUri?.toString()
        ).filterValues { it != null } // 過濾掉 null 值，避免覆蓋

        firestore.collection("users").document(uid).set(userUpdates, SetOptions.merge())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun signOut(onResult: () -> Unit) {
        auth.signOut()
        // 清理本地狀態（如果需要）
        _currentUser.value = null // 更新 currentUser 狀態
        onResult()
    }
}

// --- 確保這裡沒有 @Composable fun NavigationGraph() { ... } ---

    