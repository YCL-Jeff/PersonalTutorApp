package com.example.personaltutorapp.model
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String,
    val email: String = "",
    val name: String = "",
    val isTutor: Boolean = false,
    val bio: String = "",
    val displayName: String = "",
    val profilePicture: String? = null // 存儲圖片 URL
)
