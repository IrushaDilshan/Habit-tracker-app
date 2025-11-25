The Personal Wellness Companion app is an Android-based mobile application developed using
Android Studio and Kotlin to help users manage and improve their daily health and wellness
routines. The app encourages users to build healthy habits, track moods, and stay hydrated through
interactive and user-friendly features.
The app includes three main modules:
1. Daily Habit Tracker – Allows users to add, edit, and delete daily wellness habits such as
drinking water, exercising, or meditating. It displays daily progress to motivate users to
maintain consistency.
2. Mood Journal with Emoji Selector – Enables users to record their daily moods with a
date/time stamp and select an emoji representing their feelings. Users can view their past
moods in a simple list or calendar view to reflect on emotional trends.
3. Hydration Reminder – Provides reminders using Notifications and AlarmManager (or
WorkManager) to prompt users to drink water at regular intervals that they can customize.
The app also includes an advanced feature such as a home-screen widget that displays the current
day’s habit completion percentage or a simple mood trend chart using MPAndroidChart. All user
data is stored locally using SharedPreferences, ensuring persistence without a database.
The application is built using multiple Activities and Fragments for modular screen navigation and
uses explicit and implicit intents for transitions and data sharing (for example, sharing a mood
summary). The user interface is designed to be responsive and adaptive for both phones and
tablets in portrait and landscape modes, providing a smooth and accessible experience.
