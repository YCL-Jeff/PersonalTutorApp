package com.example.personaltutorapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.personaltutorapp.model.Enrollment

@Dao
interface EnrollmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: Enrollment)

    @Query("SELECT * FROM enrollments WHERE courseId = :courseId")
    fun getEnrolledStudents(courseId: Int): Flow<List<Enrollment>>
}
