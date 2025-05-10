package com.example.personaltutorapp.di

import android.content.Context
import com.example.personaltutorapp.database.AppDatabase
import com.example.personaltutorapp.database.CourseDao
import com.example.personaltutorapp.database.EnrollmentDao
import com.example.personaltutorapp.database.LessonDao
import com.example.personaltutorapp.database.LessonProgressDao
import com.example.personaltutorapp.database.StudentLessonStatusDao
import com.example.personaltutorapp.database.UserDao
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
    
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
    
    @Provides
    fun provideStudentLessonStatusDao(database: AppDatabase): StudentLessonStatusDao = database.studentLessonStatusDao()
    
    @Provides
    fun provideLessonProgressDao(database: AppDatabase): LessonProgressDao = database.lessonProgressDao()
}