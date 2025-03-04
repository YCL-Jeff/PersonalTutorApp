package com.example.personaltutorapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.database.AppDatabase
import kotlinx.coroutines.launch

class CourseViewModel(application: Application) : AndroidViewModel(application) {
    private val courseDao = AppDatabase.getDatabase(application).courseDao()

    val allCourses: LiveData<List<Course>> = courseDao.getAllCourses()

    fun addCourse(course: Course) {
        viewModelScope.launch {
            courseDao.insertCourse(course)
        }
    }
}
