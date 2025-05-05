package com.example.personaltutorapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.model.StudentLessonProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY `order` ASC")
    fun getLessonsByCourse(courseId: Int): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE lessonId = :lessonId")
    suspend fun getLessonById(lessonId: Int): Lesson?

    @Query("UPDATE lessons SET isCompleted = :isCompleted WHERE lessonId = :lessonId")
    suspend fun updateLessonCompletion(lessonId: Int, isCompleted: Boolean)
    
    @Insert
    suspend fun insertLesson(lesson: Lesson)
}

@Dao
interface StudentLessonProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: StudentLessonProgress)
    
    @Query("SELECT * FROM student_lesson_progress WHERE studentId = :studentId AND lessonId = :lessonId")
    suspend fun getStudentLessonProgress(studentId: String, lessonId: Int): StudentLessonProgress?
    
    @Query("SELECT * FROM student_lesson_progress WHERE studentId = :studentId")
    fun getAllStudentProgress(studentId: String): Flow<List<StudentLessonProgress>>
    
    @Query("SELECT COUNT(*) FROM student_lesson_progress WHERE studentId = :studentId AND lessonId IN (SELECT lessonId FROM lessons WHERE courseId = :courseId) AND lessonCompleted = 1")
    fun getCompletedLessonsCountForCourse(studentId: String, courseId: Int): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM lessons WHERE courseId = :courseId")
    fun getTotalLessonsForCourse(courseId: Int): Flow<Int>
    
    @Query("""
        SELECT slp.* FROM student_lesson_progress slp
        JOIN lessons l ON slp.lessonId = l.lessonId
        WHERE l.courseId = :courseId AND slp.studentId = :studentId
        ORDER BY l.`order` ASC
    """)
    fun getStudentProgressForCourse(studentId: String, courseId: Int): Flow<List<StudentLessonProgress>>
    
    @Query("""
        SELECT EXISTS (
            SELECT 1 FROM student_lesson_progress slp
            JOIN lessons l ON slp.lessonId = l.lessonId
            WHERE l.courseId = :courseId AND l.`order` < :currentOrder
            AND slp.studentId = :studentId AND slp.lessonCompleted = 0
        )
    """)
    suspend fun hasPreviousIncompleteLessons(studentId: String, courseId: Int, currentOrder: Int): Boolean
}
