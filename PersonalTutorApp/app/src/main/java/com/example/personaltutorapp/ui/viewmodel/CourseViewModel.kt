package com.example.personaltutorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import com.example.personaltutorapp.database.CourseDao
import com.example.personaltutorapp.database.EnrollmentDao
import com.example.personaltutorapp.database.LessonDao
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.model.Lesson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseDao: CourseDao,
    private val lessonDao: LessonDao,
    private val enrollmentDao: EnrollmentDao,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    fun createCourse(title: String, description: String, subject: String) {
        viewModelScope.launch {
            val tutorId = auth.currentUser?.uid ?: return@launch
            val course = Course(title = title, description = description, subject = subject, tutorId = tutorId)
            courseDao.insertCourse(course)
            loadCourses(tutorId)
        }
    }

    fun createLesson(courseId: Int, title: String, content: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val lesson = Lesson(courseId = courseId, title = title, content = content, isCompleted = false)
                lessonDao.insertLesson(lesson)
                onComplete(true) // 成功
            } catch (e: Exception) {
                onComplete(false) // 失敗
            }
        }
    }


    fun filterCourses(query: String, subject: String): Flow<List<Course>> {
        return courseDao.getAllCourses().map { courses ->
            when {
                query.isNotEmpty() && subject.isNotEmpty() -> courses.filter { it.title.contains(query, ignoreCase = true) && it.subject == subject }
                query.isNotEmpty() -> courses.filter { it.title.contains(query, ignoreCase = true) }
                subject.isNotEmpty() -> courses.filter { it.subject == subject }
                else -> courses
            }
        }
    }

    fun getEnrolledStudents(courseId: Int): Flow<List<Enrollment>> {
        return enrollmentDao.getEnrolledStudents(courseId)
    }

    fun getCourseProgress(courseId: Int): Flow<Map<String, Int>> {
        return enrollmentDao.getEnrolledStudents(courseId).map { enrollments ->
            val progress = mutableMapOf<String, Int>()
            enrollments.forEach { enrollment ->
                val studentProgress = lessonDao.getLessonsByCourse(courseId)
                    .map { lessons -> lessons.count { it.isCompleted && it.courseId == courseId } }
                    .firstOrNull() ?: 0
                progress[enrollment.studentId] = studentProgress
            }
            progress
        }
    }

    fun getLessonsByCourse(courseId: Int): Flow<List<Lesson>> {
        return lessonDao.getLessonsByCourse(courseId)
    }

    fun completeLesson(lessonId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val lesson = lessonDao.getLessonById(lessonId)
                if (lesson != null) {
                    lessonDao.updateLessonCompletion(lessonId, !lesson.isCompleted)
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    private fun loadCourses(tutorId: String) {
        viewModelScope.launch {
            courseDao.getCoursesByTutor(tutorId).collect { courses ->
                _courses.value = courses
            }
        }
    }
}