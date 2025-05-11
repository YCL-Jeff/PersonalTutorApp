package com.example.personaltutorapp.model

data class User(
    val uid: String,
    val email: String = "",
    val name: String = "",
    val isTutor: Boolean = false,
    val bio: String = "",
    val displayName: String = "",
    val profilePicture: String? = null
)