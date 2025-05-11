package com.example.personaltutorapp.model

data class Enrollment(
    val enrollmentId: String? = null,
    val courseId: String = "",
    val studentId: String = "",
    val tutorId: String = "",
    val status: String = "pending"
)