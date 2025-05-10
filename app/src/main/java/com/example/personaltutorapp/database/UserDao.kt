package com.example.personaltutorapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personaltutorapp.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE uid = :userId")
    fun getUserById(userId: String): Flow<User?> // Using Flow for reactive updates

    @Query("SELECT * FROM users WHERE uid = :userId")
    suspend fun getUserByIdOnce(userId: String): User? // Optional: for non-Flow access

    @Query("SELECT * FROM users WHERE isTutor = :isTutor")
    fun getUsersByRole(isTutor: Boolean): Flow<List<User>>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
} 