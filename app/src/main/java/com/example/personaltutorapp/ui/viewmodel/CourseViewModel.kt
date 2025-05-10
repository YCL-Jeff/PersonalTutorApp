package com.example.personaltutorapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.personaltutorapp.database.CourseDao
import com.example.personaltutorapp.database.EnrollmentDao
import com.example.personaltutorapp.database.LessonDao
import com.example.personaltutorapp.database.LessonProgressDao
import com.example.personaltutorapp.database.StudentLessonStatusDao
import com.example.personaltutorapp.database.UserDao
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.model.LessonProgress
import com.example.personaltutorapp.model.StudentLessonStatus
import com.example.personaltutorapp.model.User
import com.example.personaltutorapp.ui.screens.EligibleStudent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseDao: CourseDao,
    private val lessonDao: LessonDao,
    private val enrollmentDao: EnrollmentDao,
    private val studentLessonStatusDao: StudentLessonStatusDao,
    private val userDao: UserDao,
    private val lessonProgressDao: LessonProgressDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _currentLesson = MutableStateFlow<Lesson?>(null)
    val currentLesson: StateFlow<Lesson?> = _currentLesson.asStateFlow()
    
    private val _currentStudentLessonStatus = MutableStateFlow<StudentLessonStatus?>(null)
    val currentStudentLessonStatus: StateFlow<StudentLessonStatus?> = _currentStudentLessonStatus.asStateFlow()

    init {
        auth.currentUser?.uid?.let {
            loadCoursesForCurrentUser(it)
        } ?: getAllCourses()
    }

    private fun loadCoursesForCurrentUser(userId: String) {
        viewModelScope.launch {
            userDao.getUserById(userId).firstOrNull()?.let { user ->
                if (user.isTutor) {
                    courseDao.getCoursesByTutor(userId).collect { _courses.value = it }
                } else {
                    enrollmentDao.getEnrollmentsForStudent(userId).flatMapLatest { enrollments ->
                        val courseIds = enrollments.map { it.courseId }
                        if (courseIds.isEmpty()) flowOf(emptyList()) else courseDao.getCoursesByIds(courseIds)
                    }.collect { _courses.value = it }
                }
            } ?: getAllCourses()
        }
    }

    fun createCourse(title: String, description: String, subject: String, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val tutorId = auth.currentUser?.uid ?: return@launch
            val course = Course(title = title, description = description, subject = subject, tutorId = tutorId)
            val courseId = courseDao.insertCourse(course)
            
            val lessons = (1..10).map { lessonNumber ->
                val lessonTitle = getLessonTitle(subject, lessonNumber)
                val lessonContent = getLessonContent(subject, lessonNumber)
                Lesson(courseId = courseId.toInt(), title = lessonTitle, content = lessonContent, order = lessonNumber)
            }
            lessonDao.insertLessons(lessons)
            
            loadCoursesForCurrentUser(tutorId)
            onComplete(courseId)
        }
    }

    private fun getLessonTitle(subject: String, lessonNumber: Int): String {
        return when (subject.lowercase()) {
            "python" -> when (lessonNumber) {
                1 -> "Introduction to Python"
                2 -> "Variables and Data Types"
                3 -> "Control Flow: If Statements"
                4 -> "Loops in Python"
                5 -> "Functions and Methods"
                6 -> "Lists and Dictionaries"
                7 -> "Object-Oriented Programming"
                8 -> "File I/O Operations"
                9 -> "Error Handling"
                10 -> "Python Libraries and Packages"
                else -> "Lesson $lessonNumber"
            }
            "web development" -> when (lessonNumber) {
                1 -> "HTML Basics"
                2 -> "CSS Fundamentals"
                3 -> "JavaScript Introduction"
                4 -> "DOM Manipulation"
                5 -> "Responsive Design"
                6 -> "Forms and Validation"
                7 -> "API Integration"
                8 -> "Frontend Frameworks"
                9 -> "Web Performance"
                10 -> "Deployment and Hosting"
                else -> "Lesson $lessonNumber"
            }
            "java" -> when (lessonNumber) {
                1 -> "Java Fundamentals"
                2 -> "Object-Oriented Programming"
                3 -> "Classes and Inheritance"
                4 -> "Interfaces and Abstract Classes"
                5 -> "Collections Framework"
                6 -> "Exception Handling"
                7 -> "Multithreading"
                8 -> "Java I/O"
                9 -> "Java Database Connectivity"
                10 -> "Java Web Applications"
                else -> "Lesson $lessonNumber"
            }
            else -> "Lesson $lessonNumber"
        }
    }

    private fun getLessonContent(subject: String, lessonNumber: Int): String {
        return when (subject.lowercase()) {
            "python" -> when (lessonNumber) {
                1 -> """
                    # Introduction to Python
                    
                    Python is a high-level, interpreted programming language known for its readability and simplicity.
                    
                    ## History
                    Python was created by Guido van Rossum and first released in 1991. It was designed with an emphasis on code readability, using significant whitespace and a clean syntax.
                    
                    ## Key Features
                    - Easy to learn and use
                    - Interpreted language (no compilation needed)
                    - Dynamically typed
                    - Extensive standard library
                    - Large community and ecosystem
                    
                    ## Hello World
                    ```python
                    print("Hello, World!")
                    ```
                    
                    This lesson will introduce you to the basics of Python programming and help you set up your development environment.
                """
                2 -> """
                    # Variables and Data Types in Python
                    
                    Python has several built-in data types that are used to define the operations possible on them and the storage method for each of them.
                    
                    ## Basic Data Types
                    - Integers: Whole numbers like 3, 100, -10
                    - Floats: Decimal numbers like 3.14, -0.001
                    - Strings: Text enclosed in quotes like "Hello" or 'World'
                    - Booleans: True or False values
                    
                    ## Variable Assignment
                    ```python
                    # Integer
                    age = 25
                    
                    # Float
                    height = 1.75
                    
                    # String
                    name = "Alice"
                    
                    # Boolean
                    is_student = True
                    ```
                    
                    Variables in Python do not need explicit declaration to reserve memory space. The declaration happens automatically when you assign a value to a variable.
                """
                else -> "Content for Lesson $lessonNumber in Python"
            }
            "web development" -> when (lessonNumber) {
                1 -> """
                    # HTML Basics
                    
                    HTML (HyperText Markup Language) is the standard markup language for documents designed to be displayed in a web browser.
                    
                    ## Structure of an HTML Document
                    ```html
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>My First Web Page</title>
                    </head>
                    <body>
                        <h1>Hello, World!</h1>
                        <p>This is my first web page.</p>
                    </body>
                    </html>
                    ```
                    
                    ## Common HTML Elements
                    - Headings: `<h1>` to `<h6>`
                    - Paragraphs: `<p>`
                    - Links: `<a href="url">link text</a>`
                    - Images: `<img src="image.jpg" alt="description">`
                    - Lists: `<ul>`, `<ol>`, and `<li>`
                    
                    HTML forms the backbone of any web page, providing the structure that CSS styles and JavaScript makes interactive.
                """
                2 -> """
                    # CSS Fundamentals
                    
                    CSS (Cascading Style Sheets) is a style sheet language used for describing the presentation of a document written in HTML.
                    
                    ## How to Add CSS
                    ```html
                    <!-- External CSS -->
                    <link rel="stylesheet" href="styles.css">
                    
                    <!-- Internal CSS -->
                    <style>
                        body {
                            background-color: lightblue;
                        }
                    </style>
                    
                    <!-- Inline CSS -->
                    <p style="color: red;">This is a red paragraph.</p>
                    ```
                    
                    ## Basic Selectors
                    - Element selector: `p { color: red; }`
                    - Class selector: `.intro { font-size: 20px; }`
                    - ID selector: `#header { background-color: black; }`
                    
                    CSS allows you to style your HTML elements and create beautiful, responsive web pages.
                """
                else -> "Content for Lesson $lessonNumber in Web Development"
            }
            "java" -> when (lessonNumber) {
                1 -> """
                    # Java Fundamentals
                    
                    Java is a popular programming language and platform that was created in 1995 by Sun Microsystems, now owned by Oracle.
                    
                    ## Key Features
                    - Platform Independence (Write Once, Run Anywhere)
                    - Object-Oriented
                    - Strongly Typed
                    - Automatic Memory Management
                    
                    ## Java Program Structure
                    ```java
                    public class HelloWorld {
                        public static void main(String[] args) {
                            System.out.println("Hello, World!");
                        }
                    }
                    ```
                    
                    ## Basic Syntax
                    - Java is case-sensitive
                    - Class names start with an uppercase letter
                    - Method names start with a lowercase letter
                    - Source files must have the same name as the public class they contain
                    
                    This lesson will introduce you to the basics of Java programming and help you set up your development environment.
                """
                else -> "Content for Lesson $lessonNumber in Java"
            }
            else -> "Content for Lesson $lessonNumber"
        }
    }

    fun createLesson(courseId: Int, title: String, content: String, order: Int = 0, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val lesson = Lesson(courseId = courseId, title = title, content = content, order = order, isCompleted = false)
                lessonDao.insertLesson(lesson)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun filterCourses(query: String, subject: String): Flow<List<Course>> {
        return courseDao.getAllCourses().map { courses ->
            when {
                query.isNotEmpty() && subject.isNotEmpty() -> courses.filter { it.title.contains(query, ignoreCase = true) && it.subject == subject }
                query.isNotEmpty() -> courses.filter { it.title.contains(query, ignoreCase = true) }
                subject.isNotEmpty() -> courses.filter { it.subject == subject }
                else -> courses
            }
        }
    }

    fun getEnrolledStudents(courseId: Int): Flow<List<Enrollment>> {
        return enrollmentDao.getEnrolledStudents(courseId)
    }

    fun getCourseProgress(courseId: Int): Flow<Int> {
        return combine(
            lessonDao.getCompletedLessonsCount(courseId),
            lessonDao.getTotalLessonsCount(courseId)
        ) { completed, total ->
            if (total > 0) {
                (completed * 100) / total
            } else {
                0
            }
        }
    }

    fun getLessonsByCourse(courseId: Int): Flow<List<Lesson>> {
        return lessonDao.getLessonsByCourse(courseId)
    }

    fun getLesson(lessonId: Int, studentId: String?) {
        viewModelScope.launch {
            _currentLesson.value = lessonDao.getLessonById(lessonId)
            studentId?.let {
                _currentStudentLessonStatus.value = studentLessonStatusDao.getStatus(it, lessonId)
            }
        }
    }

    fun completeLesson(studentId: String, lessonId: Int, courseId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. 创建或更新学生课程状态
                val status = StudentLessonStatus(
                    studentId = studentId,
                    lessonId = lessonId,
                    courseId = courseId,
                    isCompleted = true
                )
                
                // 2. 保存到本地Room数据库
                studentLessonStatusDao.insertOrUpdate(status)
                
                // 3. 更新当前状态，这样UI会立即反应变化
                _currentStudentLessonStatus.value = status
                
                // 4. 获取课程和用户信息
                val course = courseDao.getCourseByIdFlow(courseId).firstOrNull()
                val courseName = course?.title ?: "未知课程"
                val user = userDao.getUserById(studentId).firstOrNull()
                val studentName = user?.name ?: "Unknown Student"
                val studentEmail = user?.email ?: "unknown@example.com"
                
                // 5. 首先检查Firebase中是否已有进度记录
                try {
                    Log.d("CourseViewModel", "正在检查Firebase LessonProgress集合中的进度数据...")
                    val document = firestore.collection("LessonProgress")
                        .document(studentEmail)
                        .get()
                        .await()
                    
                    // 获取当前进度，如果不存在则默认为0
                    val currentProgress = if (document.exists()) {
                        val progress = document.getLong("Progress")?.toInt() ?: 0
                        Log.d("CourseViewModel", "从Firebase读取到的当前进度: $progress")
                        progress
                    } else {
                        Log.d("CourseViewModel", "Firebase中不存在进度记录，设置初始进度为0")
                        0
                    }
                    
                    // 增加10%进度，最大不超过100%
                    val newProgress = (currentProgress + 10).coerceAtMost(100)
                    Log.d("CourseViewModel", "新进度计算: $currentProgress + 10 = $newProgress")
                    
                    // 更新Firestore中的LessonProgress集合
                    Log.d("CourseViewModel", "正在更新Firestore中的LessonProgress集合...")
                    firestore.collection("LessonProgress")
                        .document(studentEmail)
                        .set(mapOf(
                            "Name" to studentName,
                            "Studentemail" to studentEmail,
                            "CourseType" to courseName,
                            "Progress" to newProgress
                        ))
                        .await()
                    
                    Log.d("CourseViewModel", "Firebase LessonProgress更新成功: 学生=$studentEmail, 课程=$courseName, 进度=$newProgress%")
                    
                    // 6. 更新Room数据库中的LessonProgress
                    val lessonProgress = LessonProgress(
                        name = studentName,
                        studentEmail = studentEmail,
                        courseType = courseName,
                        progress = newProgress
                    )
                    lessonProgressDao.insertOrUpdate(lessonProgress)
                    
                    // 7. 将数据保存到其他Firebase集合 (保持原有功能)
                    // 保存完成的课程状态
                    firestore.collection("studentProgress")
                        .document(studentId)
                        .collection("courses")
                        .document(courseId.toString())
                        .collection("lessons")
                        .document(lessonId.toString())
                        .set(mapOf(
                            "lessonId" to lessonId,
                            "courseId" to courseId,
                            "courseName" to courseName,
                            "completed" to true,
                            "completedAt" to System.currentTimeMillis(),
                            "progress" to newProgress
                        ), SetOptions.merge())
                        .await()
                    
                    // 更新课程总进度
                    firestore.collection("studentProgress")
                        .document(studentId)
                        .collection("courses")
                        .document(courseId.toString())
                        .set(mapOf(
                            "courseId" to courseId,
                            "courseName" to courseName,
                            "progress" to newProgress,
                            "lastUpdated" to System.currentTimeMillis()
                        ), SetOptions.merge())
                        .await()
                    
                    // 8. 日志记录完成情况
                    Log.d("CourseViewModel", "Lesson $lessonId completed for student $studentId in course $courseId. Progress: $newProgress%")
                    
                    // 9. 通知调用者成功完成
                    onResult(true)
                } catch (e: Exception) {
                    Log.e("CourseViewModel", "Error updating Firebase: ${e.message}", e)
                    onResult(false)
                }
            } catch (e: Exception) {
                // 错误处理
                Log.e("CourseViewModel", "Error completing lesson: ${e.message}", e)
                onResult(false)
            }
        }
    }

    fun getAllCourses() {
        viewModelScope.launch {
            courseDao.getAllCourses().collect { _courses.value = it }
        }
    }
    
    fun getCourseWithLessons(courseId: Int): Flow<Pair<Course?, List<Lesson>>> {
        val courseFlow = courseDao.getCourseByIdFlow(courseId)
        val lessonsFlow = lessonDao.getLessonsByCourse(courseId)
        
        return courseFlow.combine(lessonsFlow) { course, lessons ->
            Pair(course, lessons)
        }
    }

    fun getStudentProgressInCourse(studentId: String, courseId: Int): Flow<Int> {
        return combine(
            studentLessonStatusDao.getCompletedLessonsCountForStudentInCourse(studentId, courseId),
            lessonDao.getTotalLessonsCount(courseId)
        ) { completed, total ->
            if (total > 0) (completed * 100) / total else 0
        }
    }

    fun getEnrolledStudentsWithProgress(courseId: Int): Flow<Map<User, Int>> {
        return enrollmentDao.getEnrolledStudents(courseId).flatMapLatest { enrollments ->
            if (enrollments.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val studentProgressFlows: List<Flow<Pair<User, Int>>> = enrollments.map { enrollment ->
                    userDao.getUserById(enrollment.studentId).flatMapLatest { user ->
                        if (user == null) flowOf(Pair(User(uid = enrollment.studentId, name = "Unknown Student"), 0))
                        else getStudentProgressInCourse(enrollment.studentId, courseId).map { progress ->
                            Pair(user, progress)
                        }
                    }
                }
                combine(studentProgressFlows) { studentProgressArray ->
                    studentProgressArray.toMap()
                }
            }
        }
    }

    fun getCoursesByIds(courseIds: List<Int>): Flow<List<Course>> {
        return if (courseIds.isEmpty()) flowOf(emptyList()) else courseDao.getCoursesByIds(courseIds)
    }
    
    fun getCourseByIdFlow(courseId: Int): Flow<Course?> {
        return courseDao.getCourseByIdFlow(courseId)
    }
    
    fun getLessonStatusesForStudent(studentId: String, courseId: Int): Flow<List<StudentLessonStatus>> {
        return studentLessonStatusDao.getAllLessonStatusesForStudentInCourse(studentId, courseId)
    }
    
    fun logout(onComplete: () -> Unit) {
        auth.signOut()
        onComplete()
    }

    // 从LessonProgress获取学生课程进度
    fun getCourseProgressFromLessonProgress(studentEmail: String, courseType: String): Flow<Int> {
        // 首先从Room数据库获取进度
        val roomProgressFlow = lessonProgressDao.getProgressValue(studentEmail, courseType).map { progress ->
            progress ?: 0
        }
        
        // 同时尝试从Firebase直接获取最新进度
        viewModelScope.launch {
            try {
                Log.d("CourseViewModel", "正在从Firebase LessonProgress集合获取最新进度...")
                val document = firestore.collection("LessonProgress")
                    .document(studentEmail)
                    .get()
                    .await()
                
                if (document.exists()) {
                    val firebaseProgress = document.getLong("Progress")?.toInt() ?: 0
                    Log.d("CourseViewModel", "Firebase中的进度: $firebaseProgress")
                    
                    // 更新Room数据库中的记录，确保本地数据与Firebase同步
                    if (firebaseProgress > 0) {
                        val lessonProgress = LessonProgress(
                            name = document.getString("Name") ?: "Unknown",
                            studentEmail = studentEmail,
                            courseType = courseType,
                            progress = firebaseProgress
                        )
                        lessonProgressDao.insertOrUpdate(lessonProgress)
                        Log.d("CourseViewModel", "已将Firebase进度数据同步至本地数据库")
                    }
                } else {
                    Log.d("CourseViewModel", "Firebase中不存在该学生的进度记录")
                }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "从Firebase获取进度出错: ${e.message}", e)
            }
        }
        
        return roomProgressFlow
    }
    
    // 获取某课程的所有学生进度
    fun getAllStudentProgressForCourse(courseType: String): Flow<List<LessonProgress>> {
        return lessonProgressDao.getAllProgressForCourse(courseType)
    }
    
    // 判断根据进度是否解锁某个课时
    fun isLessonUnlocked(lessonOrder: Int, progress: Int): Boolean {
        // 第一节课默认解锁
        if (lessonOrder == 1) return true
        
        // 其他课时根据进度判断
        // 要求：Lesson2需要Progress为10%, Lesson3需要Progress为20%...
        val requiredProgress = (lessonOrder - 1) * 10
        
        Log.d("CourseViewModel", "解锁检查: Lesson $lessonOrder 需要进度 $requiredProgress%, 当前进度: $progress%")
        return progress >= requiredProgress
    }

    // 从LessonProgress获取所有学生的课程进度（添加实时监听）
    fun getStudentProgressesFromLessonProgress(courseType: String): Flow<Map<String, Int>> {
        val result = MutableStateFlow<Map<String, Int>>(emptyMap())
        
        viewModelScope.launch {
            try {
                // 设置实时监听
                Log.d("CourseViewModel", "设置 $courseType 课程的学生进度实时监听")
                val listenerRegistration = firestore.collection("LessonProgress")
                    .whereEqualTo("CourseType", courseType)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("CourseViewModel", "监听LessonProgress失败: ${error.message}", error)
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null) {
                            val progressMap = mutableMapOf<String, Int>()
                            
                            for (document in snapshot.documents) {
                                val studentEmail = document.getString("Studentemail") ?: continue
                                val studentName = document.getString("Name") ?: "Unknown"
                                val progress = document.getLong("Progress")?.toInt() ?: 0
                                
                                // 保存到结果Map中
                                progressMap[studentName] = progress
                                
                                // 同步到本地数据库
                                viewModelScope.launch {
                                    val lessonProgress = LessonProgress(
                                        name = studentName,
                                        studentEmail = studentEmail,
                                        courseType = courseType,
                                        progress = progress
                                    )
                                    lessonProgressDao.insertOrUpdate(lessonProgress)
                                }
                            }
                            
                            // 更新结果Flow
                            result.value = progressMap
                            Log.d("CourseViewModel", "实时更新: $courseType 课程有 ${progressMap.size} 个学生: $progressMap")
                        }
                    }
                
                // 注册一个清理函数，当ViewModel被清理时移除监听器
                addCloseable {
                    Log.d("CourseViewModel", "移除 $courseType 课程的学生进度监听")
                    listenerRegistration.remove()
                }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "设置课程进度监听失败: ${e.message}", e)
            }
        }
        
        return result
    }
    
    // 用于管理可关闭资源的辅助方法
    private val closeables = mutableListOf<Closeable>()
    
    private fun interface Closeable {
        fun close()
    }
    
    private fun addCloseable(closeable: Closeable) {
        closeables.add(closeable)
    }
    
    override fun onCleared() {
        super.onCleared()
        closeables.forEach { it.close() }
        closeables.clear()
    }

    // 直接从Firestore获取指定课程的所有学生进度
    fun getStudentProgressesDirectlyFromFirestore(courseType: String, onResult: (Map<String, Int>) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("CourseViewModel", "直接从Firestore获取 $courseType 课程的学生进度数据")
                val snapshot = firestore.collection("LessonProgress")
                    .whereEqualTo("CourseType", courseType)
                    .get()
                    .await()
                
                val progressMap = mutableMapOf<String, Int>()
                
                for (document in snapshot.documents) {
                    val studentName = document.getString("Name") ?: continue
                    val progress = document.getLong("Progress")?.toInt() ?: 0
                    progressMap[studentName] = progress
                    
                    Log.d("CourseViewModel", "Firestore中 $courseType 课程: 学生=$studentName, 进度=$progress")
                }
                
                Log.d("CourseViewModel", "Firestore中 $courseType 课程共有 ${progressMap.size} 个学生进度记录")
                onResult(progressMap)
            } catch (e: Exception) {
                Log.e("CourseViewModel", "直接获取Firestore数据失败: ${e.message}", e)
                onResult(emptyMap())
            }
        }
    }

    // 获取所有课程进度为100%的学生（用于创建测试）
    fun getStudentsWithFullProgress(onResult: (List<EligibleStudent>) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("CourseViewModel", "正在查询所有进度为100%的学生 (Firestore query)")
                
                // 确保使用正确的字段名，与Firebase匹配
                val snapshot = firestore.collection("LessonProgress")
                    .whereEqualTo("Progress", 100) // 字段名与Firebase匹配
                    .get()
                    .await()
                
                val eligibleStudents = mutableListOf<EligibleStudent>()
                
                Log.d("CourseViewModel", "获取到 ${snapshot.documents.size} 条进度为100%的LessonProgress记录")
                
                for (document in snapshot.documents) {
                    // 打印每个文档的内容用于调试
                    val documentData = document.data
                    Log.d("CourseViewModel", "文档ID: ${document.id}, 数据: $documentData")
                    
                    // 使用与Firebase匹配的字段名
                    val studentName = document.getString("Name")
                    val studentEmail = document.getString("Studentemail")
                    val courseType = document.getString("CourseType")
                    val progress = document.getLong("Progress")?.toInt() ?: 0
                    
                    Log.d("CourseViewModel", "解析数据: 姓名=$studentName, 邮箱=$studentEmail, 课程=$courseType, 进度=$progress")
                    
                    // 即使字段都为null，也添加到列表，以便调试
                    if (studentName != null || studentEmail != null || courseType != null) {
                        val student = EligibleStudent(
                            name = studentName ?: "未知姓名",
                            email = studentEmail ?: "未知邮箱",
                            courseType = courseType ?: "未知课程"
                        )
                        
                        eligibleStudents.add(student)
                        Log.d("CourseViewModel", "添加符合条件的学生: $studentName, 课程: $courseType, 进度: $progress%")
                    } else {
                        Log.w("CourseViewModel", "文档 (ID: ${document.id}) 数据异常: 所有关键字段均为null")
                    }
                }
                
                // 如果没有找到学生，尝试不使用过滤器查询所有记录
                if (eligibleStudents.isEmpty()) {
                    Log.w("CourseViewModel", "没有找到进度为100%的学生，尝试查询所有LessonProgress记录进行调试")
                    val allRecords = firestore.collection("LessonProgress")
                        .get()
                        .await()
                    
                    Log.d("CourseViewModel", "总共有 ${allRecords.documents.size} 条LessonProgress记录")
                    for (doc in allRecords.documents) {
                        Log.d("CourseViewModel", "文档ID: ${doc.id}, 数据: ${doc.data}, Progress值: ${doc.getLong("Progress")}")
                    }
                }
                
                Log.d("CourseViewModel", "共找到 ${eligibleStudents.size} 个符合条件可以创建测试的学生")
                onResult(eligibleStudents)
            } catch (e: Exception) {
                Log.e("CourseViewModel", "查询完成课程的学生时出错: ${e.message}", e)
                onResult(emptyList())
            }
        }
    }

    // 专门检查特定学生的课程完成情况（用于调试）
    fun checkSpecificStudentProgress(studentEmail: String, onResult: (EligibleStudent?) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("CourseViewModel", "正在查询特定学生: $studentEmail 的课程进度")
                
                val document = firestore.collection("LessonProgress")
                    .document(studentEmail)
                    .get()
                    .await()
                
                if (document.exists()) {
                    val studentName = document.getString("Name")
                    val courseType = document.getString("CourseType")
                    val progress = document.getLong("Progress")?.toInt() ?: 0
                    
                    Log.d("CourseViewModel", "找到学生数据: 姓名=$studentName, 课程=$courseType, 进度=$progress")
                    
                    if (studentName != null && courseType != null) {
                        val student = EligibleStudent(
                            name = studentName,
                            email = studentEmail,
                            courseType = courseType
                        )
                        onResult(student)
                    } else {
                        Log.w("CourseViewModel", "学生数据字段不完整: Name=$studentName, CourseType=$courseType")
                        onResult(null)
                    }
                } else {
                    Log.w("CourseViewModel", "未找到学生数据: $studentEmail")
                    onResult(null)
                }
            } catch (e: Exception) {
                Log.e("CourseViewModel", "查询特定学生数据出错: ${e.message}", e)
                onResult(null)
            }
        }
    }
}