# Personal Tutor App

### Overview

The **Personal Tutor App** is a mobile tutoring platform developed for COMP6239 Mobile Applications Development at the University of Southampton. It connects tutors and students, enabling course creation, enrollment management, and progress tracking using modern Android technologies and Firebase.

### Features

1. **Course Enrollment Management**:
   - Tutors can approve or reject student enrollment requests.
   - View student details (name, ID, email).
   - Simulated email notifications for enrollment status updates.
2. **Course Creation and Management**:
   - Create courses with titles, subjects, descriptions, and tutor IDs.
   - Add lessons with text content and media (images/videos).
3. **Student Progress Tracking**:
   - Monitor completed lessons and calculate progress percentages.
   - Automatically update progress when new lessons are added.
4. **Real-Time Data Sync**:
   - Uses Firebase Firestore for live updates to courses, enrollments, and progress.
5. **User Authentication**:
   - Secure login via Firebase Authentication (email/password).
6. **Lesson Completion and Testing**:
   - Students can mark lessons as completed.
   - Tutors can send tests to students for specific courses.

### Target Users

1. **Students**: Enroll in courses, access lessons, and track learning progress.
2. **Tutors**: Create and manage courses, review enrollments, and monitor student progress.
3. **Educational Institutions**: Scalable for schools or tutoring centers.

### Technology Stack

1. **Frontend**: Jetpack Compose, Material3
2. **Backend**: Firebase Firestore, Firebase Authentication, Firebase Storage
3. **Language**: Kotlin (with coroutines and flows)
4. **Architecture**: MVVM with Hilt dependency injection
5. **Tools**: Android Studio, Firebase Console

### Installation

#### Prerequisites

1. Android Studio (2024.2.1 or higher)
2. JDK 17
3. Firebase account and project
4. Git
5. Android device/emulator (API 21+)

#### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-repo>/personal-tutor-app.git
   cd personal-tutor-app
   ```
2. Set up Firebase:
   - Create a Firebase project.
   - Add `google-services.json` to the app directory.
3. Build and run:
   - Open in Android Studio.
   - Sync project with Gradle.
   - Run on an emulator or device.

### Usage

1. **Tutors**: Log in, create courses, manage enrollments, and track student progress.
2. **Students**: Log in, browse and enroll in courses, complete lessons, and view progress.

### License

MIT License
