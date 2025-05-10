package com.example.personaltutorapp.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.model.StudentLessonStatus
import com.example.personaltutorapp.model.User
import com.example.personaltutorapp.model.LessonProgress

@Database(
    entities = [
        Course::class, 
        Lesson::class, 
        Enrollment::class,
        User::class,
        StudentLessonStatus::class,
        LessonProgress::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun lessonDao(): LessonDao
    abstract fun enrollmentDao(): EnrollmentDao
    abstract fun studentLessonStatusDao(): StudentLessonStatusDao
    abstract fun userDao(): UserDao
    abstract fun lessonProgressDao(): LessonProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_tutor_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
