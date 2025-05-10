package com.example.personaltutorapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personaltutorapp.model.StudentLessonStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentLessonStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(status: StudentLessonStatus)

    @Query("SELECT * FROM student_lesson_status WHERE studentId = :studentId AND lessonId = :lessonId")
    suspend fun getStatus(studentId: String, lessonId: Int): StudentLessonStatus?

    @Query("SELECT COUNT(*) FROM student_lesson_status WHERE studentId = :studentId AND courseId = :courseId AND isCompleted = 1")
    fun getCompletedLessonsCountForStudentInCourse(studentId: String, courseId: Int): Flow<Int>
    
    @Query("SELECT * FROM student_lesson_status WHERE studentId = :studentId AND courseId = :courseId AND isCompleted = 1")
    fun getCompletedLessonStatusesForStudentInCourse(studentId: String, courseId: Int): Flow<List<StudentLessonStatus>>

    // You might need a method to get all statuses for a student in a course if you need to display individual lesson statuses
    @Query("SELECT * FROM student_lesson_status WHERE studentId = :studentId AND courseId = :courseId")
    fun getAllLessonStatusesForStudentInCourse(studentId: String, courseId: Int): Flow<List<StudentLessonStatus>>
} 