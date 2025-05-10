package com.example.personaltutorapp.di

import android.content.Context
import com.example.personaltutorapp.database.AppDatabase
import com.example.personaltutorapp.database.CourseDao
import com.example.personaltutorapp.database.EnrollmentDao
import com.example.personaltutorapp.database.LessonDao
// 確保沒有 import com.google.firebase.auth.FirebaseAuth 等 Firebase 相關的類別
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideCourseDao(database: AppDatabase): CourseDao = database.courseDao()

    @Provides
    fun provideLessonDao(database: AppDatabase): LessonDao = database.lessonDao()

    @Provides
    fun provideEnrollmentDao(database: AppDatabase): EnrollmentDao = database.enrollmentDao()

    // !!! 重要：確保這裡沒有以下任何 Firebase 相關的 provide 方法 !!!
    // fun provideFirebaseAuth(): FirebaseAuth { ... }  <--- 如果有，請刪除
    // fun provideFirebaseFirestore(): FirebaseFirestore { ... } <--- 如果有，請刪除
    // fun provideFirebaseStorage(): FirebaseStorage { ... } <--- 如果有，請刪除
}