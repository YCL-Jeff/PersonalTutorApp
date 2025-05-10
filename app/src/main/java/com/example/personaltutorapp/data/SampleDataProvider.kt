package com.example.personaltutorapp.data

import com.example.personaltutorapp.database.CourseDao
import com.example.personaltutorapp.database.EnrollmentDao
import com.example.personaltutorapp.database.LessonDao
import com.example.personaltutorapp.database.StudentLessonStatusDao
import com.example.personaltutorapp.database.UserDao
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.model.StudentLessonStatus
import com.example.personaltutorapp.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SampleDataProvider @Inject constructor(
    private val courseDao: CourseDao,
    private val lessonDao: LessonDao,
    private val enrollmentDao: EnrollmentDao,
    private val studentLessonStatusDao: StudentLessonStatusDao,
    private val userDao: UserDao
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun insertSampleDataIfNecessary(currentFirebaseUserId: String, isCurrentUserTutor: Boolean) {
        coroutineScope.launch {
            // Check if this specific tutor already has courses, if so, assume data is fine.
            if (isCurrentUserTutor) {
                val tutorCourses = courseDao.getCoursesByTutor(currentFirebaseUserId).first()
                if (tutorCourses.isNotEmpty()) return@launch
            }
            // If it's a student, or a tutor with no courses, check for general data presence
            val anyCourse = courseDao.getAllCourses().first()
            if (anyCourse.isNotEmpty() && !isCurrentUserTutor) { // If student and courses exist, don't re-seed.
                 // For students, we might still want to ensure they are enrolled if they are new.
                 // This part can be enhanced later for student-specific sample enrollment.
                 return@launch
            }
            if(anyCourse.isNotEmpty() && isCurrentUserTutor && courseDao.getCoursesByTutor(currentFirebaseUserId).first().isNotEmpty()){ // Tutor has courses
                return@launch
            }

            // --- Create Sample Users (Students) ---
            val studentMary = User(uid = "student_mary_uid", email = "mary@example.com", name = "Mary Jane", isTutor = false)
            val studentTom = User(uid = "student_tom_uid", email = "tom@example.com", name = "Tom Thumb", isTutor = false)
            val studentMingLi = User(uid = "student_mingli_uid", email = "mingli@example.com", name = "Ming Li", isTutor = false)
            userDao.insertUser(studentMary) // Assuming UserDao has insertUser
            userDao.insertUser(studentTom)
            userDao.insertUser(studentMingLi)

            // --- Create Sample Courses (associated with the current logged-in tutor if they are one) ---
            val tutorIdForSampleCourses = if (isCurrentUserTutor) currentFirebaseUserId else "sample_tutor_uid" // Fallback tutorId if current user isn't a tutor
            
            // Ensure a sample tutor user exists if we are using a fallback tutorId
            if (tutorIdForSampleCourses == "sample_tutor_uid") {
                val sampleTutor = userDao.getUserById("sample_tutor_uid").first()
                if (sampleTutor == null) {
                    userDao.insertUser(User(uid = "sample_tutor_uid", email = "tutor@example.com", name = "Sample Tutor", isTutor = true))
                }
            }

            val coursesToCreate = listOf(
                Course(title = "Introduction to Programming with Python", description = "Python basics", subject = "CS", tutorId = tutorIdForSampleCourses),
                Course(title = "Web Development Fundamentals", description = "HTML, CSS, JS", subject = "WebDev", tutorId = tutorIdForSampleCourses),
                Course(title = "Java Programming for Beginners", description = "Java fundamentals", subject = "CS", tutorId = tutorIdForSampleCourses)
            )

            val createdCourseIds = mutableMapOf<String, Int>()

            coursesToCreate.forEach { course ->
                val courseId = courseDao.insertCourse(course).toInt()
                createdCourseIds[course.title] = courseId
                val lessons = generateLessonsList(courseId, course.title)
                lessonDao.insertLessons(lessons)
            }

            // --- Enroll Students and Set Progress ---
            val pythonCourseId = createdCourseIds["Introduction to Programming with Python"]
            val javaCourseId = createdCourseIds["Java Programming for Beginners"]

            if (pythonCourseId != null) {
                enrollmentDao.insertEnrollment(Enrollment(studentId = studentMary.uid, courseId = pythonCourseId))
                val pythonLessons = lessonDao.getLessonsByCourse(pythonCourseId).first()
                if (pythonLessons.isNotEmpty()) { // Mary completes 1 Python lesson (10%)
                    studentLessonStatusDao.insertOrUpdate(StudentLessonStatus(studentId = studentMary.uid, lessonId = pythonLessons[0].lessonId, courseId = pythonCourseId, isCompleted = true))
                }
            }

            if (javaCourseId != null) {
                enrollmentDao.insertEnrollment(Enrollment(studentId = studentTom.uid, courseId = javaCourseId))
                val javaLessons = lessonDao.getLessonsByCourse(javaCourseId).first()
                if (javaLessons.size >= 2) { // Tom completes 2 Java lessons (20%)
                    studentLessonStatusDao.insertOrUpdate(StudentLessonStatus(studentId = studentTom.uid, lessonId = javaLessons[0].lessonId, courseId = javaCourseId, isCompleted = true))
                    studentLessonStatusDao.insertOrUpdate(StudentLessonStatus(studentId = studentTom.uid, lessonId = javaLessons[1].lessonId, courseId = javaCourseId, isCompleted = true))
                }
                // Enroll MingLi in Java course with no progress yet
                enrollmentDao.insertEnrollment(Enrollment(studentId = studentMingLi.uid, courseId = javaCourseId))
            }
        }
    }

    private fun generateLessonsList(courseId: Int, courseTitle: String): List<Lesson> {
        return when (courseTitle) {
            "Introduction to Programming with Python" -> generatePythonLessons(courseId)
            "Web Development Fundamentals" -> generateWebDevLessons(courseId)
            "Java Programming for Beginners" -> generateJavaLessons(courseId)
            else -> emptyList()
        }
    }

    private fun generatePythonLessons(courseId: Int): List<Lesson> {
        return listOf(
            Lesson(courseId = courseId, title = "Introduction to Python", content = "Python is a high-level, interpreted programming language known for its readability and simplicity. In this lesson, we'll explore the basics of Python and why it's a great language for beginners. We'll cover Python's history, its applications in various fields like web development, data science, and automation, and set up our development environment.", order = 1),
            Lesson(courseId = courseId, title = "Variables and Data Types", content = "In this lesson, we'll learn about variables and the basic data types in Python including: integers, floats, strings, booleans, lists, tuples, and dictionaries. We'll practice declaring variables, type conversion, and basic operations on different data types.", order = 2),
            Lesson(courseId = courseId, title = "Control Flow: Conditionals", content = "This lesson covers control flow in Python using conditional statements. We'll learn about if, elif, and else statements, comparison operators, logical operators (and, or, not), and nested conditionals. Through practical examples, you'll understand how to make decisions in your code.", order = 3),
            Lesson(courseId = courseId, title = "Loops in Python", content = "Learn how to use loops in Python to automate repetitive tasks. We'll cover for loops for iterating over sequences, while loops for condition-based iteration, nested loops, and loop control statements like break and continue. You'll practice with various examples to master loop concepts.", order = 4),
            Lesson(courseId = courseId, title = "Functions and Modules", content = "This lesson introduces functions in Python. You'll learn how to define functions, pass arguments, use return values, and work with default and keyword arguments. We'll also explore Python's module system for organizing and reusing code across different files.", order = 5),
            Lesson(courseId = courseId, title = "Lists and List Comprehensions", content = "Dive deeper into Python lists. We'll cover list operations, methods like append, insert, remove, and sort, slicing notation, and the powerful list comprehension syntax for creating new lists based on existing ones. You'll practice with exercises that demonstrate the flexibility of lists in Python.", order = 6),
            Lesson(courseId = courseId, title = "Dictionaries and Sets", content = "Explore Python's dictionary and set data structures. You'll learn how to create and manipulate dictionaries for key-value mappings, use common dictionary methods, and understand sets for handling unique collections of elements. We'll discuss when to use dictionaries versus lists and how to optimize your code.", order = 7),
            Lesson(courseId = courseId, title = "File I/O Operations", content = "Learn how to work with files in Python. This lesson covers opening and closing files, reading and writing text and binary data, using context managers with the 'with' statement, and handling file paths with the os and pathlib modules. You'll practice with examples of file processing tasks.", order = 8),
            Lesson(courseId = courseId, title = "Exception Handling", content = "This lesson focuses on handling errors in Python programs. You'll learn about exceptions, using try-except blocks, catching specific exceptions, the else and finally clauses, and creating custom exceptions. We'll practice writing robust code that gracefully handles unexpected situations.", order = 9),
            Lesson(courseId = courseId, title = "Introduction to Object-Oriented Programming", content = "The final lesson introduces object-oriented programming in Python. You'll learn about classes, objects, attributes, methods, inheritance, and encapsulation. We'll build a simple project that demonstrates how OOP can help organize code and model real-world concepts. This lesson prepares you for more advanced Python programming.", order = 10)
        )
    }

    private fun generateWebDevLessons(courseId: Int): List<Lesson> {
        return listOf(
            Lesson(courseId = courseId, title = "Introduction to Web Development", content = "This lesson introduces the world of web development. You'll learn about how the web works, the client-server model, HTTP protocol basics, and the roles of front-end and back-end development. We'll also discuss the modern web development ecosystem and the skills you'll need to become a web developer.", order = 1),
            Lesson(courseId = courseId, title = "HTML Fundamentals", content = "Learn the building blocks of web pages with HTML (HyperText Markup Language). We'll cover document structure, common elements like headings, paragraphs, links, images, lists, and tables. You'll practice creating semantic markup and understand the importance of accessibility in HTML.", order = 2),
            Lesson(courseId = courseId, title = "CSS Basics", content = "This lesson introduces Cascading Style Sheets (CSS) for styling web pages. You'll learn about selectors, properties, values, the box model, and basic layouts. We'll practice applying styles to HTML elements, understanding specificity, and using different units for measurements.", order = 3),
            Lesson(courseId = courseId, title = "CSS Layout Techniques", content = "Dive deeper into CSS with modern layout techniques. We'll explore Flexbox and CSS Grid for creating responsive designs, positioning elements, implementing media queries for different screen sizes, and using CSS variables for maintainable code. You'll practice building common UI components and layouts.", order = 4),
            Lesson(courseId = courseId, title = "JavaScript Fundamentals", content = "Introduction to JavaScript, the programming language of the web. We'll cover variables, data types, operators, control flow with conditionals and loops, and functions. You'll practice writing simple scripts and understand how JavaScript fits into web development.", order = 5),
            Lesson(courseId = courseId, title = "DOM Manipulation with JavaScript", content = "Learn how to interact with the Document Object Model (DOM) using JavaScript. You'll understand how to select elements, modify content and attributes, create and remove elements, and handle events like clicks and form submissions. We'll practice creating interactive web pages that respond to user actions.", order = 6),
            Lesson(courseId = courseId, title = "Responsive Web Design", content = "This lesson focuses on creating websites that work well on all devices. You'll learn about responsive design principles, flexible layouts, adaptive images, viewport settings, and testing across devices. We'll practice converting fixed layouts to responsive ones and implementing mobile-first design strategies.", order = 7),
            Lesson(courseId = courseId, title = "Web Forms and Validation", content = "Learn how to create and process web forms. We'll cover form elements, input types, form validation using HTML5 attributes and JavaScript, handling form submission, and basic security considerations. You'll practice building user-friendly forms with appropriate feedback.", order = 8),
            Lesson(courseId = courseId, title = "Introduction to APIs and Fetch", content = "This lesson introduces web APIs and how to interact with them using JavaScript. You'll learn about RESTful APIs, JSON data format, making HTTP requests with the Fetch API, handling responses, and working with asynchronous JavaScript using Promises and async/await. We'll practice retrieving and displaying data from public APIs.", order = 9),
            Lesson(courseId = courseId, title = "Web Development Project", content = "In this final lesson, you'll apply everything you've learned to build a complete web application. We'll walk through planning, designing, and implementing a simple project that combines HTML, CSS, and JavaScript. You'll practice problem-solving, debugging, and deploying your application to share with others.", order = 10)
        )
    }

    private fun generateJavaLessons(courseId: Int): List<Lesson> {
        return listOf(
            Lesson(courseId = courseId, title = "Introduction to Java Programming", content = "This lesson introduces Java, a popular object-oriented programming language. You'll learn about Java's history and features, the Java Virtual Machine (JVM), setting up the Java Development Kit (JDK), and writing your first Java program. We'll cover the basic structure of Java applications and the compile-run process.", order = 1),
            Lesson(courseId = courseId, title = "Variables and Data Types in Java", content = "Learn about Java's type system, including primitive data types (int, double, boolean, char) and reference types. We'll cover variable declaration, initialization, naming conventions, type conversion, and constants. You'll practice working with different data types and understand Java's strong typing system.", order = 2),
            Lesson(courseId = courseId, title = "Control Flow in Java", content = "This lesson covers decision-making and repetition in Java programs. You'll learn about conditional statements (if-else, switch), logical operators, comparison operators, and loops (for, while, do-while). We'll practice implementing control structures through various programming challenges.", order = 3),
            Lesson(courseId = courseId, title = "Methods and Parameters", content = "Learn how to organize code with methods in Java. We'll cover method declaration, parameters, return types, method overloading, variable scope, and the 'this' keyword. You'll practice creating methods for specific tasks and understand the importance of modular code design.", order = 4),
            Lesson(courseId = courseId, title = "Introduction to Object-Oriented Programming", content = "This lesson introduces object-oriented programming concepts in Java. You'll learn about classes, objects, attributes, methods, constructors, and encapsulation. We'll practice creating simple classes, instantiating objects, and understanding the relationship between classes and objects.", order = 5),
            Lesson(courseId = courseId, title = "Arrays and ArrayLists", content = "Learn about data collections in Java, focusing on arrays and ArrayLists. We'll cover array declaration, initialization, accessing elements, multidimensional arrays, and using the ArrayList class from Java Collections Framework. You'll practice common operations like searching, sorting, and iterating through collections.", order = 6),
            Lesson(courseId = courseId, title = "Inheritance and Polymorphism", content = "This lesson explores advanced OOP concepts in Java. You'll learn about inheritance, creating subclasses, method overriding, using 'super', abstract classes, interfaces, and polymorphism. We'll practice designing class hierarchies and implementing interfaces to create flexible, extensible code.", order = 7),
            Lesson(courseId = courseId, title = "Exception Handling", content = "Learn how to handle errors and exceptional conditions in Java. We'll cover try-catch blocks, the exception hierarchy, checked vs. unchecked exceptions, the finally clause, and creating custom exceptions. You'll practice writing robust code that gracefully handles failures.", order = 8),
            Lesson(courseId = courseId, title = "File I/O and Serialization", content = "This lesson introduces file operations in Java. You'll learn about reading and writing text files, working with binary data, using BufferedReader and BufferedWriter, file paths, and object serialization. We'll practice with examples of persistent data storage and retrieval.", order = 9),
            Lesson(courseId = courseId, title = "Java Collections Framework", content = "The final lesson explores Java's rich collections framework. You'll learn about Lists, Sets, Maps, and their implementations like ArrayList, LinkedList, HashSet, TreeSet, HashMap, and TreeMap. We'll cover choosing the right collection type, common operations, and iterating through collections. You'll practice solving problems with these versatile data structures.", order = 10)
        )
    }

    private val pythonLessonTitles = (1..10).map { "Python Lesson $it" }
    private val webDevLessonTitles = (1..10).map { "WebDev Lesson $it" }
    private val javaLessonTitles = (1..10).map { "Java Lesson $it" }
} 