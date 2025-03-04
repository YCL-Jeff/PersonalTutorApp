package com.example.personaltutorapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val courseId: Int = 0,
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val tutorId: String = ""
)