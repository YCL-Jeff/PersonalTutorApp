import com.your_app_name.data.dao.CourseDao
import com.your_app_name.data.dao.LessonDao
import com.your_app_name.data.model.Course
import com.your_app_name.data.model.Lesson
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val courseDao: CourseDao, private val lessonDao: LessonDao) {

    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()

    suspend fun insertCourse(course: Course) {
        courseDao.insertCourse(course)
    }

    fun getCourseById(courseId: Int): Flow<Course?> {
        return courseDao.getCourseById(courseId)
    }

    fun getLessonsForCourse(courseId: Int): Flow<List<Lesson>> {
        return lessonDao.getLessonsForCourse(courseId)
    }

    fun getLessonById(lessonId: Int): Flow<Lesson?> {
        return lessonDao.getLessonById(lessonId)
    }

    suspend fun updateLesson(lesson: Lesson) {
        lessonDao.updateLesson(lesson)
    }

    fun getCompletedLessonCountForCourse(courseId: Int): Flow<Int> {
        return lessonDao.getCompletedLessonCountForCourse(courseId)
    }

    suspend fun insertAllLessons(lessons: List<Lesson>) {
        lessonDao.insertAllLessons(lessons)
    }
}