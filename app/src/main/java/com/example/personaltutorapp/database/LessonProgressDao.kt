package com.example.personaltutorapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personaltutorapp.model.LessonProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: LessonProgress): Long

    @Update
    suspend fun updateProgress(progress: LessonProgress)

    @Query("SELECT * FROM lesson_progress WHERE studentEmail = :email")
    fun getProgressForStudent(email: String): Flow<List<LessonProgress>>

    @Query("SELECT * FROM lesson_progress WHERE studentEmail = :email AND courseType = :courseType")
    fun getStudentProgressForCourse(email: String, courseType: String): Flow<LessonProgress?>

    @Query("SELECT progress FROM lesson_progress WHERE studentEmail = :email AND courseType = :courseType")
    fun getProgressValue(email: String, courseType: String): Flow<Int?>

    @Query("SELECT * FROM lesson_progress WHERE courseType = :courseType")
    fun getAllProgressForCourse(courseType: String): Flow<List<LessonProgress>>
} 