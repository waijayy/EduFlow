# 🎓 EduFlow

An interactive, native Android educational application designed to enhance the mobile learning experience. EduFlow provides a seamless environment for students to watch educational content, take contextual notes, and test their knowledge through an integrated quiz system. 

The application is built entirely in **Java** for the Android ecosystem and utilizes a robust **PostgreSQL (PLpgSQL)** database architecture to manage course data, user progress, and complex relational operations.

---

## ✨ Key Features

* **📈 Advanced Video Tracking:** Accurately keeps track of student progress through educational videos, ensuring learners can resume exactly where they left off. 
* **📝 Contextual Video Notes:** Allows users to take, edit, and retrieve notes tied directly to their video sessions, making studying and revision highly efficient.
* **❓ Interactive Quiz System:** Evaluates learner understanding with an integrated, database-driven quiz engine.
* **🗄️ Robust Data Management:** Employs a structured relational database to securely and reliably handle users, courses, quiz states, and tracking logs.

## 🛠️ Tech Stack

* **Client / Frontend:** Android (Java)
* **Database Scripting:** PostgreSQL / PLpgSQL
* **Build System:** Gradle (Kotlin DSL via `build.gradle.kts`)
* **IDE:** Android Studio

## 📂 Project Structure

A quick look at the core files and directories in this repository:

```text
EduFlow/
├── app/                            # Main Android application module containing Java source code and XML resources
├── gradle/                         # Gradle wrapper files for consistent build environments
├── .idea/                          # Android Studio IDE configuration files
├── DEBUGGING_VIDEO_TRACKING.md     # Developer documentation for troubleshooting video playback and tracking logs
├── VIDEO_TRACKING_SUGGESTIONS.md   # Architectural suggestions and improvements for the video tracking system
├── database_setup.sql              # Initial SQL script to provision the core database schema (Users, Courses, etc.)
├── quiz_system.sql                 # SQL schema and PLpgSQL functions for managing the quiz module
├── video_notes_fix.sql             # SQL patch/updates for the video notes feature and data relations
├── build.gradle.kts                # Project-level Gradle build configuration
└── settings.gradle.kts             # Gradle settings and module inclusions
```

## 🚀 Getting Started

Follow these instructions to set up the project on your local development machine.

### Prerequisites

* [Android Studio](https://developer.android.com/studio) (Latest stable version recommended)
* Java Development Kit (JDK) 17 or higher
* PostgreSQL installed and running locally or on a dedicated server.

### 1. Clone the Repository

```bash
git clone [https://github.com/yongjeen2409/EduFlow.git](https://github.com/yongjeen2409/EduFlow.git)
cd EduFlow
```

### 2. Database Setup

Before running the application, you need to provision the backend database using the provided SQL scripts. Connect to your PostgreSQL instance and execute the scripts in the following order:

1.  Run `database_setup.sql` to build the foundational tables.
2.  Run `quiz_system.sql` to add the quiz-related tables and triggers.
3.  Run `video_notes_fix.sql` to apply the latest patches to the notes schema.

*Note: Ensure you update your Android application's database connection strings (likely located in a configuration class or resource file) to match your local PostgreSQL credentials.*

### 3. Build and Run the App

1.  Open **Android Studio**.
2.  Select **Open** and navigate to the cloned `EduFlow` directory.
3.  Wait for Gradle to finish syncing the project dependencies.
4.  Select a connected physical device or Android Emulator.
5.  Click the **Run** button (Shift + F10) to build and deploy the application.

## 🐛 Debugging & Development Notes

If you are contributing to or modifying the video playback features, please review the specialized documentation provided in the root directory:

* **`DEBUGGING_VIDEO_TRACKING.md`**: Contains known issues, logcat filters, and methodologies for debugging state changes during video playback.
* **`VIDEO_TRACKING_SUGGESTIONS.md`**: Outlines future roadmap items and structural suggestions to optimize how video progress is synced with the database.

## 🤝 Contributing

Contributions are welcome! If you'd like to improve EduFlow, please fork the repository and use a feature branch. Pull requests are warmly welcomed.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
