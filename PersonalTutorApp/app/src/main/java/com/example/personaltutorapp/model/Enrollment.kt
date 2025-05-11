package com.example.personaltutorapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enrollments")
data class Enrollment(
    @PrimaryKey(autoGenerate = true) val enrollmentId: Int = 0,
    val courseId: Int,
    val studentId: String,
    val status: String = "pending"
)
