package com.example.personaltutorapp.di // 1. 確認套件名稱與檔案路徑一致

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage // 2. 確保導入 FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module // 3. 確保有 @Module 註解
@InstallIn(SingletonComponent::class) // 4. 確保有 @InstallIn 註解
object FirebaseModule { // 5. 確保是 object FirebaseModule

    @Provides // 6. 確保有 @Provides 註解
    @Singleton // 7. 確保有 @Singleton 註解
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides // 8. 確保有 @Provides 註解
    @Singleton // 9. 確保有 @Singleton 註解
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides // 10. 確保有 @Provides 註解
    @Singleton // 11. 確保有 @Singleton 註解
    fun provideFirebaseStorage(): FirebaseStorage { // 12. 這是提供 FirebaseStorage 的關鍵方法
        return FirebaseStorage.getInstance()
    }
}
