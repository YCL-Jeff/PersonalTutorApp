package com.example.personaltutorapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.personaltutorapp.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert
    suspend fun insertCourse(course: Course)

    @Query("SELECT * FROM courses WHERE tutorId = :tutorId")
    fun getCoursesByTutor(tutorId: String): Flow<List<Course>>

    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>
}