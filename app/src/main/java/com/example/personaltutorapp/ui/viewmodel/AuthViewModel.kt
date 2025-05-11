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

    fun register(email: String, password: String, id: String, displayName: String, isTutor: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _registerState.value = RegisterState(isLoading = true)

                // 檢查 ID 是否已存在
                val idQuery = firestore.collection("users")
                    .whereEqualTo("id", id)
                    .limit(1)
                    .get()
                    .await()
                if (idQuery.documents.isNotEmpty()) {
                    _registerState.value = RegisterState(isLoading = false, error = "ID already exists")
                    onResult(false)
                    return@launch
                }

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                            val user = hashMapOf(
                                "id" to id,
                                "displayName" to displayName, // 新增 displayName
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