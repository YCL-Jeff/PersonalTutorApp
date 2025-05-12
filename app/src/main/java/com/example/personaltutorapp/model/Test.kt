package com.example.personaltutorapp.model

import com.google.firebase.Timestamp

/**
 * Simplified Test data model.
 */
data class Test(
    var id: String = "", // Firestore will auto-generate ID, can be filled on retrieval. Var for that purpose.
    val courseId: String = "",
    val studentId: String = "",   // Custom ID of the student this test is for
    val tutorId: String = "",     // Custom ID of the tutor who created the test
    val content: String = "",     // Main content/questions of the test
    val createdAt: Timestamp = Timestamp.now()
)