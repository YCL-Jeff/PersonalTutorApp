package com.example.personaltutorapp

import com.example.personaltutorapp.database.CourseDao
import com.example.personaltutorapp.database.EnrollmentDao
import com.example.personaltutorapp.database.LessonDao
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class CourseViewModelTest {

    @Mock
    private lateinit var courseDao: CourseDao

    @Mock
    private lateinit var lessonDao: LessonDao

    @Mock
    private lateinit var enrollmentDao: EnrollmentDao

    @Mock
    private lateinit var auth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var viewModel: CourseViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(auth.currentUser).thenReturn(firebaseUser)
        `when`(firebaseUser.uid).thenReturn("test-user-id")

        viewModel = CourseViewModel(courseDao, lessonDao, enrollmentDao, auth)
    }

    @Test
    fun `test get course progress calculation`() = runBlocking {
        // Arrange
        val courseId = 1
        `when`(lessonDao.getCompletedLessonsCount(courseId)).thenReturn(flowOf(3))
        `when`(lessonDao.getTotalLessonsCount(courseId)).thenReturn(flowOf(10))

        // Act & Assert
        viewModel.getCourseProgress(courseId).collect { progress ->
            assert(progress == 30) // 3 completed out of 10 = 30%
        }
    }

    @Test
    fun `test completion of a lesson`() = runBlocking {
        // Arrange
        val lessonId = 1
        val lesson = Lesson(lessonId = lessonId, courseId = 1, title = "Test Lesson", isCompleted = false)
        `when`(lessonDao.getLessonById(lessonId)).thenReturn(lesson)

        // Act
        var result = false
        viewModel.completeLesson(lessonId) { success ->
            result = success
        }

        // Assert
        verify(lessonDao).updateLessonCompletion(lessonId, true)
        assert(result)
    }

    @Test
    fun `test creating a course with lessons`() = runBlocking {
        // Arrange
        val title = "Test Course"
        val description = "Test Description"
        val subject = "Test Subject"
        `when`(courseDao.insertCourse(any())).thenReturn(1L)

        // Act
        var courseId = 0L
        viewModel.createCourse(title, description, subject) { id ->
            courseId = id
        }

        // Assert
        verify(courseDao).insertCourse(any())
        verify(lessonDao).insertLessons(anyList())
        assert(courseId == 1L)
    }
} 