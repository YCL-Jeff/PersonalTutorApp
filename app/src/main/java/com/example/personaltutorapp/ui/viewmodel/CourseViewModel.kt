package com.example.personaltutorapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.model.Lesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    init {
        // 實時監聽 Firestore 的 courses 集合
        firestore.collection("courses")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CourseViewModel", "Error fetching courses: ${e.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    val courseList = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            Course(
                                courseId = doc.id,
                                title = doc.getString("title") ?: "",
                                subject = doc.getString("subject") ?: "",
                                tutorId = doc.getString("tutorId") ?: "",
                                description = doc.getString("description") ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e("CourseViewModel", "Error parsing course: ${e.message}")
                            null
                        }
                    }
                    _courses.value = courseList
                }
            }
    }

    // 獲取當前用戶 ID
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // 過濾課程
    fun filterCourses(query: String, subject: String): Flow<List<Course>> = flow {
        val filteredCourses = _courses.value.filter { course ->
            course.title.contains(query, ignoreCase = true) &&
                    (subject.isEmpty() || course.subject.equals(subject, ignoreCase = true))
        }
        emit(filteredCourses)
    }

    // 創建課程
    fun createCourse(
        courseId: String,
        title: String,
        description: String,
        subject: String,
        tutorId: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val courseData = hashMapOf(
                    "title" to title,
                    "description" to description,
                    "subject" to subject,
                    "tutorId" to tutorId
                )
                firestore.collection("courses")
                    .document(courseId)
                    .set(courseData)
                    .addOnSuccessListener {
                        Log.d("CourseViewModel", "Course created: $courseId")
                        onResult(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e("CourseViewModel", "Error creating course: ${e.message}", e)
                        onResult(false)
                    }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error creating course: ${e.message}", e)
                onResult(false)
            }
        }
    }

    // 創建課程
    fun createLesson(
        courseId: String,
        title: String,
        content: String,
        order: Int,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val lesson = Lesson(
                    lessonId = null, // Firestore 自動生成 ID
                    courseId = courseId,
                    title = title,
                    content = content,
                    order = order,
                    isCompleted = false
                )
                val documentRef = firestore.collection("lessons")
                    .add(lesson)
                    .await()
                Log.d("CourseViewModel", "Lesson created successfully with ID: ${documentRef.id}")
                onResult(true)
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Failed to create lesson: ${e.message}")
                onResult(false)
            }
        }
    }

    // 請求報名
    fun requestEnrollment(courseId: String, studentId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val enrollment = Enrollment(
                    enrollmentId = null,
                    courseId = courseId,
                    studentId = studentId,
                    tutorId = _courses.value.find { it.courseId == courseId }?.tutorId ?: "",
                    status = "pending"
                )
                firestore.collection("enrollments")
                    .add(enrollment)
                    .await()
                Log.d("CourseViewModel", "Enrollment requested successfully")
                onResult(true)
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error requesting enrollment: ${e.message}")
                onResult(false)
            }
        }
    }

    // 獲取課程的報名學生
    fun getEnrolledStudents(courseId: String): Flow<List<Enrollment>> = flow {
        try {
            val snapshot = firestore.collection("enrollments")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
            val enrollments = snapshot.documents.mapNotNull { doc ->
                try {
                    Enrollment(
                        enrollmentId = doc.id,
                        courseId = doc.getString("courseId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        tutorId = doc.getString("tutorId") ?: "",
                        status = doc.getString("status") ?: "pending"
                    )
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error parsing enrollment: ${e.message}")
                    null
                }
            }
            emit(enrollments)
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching enrollments: ${e.message}")
            emit(emptyList())
        }
    }

    // 獲取課程的課程進度（簡化版，僅計數完成課程）
    fun getCourseProgress(courseId: String): Flow<Int> = flow {
        try {
            val lessonsSnapshot = firestore.collection("lessons")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
            val lessons = lessonsSnapshot.documents.mapNotNull { doc ->
                try {
                    Lesson(
                        lessonId = doc.id,
                        courseId = doc.getString("courseId") ?: "",
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        order = doc.getLong("order")?.toInt() ?: 0,
                        isCompleted = doc.getBoolean("isCompleted") ?: false
                    )
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error parsing lesson: ${e.message}")
                    null
                }
            }
            val totalLessons = lessons.size
            if (totalLessons == 0) {
                emit(0)
                return@flow
            }
            val completedCount = lessons.count { it.isCompleted }
            val progressPercentage = (completedCount * 100) / totalLessons
            emit(progressPercentage)
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching progress: ${e.message}")
            emit(0)
        }
    }

    // 獲取課程的課程列表
    fun getLessonsByCourse(courseId: String): Flow<List<Lesson>> = flow {
        try {
            val snapshot = firestore.collection("lessons")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
            val lessons = snapshot.documents.mapNotNull { doc ->
                try {
                    Lesson(
                        lessonId = doc.id,
                        courseId = doc.getString("courseId") ?: "",
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        order = doc.getLong("order")?.toInt() ?: 0,
                        isCompleted = doc.getBoolean("isCompleted") ?: false
                    )
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error parsing lesson: ${e.message}")
                    null
                }
            }
            emit(lessons)
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching lessons: ${e.message}")
            emit(emptyList())
        }
    }

    // 完成課程
    fun completeLesson(lessonId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("lessons")
                    .document(lessonId)
                    .update("isCompleted", true)
                    .await()
                Log.d("CourseViewModel", "Lesson $lessonId marked as completed")
                onResult(true)
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error completing lesson: ${e.message}")
                onResult(false)
            }
        }
    }

    fun getEnrolledCourses(): Flow<List<Course>> = flow {
        try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.w("CourseViewModel", "User ID is empty")
                emit(emptyList())
                return@flow
            }
            val enrollmentSnapshot = firestore.collection("enrollments")
                .whereEqualTo("studentId", userId)
                .whereEqualTo("status", "accepted")
                .get()
                .await()
            val courseIds = enrollmentSnapshot.documents.mapNotNull { it.getString("courseId") }
            val courses = mutableListOf<Course>()
            for (courseId in courseIds) {
                val courseDoc = firestore.collection("courses")
                    .document(courseId)
                    .get()
                    .await()
                courseDoc.toObject(Course::class.java)?.let { course ->
                    courses.add(course.copy(courseId = courseId))
                }
            }
            Log.d("CourseViewModel", "Enrolled courses: ${courses.size}")
            emit(courses)
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching enrolled courses: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO) // 在 IO 線程執行
}