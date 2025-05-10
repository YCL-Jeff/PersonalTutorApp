package com.example.personaltutorapp

import android.app.Application
import com.example.personaltutorapp.data.SampleDataProvider
import com.example.personaltutorapp.database.UserDao
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PersonalTutorApp : Application() {

    @Inject
    lateinit var sampleDataProvider: SampleDataProvider
    
    @Inject
    lateinit var userDao: UserDao

    private val applicationScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            auth.currentUser?.let { firebaseUser ->
                applicationScope.launch {
                    val user = userDao.getUserById(firebaseUser.uid).firstOrNull()
                    val isTutor = user?.isTutor ?: false
                    sampleDataProvider.insertSampleDataIfNecessary(firebaseUser.uid, isTutor)
                }
            }
        }
    }
}
