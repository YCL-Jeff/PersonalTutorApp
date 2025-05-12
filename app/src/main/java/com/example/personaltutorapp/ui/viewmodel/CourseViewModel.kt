package com.example.personaltutorapp.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
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
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.CourseProgress
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.model.Test
import com.example.personaltutorapp.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _currentTutorCustomId = MutableStateFlow<String?>(null)
    private val _currentUserProfile = MutableStateFlow<User?>(null)

    init {
        Log.d("CourseViewModel", "Initializing CourseViewModel")
        fetchCurrentUserProfile()

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

    private fun fetchCurrentUserProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = auth.currentUser
            if (user == null) {
                Log.w("CourseViewModel", "No authenticated user found")
                _currentTutorCustomId.value = null
                _currentUserProfile.value = null
                return@launch
            }
            Log.d("CourseViewModel", "Fetching user profile for UID: ${user.uid}")

            repeat(3) { attempt ->
                try {
                    val userDoc = firestore.collection("users").document(user.uid).get().await()
                    if (userDoc.exists()) {
                        val userProfile = userDoc.toObject<User>()?.copy(uid = userDoc.id)
                        if (userProfile == null || userProfile.id.isEmpty()) {
                            Log.w("CourseViewModel", "User document exists but profile is invalid or 'id' field is empty for UID: ${user.uid}")
                            _currentTutorCustomId.value = null
                            _currentUserProfile.value = null
                        } else {
                            Log.d("CourseViewModel", "Fetched user profile: ${userProfile.id}, displayName: ${userProfile.displayName}, email: ${userProfile.email}")
                            _currentTutorCustomId.value = userProfile.id
                            _currentUserProfile.value = userProfile
                        }
                        return@launch
                    } else {
                        Log.w("CourseViewModel", "No user document found for UID: ${user.uid}")
                        _currentTutorCustomId.value = null
                        _currentUserProfile.value = null
                    }
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error fetching user profile for UID: ${user.uid} (attempt ${attempt + 1}/3): ${e.message}", e)
                    if (attempt < 2) delay(1000)
                }
            }
            Log.e("CourseViewModel", "Failed to fetch user profile after 3 attempts for UID: ${user.uid}")
            _currentTutorCustomId.value = null
            _currentUserProfile.value = null
        }
    }

    val tutorCourses: Flow<List<Course>> = _currentTutorCustomId.flatMapLatest { tutorId ->
        if (tutorId == null) {
            Log.w("CourseViewModel", "Tutor ID is null, emitting empty course list for tutorCourses flow")
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
                if (mediaUri != null && mediaType != null) {
                    try {
                        val fileRef = storage.reference.child("lessons/$courseId/${System.currentTimeMillis()}_${mediaUri.lastPathSegment}")
                        val uploadTask = fileRef.putFile(mediaUri).await()
                        mediaUrl = uploadTask.metadata?.reference?.downloadUrl?.await()?.toString()
                        Log.d("CourseViewModel", "Media uploaded successfully: $mediaUrl (type: $mediaType)")
                    } catch (e: Exception) {
                        Log.e("CourseViewModel", "Failed to upload media: ${e.message}", e)
                        launch(Dispatchers.Main) { onResult(false) }
                        return@launch
                    }
                }

                val lessonData = hashMapOf(
                    "courseId" to courseId,
                    "content" to content,
                    "title" to title,
                    "order" to order,
                    "mediaUrl" to mediaUrl,
                    "mediaType" to mediaType
                )
                firestore.collection("lessons")
                    .add(lessonData)
                    .await()
                Log.d("CourseViewModel", "Lesson created successfully for course: $courseId")

                // Find course title
                val course = _courses.value.find { it.courseId == courseId }
                val courseTitle = course?.title ?: "Unknown Course"

                // Get tutor display name
                val tutorName = _currentUserProfile.value?.displayName?.takeIf { it.isNotEmpty() }
                    ?: _currentUserProfile.value?.id?.takeIf { it.isNotEmpty() } ?: "Tutor"

                // Notify students
                notifyStudentsOfNewLesson(courseId, courseTitle, title, tutorName)

                updateCourseProgressForNewLesson(courseId)

                launch(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Failed to create lesson: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    private suspend fun getAcceptedStudentEmailsForCourse(courseId: String): List<String> {
        return try {
            // Query accepted enrollments
            val enrollmentsSnapshot = firestore.collection("enrollments")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("status", "accepted")
                .get()
                .await()

            // Fetch student emails in parallel
            coroutineScope {
                enrollmentsSnapshot.documents.mapNotNull { doc ->
                    async {
                        val studentId = doc.getString("studentId") ?: return@async null
                        try {
                            val studentUserSnapshot = firestore.collection("users")
                                .whereEqualTo("id", studentId)
                                .limit(1)
                                .get()
                                .await()
                            if (studentUserSnapshot.isEmpty) {
                                Log.w("CourseViewModel", "No user document found for studentId: $studentId")
                                return@async null
                            }
                            val studentUser = studentUserSnapshot.documents.first().toObject<User>()
                            studentUser?.email?.takeIf { it.isNotEmpty() }?.also {
                                Log.d("CourseViewModel", "Found email for studentId $studentId: $it")
                            }
                        } catch (e: Exception) {
                            Log.e("CourseViewModel", "Error fetching email for studentId $studentId: ${e.message}", e)
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching student emails for course $courseId: ${e.message}", e)
            emptyList()
        }
    }

    private fun notifyStudentsOfNewLesson(
        courseId: String,
        courseTitle: String,
        lessonTitle: String,
        tutorName: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get student email list
                val studentEmails = getAcceptedStudentEmailsForCourse(courseId)
                if (studentEmails.isEmpty()) {
                    Log.w("CourseViewModel", "No accepted students found for course $courseId")
                    return@launch
                }
                Log.d("CourseViewModel", "Found ${studentEmails.size} student emails for course $courseId: $studentEmails")

                // Construct JSON payload
                val payload = JSONObject().apply {
                    put("courseTitle", courseTitle)
                    put("lessonTitle", lessonTitle)
                    put("tutorName", tutorName)
                    put("studentEmails", JSONArray(studentEmails))
                }

                // Send HTTP POST request to Apps Script
                val url = URL("https://script.google.com/macros/s/AKfycbzFFc5jTmGR7A3GX2hnuLCPM1JgmOIhSuZ-42PmAbFGh8rWkyM-xe2zTFR36YzqTqdn/exec")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                    outputStream.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                // Get response
                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                if (responseCode == 200) {
                    Log.d("CourseViewModel", "Apps Script called successfully: $response")
                } else {
                    Log.e("CourseViewModel", "Apps Script call failed with code $responseCode: $response")
                }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error notifying students for new lesson in course $courseId: ${e.message}", e)
            }
        }
    }

    private suspend fun updateCourseProgressForNewLesson(courseId: String) {
        try {
            // Get all accepted students for the course
            val enrollmentsSnapshot = firestore.collection("enrollments")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("status", "accepted")
                .get()
                .await()

            // Get total number of lessons
            val lessonsQuery = firestore.collection("lessons")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()

            val totalLessons = lessonsQuery.size()

            Log.d("CourseViewModel", "Updating CourseProgress for all enrolled students. Total lessons: $totalLessons")

            // Update CourseProgress for each student
            enrollmentsSnapshot.documents.forEach { doc ->
                val studentId = doc.getString("studentId") ?: return@forEach

                // Get student's auth UID
                val studentUserSnapshot = firestore.collection("users")
                    .whereEqualTo("id", studentId)
                    .limit(1)
                    .get()
                    .await()

                if (studentUserSnapshot.isEmpty) {
                    Log.w("CourseViewModel", "Student user document not found for studentId: $studentId")
                    return@forEach
                }

                val studentUserDoc = studentUserSnapshot.documents.first()
                val studentAuthUid = studentUserDoc.id // The document ID is the auth UID

                // Query the student's CourseProgress record
                val progressQuery = firestore.collection("CourseProgress")
                    .whereEqualTo("courseId", courseId)
                    .whereEqualTo("userAuthId", studentAuthUid)
                    .limit(1)
                    .get()
                    .await()

                if (progressQuery.isEmpty) {
                    // If no record exists, create a new one
                    val studentName = doc.getString("studentName") ?: studentId

                    // Create new progress record
                    val progressData = hashMapOf(
                        "courseId" to courseId,
                        "userAuthId" to studentAuthUid,
                        "studentId" to studentId,
                        "completedLessons" to 0,
                        "totalLessons" to totalLessons,
                        "studentName" to studentName
                    )

                    firestore.collection("CourseProgress")
                        .add(progressData)
                        .await()

                    Log.d("CourseViewModel", "Created new CourseProgress for student: $studentId, course: $courseId. Total lessons: $totalLessons")
                } else {
                    val progressDoc = progressQuery.documents.first()
                    firestore.collection("CourseProgress")
                        .document(progressDoc.id)
                        .update(
                            mapOf(
                                "totalLessons" to totalLessons,
                                "userAuthId" to studentAuthUid
                            )
                        )
                        .await()

                    Log.d("CourseViewModel", "Updated totalLessons in existing CourseProgress for student: $studentId, course: $courseId. Total lessons: $totalLessons")
                }
            }

            Log.d("CourseViewModel", "Finished updating CourseProgress for all enrolled students")
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error updating CourseProgress: ${e.message}", e)
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
        val listenerRegistration = firestore.collection("enrollments")
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
                            Log.d("CourseViewModel", "Fetched ${enrichedEnrollments.size} enriched enrollments for course $courseId")
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
                val enrollmentDoc = firestore.collection("enrollments")
                    .document(enrollmentId)
                    .get()
                    .await()

                if (!enrollmentDoc.exists()) {
                    Log.e("CourseViewModel", "Enrollment document not found: $enrollmentId")
                    launch(Dispatchers.Main) { onResult(false) }
                    return@launch
                }

                val courseId = enrollmentDoc.getString("courseId")
                val studentId = enrollmentDoc.getString("studentId")

                if (courseId == null || studentId == null) {
                    Log.e("CourseViewModel", "CourseId or StudentId missing in enrollment: $enrollmentId")
                    launch(Dispatchers.Main) { onResult(false) }
                    return@launch
                }

                firestore.collection("enrollments").document(enrollmentId)
                    .update("status", newStatus)
                    .await()
                Log.d("CourseViewModel", "Enrollment $enrollmentId status updated to $newStatus")

                if (newStatus == "accepted") {
                    createInitialCourseProgress(courseId, studentId, student?.displayName ?: "")
                }

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
                    Log.w("CourseViewModel", "Cannot send email notification: student details or course title missing")
                }
                launch(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error updating enrollment status for $enrollmentId: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    private suspend fun createInitialCourseProgress(courseId: String, studentId: String, studentName: String) {
        try {
            // Find student's auth UID
            val studentUserQuery = firestore.collection("users")
                .whereEqualTo("id", studentId)
                .limit(1)
                .get()
                .await()

            if (studentUserQuery.isEmpty) {
                Log.w("CourseViewModel", "createInitialCourseProgress: Student user not found for ID: $studentId")
                return
            }

            val studentUserDoc = studentUserQuery.documents.first()
            val userAuthId = studentUserDoc.id // Document ID is the auth UID

            // Get total number of lessons
            val lessonsSnapshot = firestore.collection("lessons")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()

            val totalLessons = lessonsSnapshot.size()

            // Create progress record using composite ID
            val progressDocId = "$userAuthId-$courseId"

            val progressData = hashMapOf(
                "courseId" to courseId,
                "userAuthId" to userAuthId,
                "studentId" to studentId,
                "completedLessons" to 0,
                "totalLessons" to totalLessons,
                "studentName" to studentName.ifEmpty { studentId }
            )

            firestore.collection("CourseProgress")
                .document(progressDocId)
                .set(progressData)
                .await()

            Log.d("CourseViewModel", "Created initial CourseProgress for student $studentId (auth: $userAuthId) in course $courseId with $totalLessons total lessons")
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error creating initial CourseProgress: ${e.message}", e)
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
            val userAuthId = getCurrentUserId()
            if (userAuthId.isEmpty()) {
                emit(0)
                return@flow
            }

            // Get total number of lessons
            val lessonsSnapshot = firestore.collection("lessons")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()

            val totalLessons = lessonsSnapshot.size()
            if (totalLessons == 0) {
                Log.d("CourseViewModel", "No lessons found for course $courseId, progress is 0%")
                emit(0)
                return@flow
            }

            // Get number of completed lessons
            val completedLessonsSnapshot = firestore.collection("users")
                .document(userAuthId)
                .collection("completedLessons")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()

            val completedLessons = completedLessonsSnapshot.size()

            // Calculate progress percentage
            val progressPercentage = if (totalLessons > 0) {
                (completedLessons * 100) / totalLessons
            } else {
                0
            }

            Log.d("CourseViewModel", "Course $courseId progress: $completedLessons/$ lessons completed ($progressPercentage%)")
            emit(progressPercentage)
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching course progress for $courseId: ${e.message}", e)
            emit(0)
        }
    }.flowOn(Dispatchers.IO)

    fun getLessonsByCourse(courseId: String): Flow<List<Lesson>> = callbackFlow {
        val userAuthId = getCurrentUserId()

        // Send initial empty list to prevent UI waiting
        trySend(emptyList())

        if (userAuthId.isEmpty()) {
            Log.w("CourseViewModel", "User ID is empty, cannot fetch lessons")
            close()
            return@callbackFlow
        }

        // Query all lessons for the course
        val lessonsQuery = firestore.collection("lessons")
            .whereEqualTo("courseId", courseId)

        // Query completed lesson IDs from user's subcollection
        val completedLessonsQuery = firestore.collection("users")
            .document(userAuthId)
            .collection("completedLessons")
            .whereEqualTo("courseId", courseId)

        var lessonsListenerRegistration: ListenerRegistration? = null
        var completedLessonsListenerRegistration: ListenerRegistration? = null

        var lessonsList = listOf<Lesson>()
        var completedLessonIds = setOf<String>()
        var shouldUpdate = true

        // Update and send lessons list
        fun updateAndSendLessons() {
            if (!shouldUpdate) return

            try {
                val updatedLessons = lessonsList.map { lesson ->
                    lesson.copy(isCompleted = completedLessonIds.contains(lesson.lessonId))
                }.sortedBy { it.order }

                trySend(updatedLessons)
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error sending lessons: ${e.message}", e)
            }
        }

        // Listen to lessons
        try {
            lessonsListenerRegistration = lessonsQuery.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CourseViewModel", "Error fetching lessons: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        lessonsList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val lesson = doc.toObject<Lesson>()?.copy(lessonId = doc.id)
                                lesson
                            } catch (e: Exception) {
                                Log.e("CourseViewModel", "Error parsing lesson: ${e.message}", e)
                                null
                            }
                        }

                        Log.d("CourseViewModel", "Fetched ${lessonsList.size} lessons for course $courseId")
                        updateAndSendLessons()
                    } catch (e: Exception) {
                        Log.e("CourseViewModel", "Error processing lessons snapshot: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error setting up lessons listener: ${e.message}", e)
        }

        // Listen to completed lessons
        try {
            completedLessonsListenerRegistration = completedLessonsQuery.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CourseViewModel", "Error fetching completed lessons: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        completedLessonIds = snapshot.documents.mapNotNull { doc ->
                            doc.getString("lessonId")
                        }.toSet()

                        Log.d("CourseViewModel", "Fetched ${completedLessonIds.size} completed lessons for user $userAuthId in course $courseId")
                        updateAndSendLessons()
                    } catch (e: Exception) {
                        Log.e("CourseViewModel", "Error processing completed lessons: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error setting up completed lessons listener: ${e.message}", e)
        }

        awaitClose {
            Log.d("CourseViewModel", "Closing lesson listeners")
            shouldUpdate = false
            lessonsListenerRegistration?.remove()
            completedLessonsListenerRegistration?.remove()
        }
    }.flowOn(Dispatchers.IO).catch { e ->
        Log.e("CourseViewModel", "Error in lessons flow: ${e.message}", e)
        emit(emptyList())
    }

    fun isLessonCompleted(lessonId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (lessonId.isEmpty()) {
                Log.w("CourseViewModel", "isLessonCompleted: lessonId is empty")
                launch(Dispatchers.Main) { callback(false) }
                return@launch
            }

            val userAuthId = getCurrentUserId()
            if (userAuthId.isEmpty()) {
                Log.w("CourseViewModel", "isLessonCompleted: User is not authenticated")
                launch(Dispatchers.Main) { callback(false) }
                return@launch
            }

            try {
                val completedLessonDoc = firestore.collection("users")
                    .document(userAuthId)
                    .collection("completedLessons")
                    .document(lessonId)
                    .get()
                    .await()

                val completed = completedLessonDoc.exists()
                Log.d("CourseViewModel", "isLessonCompleted: Lesson $lessonId completed status: $completed")
                launch(Dispatchers.Main) { callback(completed) }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error checking lesson completion: ${e.message}", e)
                launch(Dispatchers.Main) { callback(false) }
            }
        }
    }

    fun completeLesson(lessonId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val userAuthId = getCurrentUserId()
            Log.d("CourseViewModel", "completeLesson: Called for lessonId: $lessonId, userAuthId: $userAuthId")

            if (userAuthId.isEmpty() || lessonId.isEmpty()) {
                Log.e("CourseViewModel", "completeLesson: Invalid parameters - userAuthId: $userAuthId, lessonId: $lessonId")
                launch(Dispatchers.Main) { onResult(false) }
                return@launch
            }

            try {
                val lessonDoc = firestore.collection("lessons").document(lessonId).get().await()
                val courseId = lessonDoc.getString("courseId") ?: ""

                if (courseId.isEmpty()) {
                    Log.e("CourseViewModel", "completeLesson: CourseId is empty for lesson $lessonId")
                    launch(Dispatchers.Main) { onResult(false) }
                    return@launch
                }

                try {
                    firestore.collection("users")
                        .document(userAuthId)
                        .collection("completedLessons")
                        .document(lessonId)
                        .set(
                            mapOf(
                                "lessonId" to lessonId,
                                "courseId" to courseId,
                                "completedAt" to com.google.firebase.Timestamp.now()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        .await()

                    Log.d("CourseViewModel", "completeLesson: Successfully marked lesson as completed")

                    try {
                        updateSimplifiedCourseProgress(courseId, userAuthId)
                    } catch (e: Exception) {
                        Log.w("CourseViewModel", "completeLesson: Failed to update course progress: ${e.message}", e)
                    }

                    launch(Dispatchers.Main) { onResult(true) }
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "completeLesson: Error updating completedLessons: ${e.message}", e)
                    launch(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "completeLesson: Exception during operation: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    private suspend fun updateSimplifiedCourseProgress(courseId: String, userAuthId: String) {
        try {
            val totalLessons = try {
                val lessonsSnapshot = firestore.collection("lessons")
                    .whereEqualTo("courseId", courseId)
                    .get()
                    .await()
                lessonsSnapshot.size()
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error getting total lessons: ${e.message}", e)
                0
            }

            if (totalLessons == 0) {
                Log.w("CourseViewModel", "updateSimplifiedCourseProgress: No lessons found for course $courseId")
                return
            }

            val completedLessons = try {
                val completedLessonsSnapshot = firestore.collection("users")
                    .document(userAuthId)
                    .collection("completedLessons")
                    .whereEqualTo("courseId", courseId)
                    .get()
                    .await()
                completedLessonsSnapshot.size()
            } catch (e: Exception) {
                Log.e("CourseViewModel", "Error getting completed lessons: ${e.message}", e)
                0
            }

            val progressDocId = "$userAuthId-$courseId"

            firestore.collection("CourseProgress")
                .document(progressDocId)
                .set(
                    mapOf(
                        "courseId" to courseId,
                        "userAuthId" to userAuthId,
                        "completedLessons" to completedLessons,
                        "totalLessons" to totalLessons,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            Log.d("CourseViewModel", "Updated CourseProgress: $completedLessons/$totalLessons lessons for course $courseId and user $userAuthId")
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error updating course progress: ${e.message}", e)
        }
    }

    fun getEnrolledCourses(): Flow<List<EnrolledCourse>> = flow {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            Log.w("CourseViewModel", "Student User ID is empty, cannot fetch enrolled courses")
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
            Log.d("CourseViewModel", "Fetched ${enrolledCourses.size} enrolled courses for studentCustomId: $studentCustomId")
            emit(enrolledCourses)
        } catch (e: Exception) {
            Log.e("CourseViewModel", "Error fetching enrolled courses for student $userId: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun sendTest(courseId: String, studentCustomId: String, tutorCustomId: String, content: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("CourseViewModel", "sendTest: Attempting to send test. Course: $courseId, Student: $studentCustomId, Tutor: $tutorCustomId")
                val testData = Test(
                    courseId = courseId,
                    studentId = studentCustomId,
                    tutorId = tutorCustomId,
                    content = content,
                    createdAt = com.google.firebase.Timestamp.now()
                )
                firestore.collection("tests")
                    .add(testData)
                    .await()
                Log.i("CourseViewModel", "sendTest: Successfully sent test for course $courseId to student $studentCustomId")
                launch(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "sendTest: Error sending test for course $courseId, student $studentCustomId: ${e.message}", e)
                launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun getReceivedTests(): Flow<List<Test>> {
        return _currentTutorCustomId.flatMapLatest { studentCustomId ->
            if (studentCustomId.isNullOrEmpty()) {
                Log.w("CourseViewModel", "getReceivedTests: Current user custom ID is null or empty. Returning empty list")
                flowOf(emptyList())
            } else {
                Log.d("CourseViewModel", "getReceivedTests: Fetching tests for studentCustomId: $studentCustomId")
                firestore.collection("tests")
                    .whereEqualTo("studentId", studentCustomId)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject<Test>()?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e("CourseViewModel", "getReceivedTests: Error parsing Test document ${doc.id}: ${e.message}", e)
                                null
                            }
                        }
                    }
                    .catch { e ->
                        Log.e("CourseViewModel", "getReceivedTests: Error in flow for student $studentCustomId: ${e.message}", e)
                        emit(emptyList())
                    }
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getCourseProgressForStudent(courseId: String, studentCustomId: String): Flow<CourseProgress?> = callbackFlow {
        Log.d("CourseViewModel", "getCourseProgressForStudent: Fetching for course $courseId, student $studentCustomId")

        try {
            // Find student's auth UID
            val studentUserQuery = firestore.collection("users")
                .whereEqualTo("id", studentCustomId)
                .limit(1)
                .get()
                .await()

            if (studentUserQuery.isEmpty) {
                Log.w("CourseViewModel", "getCourseProgressForStudent: Student user not found for ID: $studentCustomId")
                trySend(null)
                close()
                return@callbackFlow
            }

            val studentUserDoc = studentUserQuery.documents.first()
            val userAuthId = studentUserDoc.id // Document ID is the auth UID

            // Query progress record using composite ID
            val progressDocId = "$userAuthId-$courseId"
            val progressListener = firestore.collection("CourseProgress")
                .document(progressDocId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("CourseViewModel", "getCourseProgressForStudent: Error listening: ${e.message}", e)
                        trySend(null)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val progress = snapshot.toObject<CourseProgress>()
                            Log.d("CourseViewModel", "getCourseProgressForStudent: Progress fetched: $progress")
                            trySend(progress)
                        } catch (e: Exception) {
                            Log.e("CourseViewModel", "getCourseProgressForStudent: Error parsing CourseProgress: ${e.message}", e)
                            trySend(null)
                        }
                    } else {
                        Log.d("CourseViewModel", "getCourseProgressForStudent: No progress document found")
                        trySend(null)
                    }
                }

            awaitClose { progressListener.remove() }
        } catch (e: Exception) {
            Log.e("CourseViewModel", "getCourseProgressForStudent: Error: ${e.message}", e)
            trySend(null)
            close(e)
        }
    }.flowOn(Dispatchers.IO)
}