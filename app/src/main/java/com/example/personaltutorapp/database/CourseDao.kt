package com.example.personaltutorapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.personaltutorapp.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert
    suspend fun insertCourse(course: Course): Long

    @Query("SELECT * FROM courses WHERE tutorId = :tutorId")
    fun getCoursesByTutor(tutorId: String): Flow<List<Course>>

    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>
    
    @Query("SELECT * FROM courses WHERE courseId = :courseId")
    suspend fun getCourseById(courseId: Int): Course?
    
    @Query("SELECT * FROM courses WHERE courseId = :courseId")
    fun getCourseByIdFlow(courseId: Int): Flow<Course?>
    
    @Update
    suspend fun updateCourse(course: Course)
    
    @Query("SELECT * FROM courses WHERE courseId IN (:courseIds)")
    fun getCoursesByIds(courseIds: List<Int>): Flow<List<Course>>
}