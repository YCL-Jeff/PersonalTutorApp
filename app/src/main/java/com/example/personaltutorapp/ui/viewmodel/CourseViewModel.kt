package com.example.personaltutorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.personaltutorapp.database.CourseDao
import com.example.personaltutorapp.database.EnrollmentDao
import com.example.personaltutorapp.database.LessonDao
import com.example.personaltutorapp.database.StudentLessonProgressDao
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.model.StudentLessonProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Date

data class LessonWithProgress(
    val lesson: Lesson,
    val isCompleted: Boolean,
    val isLocked: Boolean
)

data class CourseProgress(
    val completedLessons: Int,
    val totalLessons: Int,
    val percentage: Int
)

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseDao: CourseDao,
    private val lessonDao: LessonDao,
    private val enrollmentDao: EnrollmentDao,
    private val studentLessonProgressDao: StudentLessonProgressDao,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    // 創建課程
    fun createCourse(title: String, description: String, subject: String) {
        viewModelScope.launch {
            val tutorId = auth.currentUser?.uid ?: return@launch
            val course = Course(title = title, description = description, subject = subject, tutorId = tutorId)
            courseDao.insertCourse(course)
            loadCourses(tutorId)
        }
    }

    // 創建固定數量的課程 (每個課程4節課)
    fun createLessonsForCourse(courseId: Int, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // 總是創建4節課
                for (i in 1..4) {
                    val lesson = Lesson(
                        courseId = courseId,
                        title = "Lesson $i",
                        content = "Content for lesson $i",
                        order = i
                    )
                    lessonDao.insertLesson(lesson)
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // 獲取課程中的課節 (帶有學生進度)
    fun getLessonsWithProgress(courseId: Int): Flow<List<LessonWithProgress>> {
        val currentUserId = auth.currentUser?.uid ?: return MutableStateFlow(emptyList())
        
        val lessons = lessonDao.getLessonsByCourse(courseId)
        val studentProgress = studentLessonProgressDao.getStudentProgressForCourse(currentUserId, courseId)
        
        return combine(lessons, studentProgress) { lessonList, progressList ->
            lessonList.map { lesson ->
                val progress = progressList.find { it.lessonId == lesson.lessonId }
                val previousLessonsIncomplete = if (lesson.order > 1) {
                    studentLessonProgressDao.hasPreviousIncompleteLessons(currentUserId, courseId, lesson.order)
                } else false
                
                LessonWithProgress(
                    lesson = lesson,
                    isCompleted = progress?.lessonCompleted ?: false,
                    isLocked = previousLessonsIncomplete
                )
            }
        }
    }

    // 完成課節 (只有滾動到底部才能標記完成)
    fun markLessonAsCompleted(lessonId: Int, scrolledToBottom: Boolean, onResult: (Boolean) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onResult(false)
            return
        }
        
        if (!scrolledToBottom) {
            onResult(false)
            return
        }
        
        viewModelScope.launch {
            try {
                val lesson = lessonDao.getLessonById(lessonId) ?: run {
                    onResult(false)
                    return@launch
                }
                
                // 檢查前面的課程是否都已完成
                val hasPreviousIncomplete = studentLessonProgressDao.hasPreviousIncompleteLessons(
                    currentUserId, lesson.courseId, lesson.order
                )
                
                if (hasPreviousIncomplete) {
                    onResult(false)
                    return@launch
                }
                
                // 更新學生進度
                val progress = StudentLessonProgress(
                    studentId = currentUserId,
                    lessonId = lessonId,
                    lessonCompleted = true,
                    completedTimestamp = Date().time
                )
                
                studentLessonProgressDao.insertOrUpdateProgress(progress)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    // 獲取學生在課程中的進度
    fun getStudentCourseProgress(courseId: Int): Flow<CourseProgress> {
        val currentUserId = auth.currentUser?.uid ?: return MutableStateFlow(CourseProgress(0, 0, 0))
        
        val completedLessons = studentLessonProgressDao.getCompletedLessonsCountForCourse(currentUserId, courseId)
        val totalLessons = studentLessonProgressDao.getTotalLessonsForCourse(courseId)
        
        return combine(completedLessons, totalLessons) { completed, total ->
            val percentage = if (total > 0) (completed * 100) / total else 0
            CourseProgress(completed, total, percentage)
        }
    }

    // 獲取老師所教課程中學生的進度
    fun getStudentProgressInTutorCourse(courseId: Int): Flow<Map<String, CourseProgress>> {
        return enrollmentDao.getEnrolledStudents(courseId).map { enrollments ->
            enrollments.associate { enrollment ->
                val completedCount = studentLessonProgressDao.getCompletedLessonsCountForCourse(
                    enrollment.studentId, courseId
                ).firstOrNull() ?: 0
                
                val totalCount = studentLessonProgressDao.getTotalLessonsForCourse(courseId).firstOrNull() ?: 0
                val percentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
                
                enrollment.studentId to CourseProgress(completedCount, totalCount, percentage)
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

    fun getLessonsByCourse(courseId: Int): Flow<List<Lesson>> {
        return lessonDao.getLessonsByCourse(courseId)
    }

    // 獲取特定學生在特定課程中的課節進度
    fun getStudentLessonProgressForCourse(studentId: String, courseId: Int): Flow<List<StudentLessonProgress>> {
        return studentLessonProgressDao.getStudentProgressForCourse(studentId, courseId)
    }

    private fun loadCourses(tutorId: String) {
        viewModelScope.launch {
            courseDao.getCoursesByTutor(tutorId).collect { courses ->
                _courses.value = courses
            }
        }
    }
}
