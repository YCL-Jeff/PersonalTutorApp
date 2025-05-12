package com.example.personaltutorapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.net.URL
import java.net.HttpURLConnection
import java.io.OutputStreamWriter
import java.io.BufferedReader
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
data class EnrichedEnrollment(
    val enrollment: Enrollment,
    val student: User?
)

data class EnrolledCourse(
    val course: Course,
    val enrollmentStatus: String
)

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _currentTutorCustomIdInternal = MutableStateFlow<String?>(null)
    val currentTutorCustomId: StateFlow<String?> = _currentTutorCustomIdInternal.asStateFlow()

    private val _enrolledCoursesUi = MutableStateFlow<List<EnrolledCourse>>(emptyList())
    val enrolledCoursesUi: StateFlow<List<EnrolledCourse>> = _enrolledCoursesUi.asStateFlow()

    private val _isLoadingEnrolledCourses = MutableStateFlow(true)
    val isLoadingEnrolledCourses: StateFlow<Boolean> = _isLoadingEnrolledCourses.asStateFlow()

    private val _enrollmentError = MutableStateFlow<String?>(null)
    val enrollmentError: StateFlow<String?> = _enrollmentError.asStateFlow()

    private val _enrichedEnrollments = MutableStateFlow<List<EnrichedEnrollment>>(emptyList())
    val enrichedEnrollments: StateFlow<List<EnrichedEnrollment>> = _enrichedEnrollments.asStateFlow()

    private val _isLoadingEnrichedEnrollments = MutableStateFlow(true)
    val isLoadingEnrichedEnrollments: StateFlow<Boolean> = _isLoadingEnrichedEnrollments.asStateFlow()

    private val _enrichedEnrollmentsError = MutableStateFlow<String?>(null)
    val enrichedEnrollmentsError: StateFlow<String?> = _enrichedEnrollmentsError.asStateFlow()

    init {
        Log.d("CourseViewModel", "Initializing CourseViewModel")
        fetchCurrentUserCustomId()

        firestore.collection("courses")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CourseViewModel", "Error fetching courses: ${e.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { querySnapshot ->
                    val courseList = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Course::class.java)?.copy(courseId = doc.id)
                        } catch (ex: Exception) {
                            Log.e("CourseViewModel", "Error parsing course ${doc.id}: ${ex.message}")
                            null
                        }
                    }
                    _courses.value = courseList
                    Log.d("CourseViewModel", "Fetched ${courseList.size} courses")
                }
            }
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.uid?.let {
                Log.d("CourseViewModel", "Auth state changed, refreshing enrolled courses for userId: $it")
                viewModelScope.launch(Dispatchers.IO) {
                    delay(1000) // Delay to ensure customId is fetched
                    refreshEnrolledCourses()
                }
            } ?: run {
                Log.w("CourseViewModel", "No authenticated user, clearing enrolled courses")
                _enrolledCoursesUi.value = emptyList()
                _enrollmentError.value = "User not authenticated, please sign in"
            }
        }
    }

    private fun fetchCurrentUserCustomId() {
        viewModelScope.launch(Dispatchers.IO) {
            repeat(5) { attempt ->
                try {
                    val user = auth.currentUser
                    if (user == null) {
                        Log.w("CourseViewModel", "No authenticated user found, attempt ${attempt + 1}")
                        _currentTutorCustomIdInternal.value = null
                        _enrollmentError.value = "User not authenticated, please sign in"
                        delay(4000)
                        return@repeat
                    }
                    val userDoc = firestore.collection("users").document(user.uid).get().await()
                    if (!userDoc.exists()) {
                        Log.w("CourseViewModel", "No user document found for UID: ${user.uid}, attempt ${attempt + 1}")
                        _currentTutorCustomIdInternal.value = null
                        _enrollmentError.value = "User profile not found"
                        delay(4000)
                        return@repeat
                    }
                    val customId = userDoc.getString("id")
                    if (customId.isNullOrEmpty()) {
                        Log.w("CourseViewModel", "Custom ID is null or empty for UID: ${user.uid}, attempt ${attempt + 1}")
                        _currentTutorCustomIdInternal.value = null
                        _enrollmentError.value = "User profile missing custom ID"
                        delay(4000)
                        return@repeat
                    }
                    _currentTutorCustomIdInternal.value = customId
                    Log.d("CourseViewModel", "Fetched current tutor custom ID ('id' field): $customId for UID: ${user.uid}")
                    return@launch
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error fetching user custom ID for UID: ${auth.currentUser?.uid}, attempt ${attempt + 1}: ${e.message}", e)
                    if (attempt == 4) {
                        _currentTutorCustomIdInternal.value = null
                        _enrollmentError.value = "Failed to fetch user profile after retries: ${e.message}"
                    }
                    delay(4000)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val tutorCourses: Flow<List<Course>> = _currentTutorCustomIdInternal.flatMapLatest { tutorId ->
        if (tutorId == null) {
            flow { emit(emptyList<Course>()) }
        } else {
            _courses.map { allCourses ->
                allCourses.filter { course ->
                    course.tutorId == tutorId
                }
            }
        }
    }.catch { e ->
        Log.e("CourseViewModel", "Error in tutorCourses flow: ${e.message}")
        emit(emptyList<Course>())
    }.flowOn(Dispatchers.IO)

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    fun filterCourses(query: String, subjectFilter: String): Flow<List<Course>> = _courses.map { courseList ->
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
                val courseData: HashMap<String, Any> = hashMapOf(
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
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lessonData: HashMap<String, Any> = hashMapOf(
                    "courseId" to courseId,
                    "content" to content,
                    "title" to title,
                    "order" to order,
                    "completed" to false
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
                val tutorCustomId = courseDoc.getString("tutorId")
                val courseTitle = courseDoc.getString("title")

                if (tutorCustomId == null || courseTitle == null) {
                    Log.e("CourseViewModel", "Tutor ID or Course Title not found for course $courseId")
                    launch(Dispatchers.Main) { onResult(false) }
                    return@launch
                }

                val studentUserDoc = firestore.collection("users").document(studentAuthId).get().await()
                val studentUserObject = if (studentUserDoc.exists()) {
                    studentUserDoc.toObject(User::class.java)?.copy(uid = studentUserDoc.id)
                } else {
                    null
                }
                val studentDisplayNameForEnrollment = studentUserObject?.displayName?.takeIf { it.isNotEmpty() }
                    ?: studentUserObject?.id
                    ?: "Unknown Student (AuthUID: $studentAuthId)"

                val enrollmentData: HashMap<String, Any> = hashMapOf(
                    "courseId" to courseId,
                    "studentId" to (studentUserObject?.id ?: studentAuthId),
                    "tutorId" to tutorCustomId,
                    "status" to "pending",
                    "studentName" to studentDisplayNameForEnrollment,
                    "courseTitle" to courseTitle
                )

                firestore.collection("enrollments")
                    .add(enrollmentData)
                    .await()
                Log.d("CourseViewModel", "Enrollment requested: course $courseId, studentAuthId $studentAuthId")
                launch(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error requesting enrollment: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun fetchEnrichedEnrollmentsForCourse(courseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingEnrichedEnrollments.value = true
            _enrichedEnrollmentsError.value = null
            try {
                val enrollmentsSnapshot = firestore.collection("enrollments")
                    .whereEqualTo("courseId", courseId)
                    .get()
                    .await()

                val fetchedEnrichedEnrollments = coroutineScope {
                    enrollmentsSnapshot.documents.mapNotNull { doc ->
                        async(Dispatchers.IO) {
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
                                    Log.w("CourseViewModel", "Student document not found for studentId: ${enrollment.studentId}")
                                    null
                                }
                                EnrichedEnrollment(enrollment, student)
                            } else {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }
                _enrichedEnrollments.value = fetchedEnrichedEnrollments
                Log.d("CourseViewModel", "Fetched ${fetchedEnrichedEnrollments.size} enriched enrollments for course $courseId")
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error fetching enriched enrollments for course $courseId: ${e.message}", e)
                _enrichedEnrollments.value = emptyList()
                _enrichedEnrollmentsError.value = "Failed to load enrollments: ${e.message}"
            } finally {
                _isLoadingEnrichedEnrollments.value = false
            }
        }
    }

    fun updateEnrollmentStatus(
        enrollmentId: String,
        newStatus: String,
        student: User?,
        courseTitle: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("enrollments").document(enrollmentId)
                    .update("status", newStatus)
                    .await()
                Log.d("CourseViewModel", "Enrollment $enrollmentId status updated to $newStatus in Firestore.")

                if (student != null && courseTitle != null && (newStatus == "accepted" || newStatus == "rejected")) {
                    val studentEmail = student.email
                    val studentNameForEmail = student.displayName.takeIf { it.isNotEmpty() } ?: student.id

                    if (studentEmail.isNullOrEmpty()) {
                        Log.w("CourseViewModel", "Student email is empty for enrollment $enrollmentId.")
                        launch(Dispatchers.Main) { onResult(true, "Status updated. Student email missing for notification.") }
                        return@launch
                    }
                    val scriptUrl = "https://script.google.com/macros/s/AKfycbxyVdusqORtmC6MCrdPgLVo7XsfnKGN2oE9pXdprepkINfLYgjkXRkotR3qDv3XlLFO/exec"

                    try {
                        val url = URL(scriptUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        val jsonPayload = JSONObject()
                        jsonPayload.put("studentEmail", studentEmail)
                        jsonPayload.put("studentName", studentNameForEmail)
                        jsonPayload.put("courseTitle", courseTitle)
                        jsonPayload.put("enrollmentStatus", newStatus)

                        val outputStreamWriter = OutputStreamWriter(connection.outputStream, "UTF-8")
                        outputStreamWriter.write(jsonPayload.toString())
                        outputStreamWriter.flush()
                        outputStreamWriter.close()

                        val responseCode = connection.responseCode
                        Log.d("CourseViewModel", "Apps Script HTTP Response Code: $responseCode")

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val response = connection.inputStream.bufferedReader().readText()
                            Log.i("CourseViewModel", "Apps Script Response: $response")
                            launch(Dispatchers.Main) { onResult(true, "Status updated. Email notification sent.") }
                        } else {
                            val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "No error response"
                            Log.e("CourseViewModel", "Error calling Apps Script. Code: $responseCode, Response: $errorResponse")
                            launch(Dispatchers.Main) { onResult(true, "Status updated. Failed to send email notification (HTTP $responseCode).") }
                        }
                        connection.disconnect()
                    } catch (e: Exception) {
                        Log.e("CourseViewModel", "Exception during Apps Script call: ${e.message}", e)
                        launch(Dispatchers.Main) { onResult(true, "Status updated. Email notification failed: ${e.message}") }
                    }
                } else {
                    Log.w("CourseViewModel", "Skipping email notification for enrollment $enrollmentId.")
                    launch(Dispatchers.Main) { onResult(true, "Status updated.") }
                }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error updating enrollment status for $enrollmentId: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false, "Failed to update status: ${e.message}") }
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
                emit(0)
                return@flow
            }
            val completedLessonsCount = firestoreLessonsData.count { it.isCompleted }
            val progressPercentage = (completedLessonsCount * 100) / firestoreLessonsData.size
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
                .orderBy("order")
                .get()
                .await()
            val lessons = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Lesson::class.java)?.copy(lessonId = doc.id)
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error parsing lesson: ${e.message}")
                    null
                }
            }
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

    fun refreshEnrolledCourses() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingEnrolledCourses.value = true
            _enrollmentError.value = null
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.w("CourseViewModel", "User ID is empty for refreshEnrolledCourses.")
                _enrolledCoursesUi.value = emptyList()
                _isLoadingEnrolledCourses.value = false
                _enrollmentError.value = "User not authenticated, please sign in"
                return@launch
            }
            repeat(5) { attempt ->
                try {
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    if (!userDoc.exists()) {
                        Log.w("CourseViewModel", "No user document found for Auth UID: $userId, attempt ${attempt + 1}")
                        _enrolledCoursesUi.value = emptyList()
                        _isLoadingEnrolledCourses.value = false
                        _enrollmentError.value = "User profile not found"
                        delay(4000)
                        return@repeat
                    }
                    val isTutor = userDoc.getBoolean("isTutor") ?: false
                    val customId = userDoc.getString("id") ?: userId
                    Log.d("CourseViewModel", "Refreshing enrolled courses for userId: $userId, customId: $customId, isTutor: $isTutor, attempt ${attempt + 1}")

                    val enrolledCourses = mutableListOf<EnrolledCourse>()

                    // Tutor query
                    if (isTutor) {
                        // Query enrollments for tutor
                        val tutorSnapshot = firestore.collection("enrollments")
                            .whereEqualTo("tutorId", customId)
                            .whereIn("status", listOf("accepted", "pending"))
                            .get()
                            .await()
                        Log.d("CourseViewModel", "Found ${tutorSnapshot.documents.size} tutor enrollments for customId: $customId")

                        for (doc in tutorSnapshot.documents) {
                            val enrollment = doc.toObject(Enrollment::class.java)?.copy(enrollmentId = doc.id)
                            if (enrollment != null && enrollment.courseId.isNotEmpty()) {
                                val courseDoc = firestore.collection("courses").document(enrollment.courseId).get().await()
                                if (courseDoc.exists()) {
                                    val course = courseDoc.toObject(Course::class.java)?.copy(courseId = courseDoc.id)
                                    if (course != null) {
                                        enrolledCourses.add(EnrolledCourse(course, enrollment.status))
                                        Log.d("CourseViewModel", "Added tutor course from enrollments: ${course.title} (status: ${enrollment.status})")
                                    }
                                } else {
                                    Log.w("CourseViewModel", "Course document ${enrollment.courseId} not found for enrollment ${doc.id}")
                                }
                            }
                        }

                        // Query courses directly for tutor
                        val courseSnapshot = firestore.collection("courses")
                            .whereEqualTo("tutorId", customId)
                            .get()
                            .await()
                        Log.d("CourseViewModel", "Found ${courseSnapshot.documents.size} owned courses for customId: $customId")

                        for (doc in courseSnapshot.documents) {
                            val course = doc.toObject(Course::class.java)?.copy(courseId = doc.id)
                            if (course != null && !enrolledCourses.any { it.course.courseId == course.courseId }) {
                                enrolledCourses.add(EnrolledCourse(course, "owned"))
                                Log.d("CourseViewModel", "Added owned course: ${course.title} (status: owned)")
                            }
                        }
                    }

                    // Student query
                    val studentSnapshot = firestore.collection("enrollments")
                        .whereEqualTo("studentId", customId)
                        .whereIn("status", listOf("accepted", "pending"))
                        .get()
                        .await()
                    Log.d("CourseViewModel", "Found ${studentSnapshot.documents.size} student enrollments for customId: $customId")

                    for (doc in studentSnapshot.documents) {
                        val enrollment = doc.toObject(Enrollment::class.java)?.copy(enrollmentId = doc.id)
                        if (enrollment != null && enrollment.courseId.isNotEmpty()) {
                            val courseDoc = firestore.collection("courses").document(enrollment.courseId).get().await()
                            if (courseDoc.exists()) {
                                val course = courseDoc.toObject(Course::class.java)?.copy(courseId = courseDoc.id)
                                if (course != null && !enrolledCourses.any { it.course.courseId == course.courseId }) {
                                    enrolledCourses.add(EnrolledCourse(course, enrollment.status))
                                    Log.d("CourseViewModel", "Added student course: ${course.title} (status: ${enrollment.status})")
                                }
                            } else {
                                Log.w("CourseViewModel", "Course document ${enrollment.courseId} not found for enrollment ${doc.id}")
                            }
                        }
                    }

                    _enrolledCoursesUi.value = enrolledCourses
                    Log.d("CourseViewModel", "Refreshed ${enrolledCourses.size} enrolled courses for userId: $userId")
                    _isLoadingEnrolledCourses.value = false
                    _enrollmentError.value = null
                    return@launch
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error refreshing enrolled courses for userId: $userId, attempt ${attempt + 1}: ${e.message}", e)
                    if (attempt == 4) {
                        _enrolledCoursesUi.value = emptyList()
                        _isLoadingEnrolledCourses.value = false
                        _enrollmentError.value = "Failed to load courses after retries: ${e.message}"
                    }
                    delay(4000)
                }
            }
        }
    }
}