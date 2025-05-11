package com.example.personaltutorapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.model.Enrollment // ✅ 確保有導入 Enrollment

@Database(
    entities = [Course::class, Lesson::class, Enrollment::class], // ✅ 確保有 Enrollment
    version = 1,
    exportSchema = false // ✅ 避免 Gradle 報錯
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun lessonDao(): LessonDao
    abstract fun enrollmentDao(): EnrollmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_tutor_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
