package com.example.personaltutorapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.personaltutorapp.model.Lesson
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
    suspend fun insertLesson(lesson: Lesson): Long
    
    @Insert
    suspend fun insertLessons(lessons: List<Lesson>)
    
    @Update
    suspend fun updateLesson(lesson: Lesson)
    
    @Query("SELECT COUNT(*) FROM lessons WHERE courseId = :courseId AND isCompleted = 1")
    fun getCompletedLessonsCount(courseId: Int): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM lessons WHERE courseId = :courseId")
    fun getTotalLessonsCount(courseId: Int): Flow<Int>
}