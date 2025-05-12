package com.example.personaltutorapp.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
data class EnrichedEnrollment(
    val enrollment: Enrollment,
    val student: User?
)

data class EnrolledCourse(
    val course: Course,
    val enrollmentStatus: String // "accepted" or "pending"
)

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage // 注入 FirebaseStorage
) : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _currentTutorCustomId = MutableStateFlow<String?>(null)

    init {
        Log.d("CourseViewModel", "Initializing CourseViewModel")
        fetchCurrentUserCustomId()

        firestore.collection("courses")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CourseViewModel", "Error fetching courses: ${e.message}", e)
                    _courses.value = emptyList()
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    Log.d("CourseViewModel", "Received courses snapshot with ${querySnapshot.documents.size} documents")
                    val courseList = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            val course = Course(
                                courseId = doc.id,
                                title = doc.getString("title") ?: "",
                                subject = doc.getString("subject") ?: "",
                                tutorId = doc.getString("tutorId") ?: "",
                                description = doc.getString("description") ?: ""
                            )
                            Log.d("CourseViewModel", "Parsed course: ${course.title} (tutorId: ${course.tutorId})")
                            course
                        } catch (ex: Exception) {
                            Log.e("CourseViewModel", "Error parsing course ${doc.id}: ${ex.message}", ex)
                            null
                        }
                    }
                    Log.d("CourseViewModel", "Fetched ${courseList.size} courses: ${courseList.map { "${it.title} (tutorId: ${it.tutorId})" }}")
                    _courses.value = courseList
                } ?: run {
                    Log.w("CourseViewModel", "Courses snapshot is null")
                    _courses.value = emptyList()
                }
            }
    }

    private fun fetchCurrentUserCustomId() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = auth.currentUser
            if (user == null) {
                Log.w("CourseViewModel", "No authenticated user found")
                _currentTutorCustomId.value = null
                return@launch
            }
            Log.d("CourseViewModel", "Fetching custom ID for user UID: ${user.uid}")

            repeat(3) { attempt ->
                try {
                    val userDoc = firestore.collection("users").document(user.uid).get().await()
                    if (userDoc.exists()) {
                        val customId = userDoc.getString("id")
                        if (customId.isNullOrEmpty()) {
                            Log.w("CourseViewModel", "User document exists but 'id' field is null or empty for UID: ${user.uid}")
                            _currentTutorCustomId.value = null
                        } else {
                            Log.d("CourseViewModel", "Fetched current tutor custom ID ('id' field): $customId for UID: ${user.uid}")
                            _currentTutorCustomId.value = customId
                        }
                        return@launch
                    } else {
                        Log.w("CourseViewModel", "No user document found for UID: ${user.uid}")
                        _currentTutorCustomId.value = null
                    }
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error fetching user custom ID for UID: ${user.uid} (attempt ${attempt + 1}/3): ${e.message}", e)
                    if (attempt < 2) delay(1000)
                }
            }
            Log.e("CourseViewModel", "Failed to fetch user custom ID after 3 attempts for UID: ${user.uid}")
            _currentTutorCustomId.value = null
        }
    }

    val tutorCourses: Flow<List<Course>> = _currentTutorCustomId.flatMapLatest { tutorId ->
        if (tutorId == null) {
            Log.w("CourseViewModel", "Tutor ID is null, emitting empty course list")
            flow { emit(emptyList<Course>()) }
        } else {
            _courses.map { allCourses ->
                Log.d("CourseViewModel", "Filtering courses for tutorId: $tutorId, total courses: ${allCourses.size}")
                val filteredCourses = allCourses.filter { course ->
                    val matches = course.tutorId == tutorId
                    Log.d("CourseViewModel", "Course ${course.title} (tutorId: ${course.tutorId}) matches tutorId $tutorId: $matches")
                    matches
                }
                Log.d("CourseViewModel", "Filtered ${filteredCourses.size} courses for tutorId: $tutorId: ${filteredCourses.map { it.title }}")
                filteredCourses
            }
        }
    }.catch { e ->
        Log.e("CourseViewModel", "Error in tutorCourses flow: ${e.message}", e)
        emit(emptyList<Course>())
    }.flowOn(Dispatchers.IO)

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    fun filterCourses(query: String, subjectFilter: String): Flow<List<Course>> = courses.map { courseList ->
        courseList.filter { course ->
            val matchesTitle = course.title.contains(query, ignoreCase = true)
            val matchesSubject = subjectFilter.isEmpty() || course.subject.equals(subjectFilter, ignoreCase = true)
            matchesTitle && matchesSubject
        }
    }.flowOn(Dispatchers.Default)

    fun createCourse(
        courseId: String,
        title: String,
        description: String,
        subject: String,
        tutorCustomId: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseData = hashMapOf(
                    "title" to title,
                    "description" to description,
                    "subject" to subject,
                    "tutorId" to tutorCustomId
                )
                firestore.collection("courses")
                    .document(courseId)
                    .set(courseData)
                    .await()
                Log.d("CourseViewModel", "Course created: $courseId with tutorCustomId: $tutorCustomId")
                launch(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error creating course: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun createLesson(
        courseId: String,
        title: String,
        content: String,
        order: Int,
        mediaUri: Uri? = null,
        mediaType: String? = null,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var mediaUrl: String? = null
                // 如果有媒体文件，上传到 Firebase Storage
                if (mediaUri != null && mediaType != null) {
                    val fileRef = storage.reference.child("lessons/$courseId/${System.currentTimeMillis()}_${mediaUri.lastPathSegment}")
                    val uploadTask = fileRef.putFile(mediaUri).await()
                    mediaUrl = uploadTask.metadata?.reference?.downloadUrl?.await()?.toString()
                    Log.d("CourseViewModel", "Media uploaded: $mediaUrl (type: $mediaType)")
                }

                val lessonData = hashMapOf(
                    "courseId" to courseId,
                    "content" to content,
                    "title" to title,
                    "order" to order,
                    "completed" to false,
                    "mediaUrl" to mediaUrl,
                    "mediaType" to mediaType
                )
                firestore.collection("lessons")
                    .add(lessonData)
                    .await()
                Log.d("CourseViewModel", "Lesson created successfully for course: $courseId")
                launch(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Failed to create lesson: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun requestEnrollment(courseId: String, studentAuthId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseDoc = firestore.collection("courses").document(courseId).get().await()
                if (!courseDoc.exists()) {
                    Log.e("CourseViewModel", "Course document not found for courseId: $courseId")
                    launch(Dispatchers.Main) { onResult(false) }
                    return@launch
                }
                val tutorCustomId = courseDoc.getString("tutorId")
                val courseTitle = courseDoc.getString("title")

                if (tutorCustomId == null || courseTitle == null) {
                    Log.e("CourseViewModel", "Tutor ID or Course Title not found for course $courseId")
                    launch(Dispatchers.Main) { onResult(false) }
                    return@launch
                }

                val studentUserDoc = firestore.collection("users").document(studentAuthId).get().await()
                if (!studentUserDoc.exists()) {
                    Log.e("CourseViewModel", "No user document found for student Auth UID: $studentAuthId")
                    launch(Dispatchers.Main) { onResult(false) }
                    return@launch
                }

                val studentUserObject = studentUserDoc.toObject(User::class.java)?.copy(uid = studentUserDoc.id)
                val studentCustomId = studentUserObject?.id?.takeIf { it.isNotEmpty() } ?: run {
                    Log.w("CourseViewModel", "Student custom ID not found for Auth UID: $studentAuthId, using Auth UID as fallback")
                    studentAuthId
                }
                val studentDisplayName = studentUserObject?.displayName?.takeIf { it.isNotEmpty() } ?: studentCustomId

                val enrollmentData = hashMapOf(
                    "courseId" to courseId,
                    "studentId" to studentCustomId,
                    "tutorId" to tutorCustomId,
                    "status" to "pending",
                    "studentName" to studentDisplayName,
                    "courseTitle" to courseTitle
                )

                firestore.collection("enrollments")
                    .add(enrollmentData)
                    .await()
                Log.d("CourseViewModel", "Enrollment requested: course $courseId, studentCustomId $studentCustomId, studentName $studentDisplayName")
                launch(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error requesting enrollment for course $courseId, studentAuthId $studentAuthId: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun getEnrichedEnrollmentsForCourse(courseId: String): Flow<List<EnrichedEnrollment>> = callbackFlow {
        val listenerRegistration: ListenerRegistration = firestore.collection("enrollments")
            .whereEqualTo("courseId", courseId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CourseViewModel", "Error fetching enrollments for course $courseId: ${e.message}", e)
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val enrichedEnrollments = coroutineScope {
                                querySnapshot.documents.mapNotNull { doc ->
                                    async {
                                        val enrollment = doc.toObject(Enrollment::class.java)?.copy(enrollmentId = doc.id)
                                        if (enrollment != null) {
                                            val studentDoc = firestore.collection("users")
                                                .whereEqualTo("id", enrollment.studentId)
                                                .limit(1)
                                                .get()
                                                .await()
                                            val student = if (studentDoc.documents.isNotEmpty()) {
                                                studentDoc.documents.first().toObject(User::class.java)?.copy(uid = studentDoc.documents.first().id)
                                            } else {
                                                Log.w("CourseViewModel", "Student document not found for studentId (custom ID): ${enrollment.studentId}")
                                                null
                                            }
                                            EnrichedEnrollment(enrollment, student)
                                        } else {
                                            Log.w("CourseViewModel", "Failed to parse enrollment document: ${doc.id}")
                                            null
                                        }
                                    }
                                }.awaitAll().filterNotNull()
                            }
                            Log.d("CourseViewModel", "Fetched ${enrichedEnrollments.size} enriched enrollments for course $courseId: ${enrichedEnrollments.map { "${it.enrollment.studentName} (status: ${it.enrollment.status})" }}")
                            trySend(enrichedEnrollments).isSuccess
                        } catch (e: Exception) {
                            Log.e("CourseViewModel", "Error processing enriched enrollments for course $courseId: ${e.message}", e)
                            trySend(emptyList()).isSuccess
                        }
                    }
                } ?: Log.w("CourseViewModel", "Enrollment snapshot is null for course $courseId")
            }

        awaitClose { listenerRegistration.remove() }
    }.flowOn(Dispatchers.IO)

    fun updateEnrollmentStatus(
        enrollmentId: String,
        newStatus: String,
        student: User?,
        courseTitle: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("enrollments").document(enrollmentId)
                    .update("status", newStatus)
                    .await()
                Log.d("CourseViewModel", "Enrollment $enrollmentId status updated to $newStatus")
                if (student != null && courseTitle != null) {
                    val studentEmail = student.email
                    val studentNameForEmail = student.displayName.takeIf { it.isNotEmpty() } ?: student.id
                    val subject: String
                    val body: String
                    if (newStatus == "accepted") {
                        subject = "Your course enrollment has been approved!"
                        body = "Dear ${studentNameForEmail},\n\nCongratulations! Your enrollment for the course '${courseTitle}' has been approved.\n\nHappy learning!"
                    } else if (newStatus == "rejected") {
                        subject = "Regarding your course enrollment"
                        body = "Dear ${studentNameForEmail},\n\nWe regret to inform you that your enrollment for the course '${courseTitle}' has been rejected.\n\nPlease contact us if you have any questions."
                    } else {
                        launch(Dispatchers.Main) { onResult(true) }
                        return@launch
                    }
                    Log.i("EmailNotification", "---- SIMULATING EMAIL ----")
                    Log.i("EmailNotification", "To: $studentEmail")
                    Log.i("EmailNotification", "Subject: $subject")
                    Log.i("EmailNotification", "Body: \n$body")
                    Log.i("EmailNotification", "--------------------------")
                } else {
                    Log.w("CourseViewModel", "Cannot send email notification: student details or course title missing. Student: $student, CourseTitle: $courseTitle")
                }
                launch(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error updating enrollment status for $enrollmentId: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun getEnrolledStudents(courseId: String): Flow<List<Enrollment>> = flow {
        try {
            val snapshot = firestore.collection("enrollments")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
            val enrollments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Enrollment::class.java)?.copy(enrollmentId = doc.id)
            }
            Log.d("CourseViewModel", "Fetched ${enrollments.size} enrolled students for course $courseId")
            emit(enrollments)
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching enrollments for course $courseId: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getCourseProgress(courseId: String): Flow<Int> = flow {
        try {
            val lessonsSnapshot = firestore.collection("lessons")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
            val firestoreLessonsData = lessonsSnapshot.documents.map { doc ->
                object {
                    val isCompleted = doc.getBoolean("completed") ?: false
                }
            }
            if (firestoreLessonsData.isEmpty()) {
                Log.d("CourseViewModel", "No lessons found for course $courseId, progress is 0%")
                emit(0)
                return@flow
            }
            val completedLessonsCount = firestoreLessonsData.count { it.isCompleted }
            val progressPercentage = (completedLessonsCount * 100) / firestoreLessonsData.size
            Log.d("CourseViewModel", "Course $courseId progress: $completedLessonsCount/$firestoreLessonsData.size lessons completed ($progressPercentage%)")
            emit(progressPercentage)
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching course progress for $courseId: ${e.message}", e)
            emit(0)
        }
    }.flowOn(Dispatchers.IO)

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
                        isCompleted = doc.getBoolean("completed") ?: false,
                        mediaUrl = doc.getString("mediaUrl"),
                        mediaType = doc.getString("mediaType")
                    )
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error parsing lesson: ${e.message}")
                    null
                }
            }
            Log.d("CourseViewModel", "Fetched ${lessons.size} lessons for course $courseId")
            emit(lessons)
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching lessons for course $courseId: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun completeLesson(lessonId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("lessons")
                    .document(lessonId)
                    .update("completed", true)
                    .await()
                Log.d("CourseViewModel", "Lesson $lessonId marked as completed in Firestore.")
                launch(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error completing lesson $lessonId: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun getEnrolledCourses(): Flow<List<EnrolledCourse>> = flow {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            Log.w("CourseViewModel", "Student User ID is empty, cannot fetch enrolled courses.")
            emit(emptyList())
            return@flow
        }
        try {
            Log.d("CourseViewModel", "Fetching enrolled courses for userId: $userId")
            val studentDoc = firestore.collection("users").document(userId).get().await()
            if (!studentDoc.exists()) {
                Log.w("CourseViewModel", "No user document found for student Auth UID: $userId")
                emit(emptyList())
                return@flow
            }
            val studentCustomId = studentDoc.getString("id") ?: run {
                Log.w("CourseViewModel", "Student custom ID not found for Auth UID: $userId, using Auth UID as fallback")
                userId
            }
            Log.d("CourseViewModel", "Student custom ID: $studentCustomId")

            val enrollmentSnapshot = firestore.collection("enrollments")
                .whereEqualTo("studentId", studentCustomId)
                .whereIn("status", listOf("accepted", "pending"))
                .get()
                .await()
            Log.d("CourseViewModel", "Found ${enrollmentSnapshot.documents.size} enrollments for studentCustomId: $studentCustomId")

            val enrolledCourses = mutableListOf<EnrolledCourse>()
            for (doc in enrollmentSnapshot.documents) {
                val enrollment = doc.toObject(Enrollment::class.java)?.copy(enrollmentId = doc.id)
                if (enrollment != null) {
                    Log.d("CourseViewModel", "Processing enrollment: courseId=${enrollment.courseId}, status=${enrollment.status}")
                    val courseDoc = firestore.collection("courses").document(enrollment.courseId).get().await()
                    if (courseDoc.exists()) {
                        val course = Course(
                            courseId = courseDoc.id,
                            title = courseDoc.getString("title") ?: "",
                            subject = courseDoc.getString("subject") ?: "",
                            tutorId = courseDoc.getString("tutorId") ?: "",
                            description = courseDoc.getString("description") ?: ""
                        )
                        enrolledCourses.add(EnrolledCourse(course, enrollment.status))
                        Log.d("CourseViewModel", "Added course: ${course.title} (status: ${enrollment.status})")
                    } else {
                        Log.w("CourseViewModel", "Course document not found for courseId: ${enrollment.courseId}")
                    }
                } else {
                    Log.w("CourseViewModel", "Failed to parse enrollment document: ${doc.id}")
                }
            }
            Log.d("CourseViewModel", "Fetched ${enrolledCourses.size} enrolled courses for studentCustomId: $studentCustomId: ${enrolledCourses.map { "${it.course.title} (status: ${it.enrollmentStatus})" }}")
            emit(enrolledCourses)
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching enrolled courses for student $userId: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}