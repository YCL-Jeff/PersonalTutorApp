package com.example.personaltutorapp.model

data class Enrollment(
    val enrollmentId: String? = null,
    val courseId: String = "",
    val studentId: String = "",
    val tutorId: String = "",
    val status: String = "pending",
    val studentName: String = "", // Added to match Firestore data structure
    val courseTitle: String = ""  // Added to match Firestore data structure
)