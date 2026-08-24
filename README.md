Ratibu

Ratibu is an Android appointment-booking application designed to connect customers with service providers and manage bookings, communication, and appointment-related activities from a mobile application.

The project focuses on building a complete mobile product rather than a collection of isolated features. It includes user authentication, provider discovery, appointment booking, booking management, realtime chat, notifications, reminders, caching, and analytics.

«Status: Active development.»

Features

Authentication & User Management

- User registration and login
- User profiles
- Provider profiles
- Role-oriented application flows
- Session and user-state management

Appointment Booking

- Browse service providers
- View provider details
- Create bookings
- View upcoming bookings
- Manage existing bookings
- View today's appointments
- Booking-specific data persistence

Communication

- Realtime chat functionality
- Chat conversation list
- Individual chat screens
- User-to-user messaging through the application's repository layer

Notifications & Reminders

- Firebase Cloud Messaging integration
- Push notification handling
- Background reminder processing
- Appointment-related reminders

Supporting Features

- Local caching
- Network result/error abstraction
- Analytics functionality
- Cloudinary integration for media handling
- Application settings
- Structured navigation between application screens

Technology Stack

- Kotlin
- Android SDK
- Jetpack components
- Firebase
- Firebase Cloud Messaging (FCM)
- Cloudinary
- Coroutines
- ViewModel
- Repository pattern
- WorkManager
- Android Navigation

Application Structure

The project separates UI, application state, data access, and supporting services.

app/
└── src/main/java/com/ikoha/ratibu/
    │
    ├── data/
    │   ├── navigation/
    │   ├── screens/
    │   ├── ui/
    │   └── viewmodel/
    │
    ├── repository/
    │   ├── BookingRepository.kt
    │   ├── ChatRepository.kt
    │   └── UserRepository.kt
    │
    ├── AnalyticsEngine.kt
    ├── CacheManager.kt
    ├── CloudinaryHelper.kt
    ├── Models.kt
    ├── MyFirebaseMessagingService.kt
    ├── NetworkResult.kt
    ├── ReminderWorker.kt
    │
    └── MainActivity.kt

The repository layer provides an abstraction between the application's UI/state-management layer and its underlying data sources.

Main Screens

Ratibu currently contains dedicated screens for several application flows, including:

- Login
- Registration
- Home
- Dashboard
- Provider details
- Provider profile
- Booking
- My bookings
- Today's appointments
- Chat list
- Individual chat
- Analytics
- Settings

The application uses centralized navigation to manage movement between these screens.

Architecture & Design

The application uses a separation of concerns between UI, state management, repositories, and supporting infrastructure.

A simplified flow is:

UI Screens
    │
    ▼
ViewModels
    │
    ▼
Repositories
    │
    ├── Firebase / Remote Data
    ├── Local Cache
    └── Other Data Sources

Supporting services handle concerns that should not be tightly coupled to individual screens.

Examples include:

- "CacheManager" — local data caching
- "AnalyticsEngine" — application analytics
- "MyFirebaseMessagingService" — push notification handling
- "ReminderWorker" — background appointment reminders
- "CloudinaryHelper" — media upload/handling
- "NetworkResult" — representation of network operation results

Repository Layer

The repository layer contains dedicated components for major application domains.

"BookingRepository"

Responsible for appointment-related data operations.

"ChatRepository"

Responsible for retrieving and managing chat-related data.

"UserRepository"

Responsible for user-related data operations.

Separating these responsibilities makes the application easier to maintain as additional functionality is introduced.

Notifications

Ratibu integrates Firebase Cloud Messaging to support push notifications.

"MyFirebaseMessagingService" handles incoming Firebase messages and provides the application's notification-processing layer.

Appointment reminders are handled separately through "ReminderWorker", allowing reminder-related work to run independently of individual UI screens.

Media

The application includes "CloudinaryHelper" for handling cloud-based media operations.

This allows media-related functionality to remain separate from the application's core UI and repository logic.

Caching & Offline Considerations

"CacheManager" provides a dedicated component for local caching.

The project also uses a "NetworkResult" abstraction to represent the outcome of network operations and separate successful responses from failure states.

These components provide a foundation for improving the application's resilience when network connectivity is unavailable or unreliable.

Analytics

Ratibu includes an "AnalyticsEngine" for application analytics.

Keeping analytics functionality in a dedicated component prevents individual screens and business flows from becoming tightly coupled to analytics implementation details.

Project Development

Ratibu was developed as a collaborative software-engineering project.

Development involved working with:

- Android application architecture
- Kotlin
- Firebase
- Repository-based data access
- UI/navigation
- Authentication flows
- Realtime communication
- Push notifications
- Background processing
- Cloud media services

The project also involved iterative debugging and feature development across multiple application layers.

Getting Started

Prerequisites

- Android Studio
- Android SDK
- A configured Firebase project
- Kotlin/Gradle-compatible Android development environment

Clone the Repository

git clone https://github.com/mfalme1k0/Ratibu.git
cd Ratibu

Open the project in Android Studio and allow Gradle to synchronize the project dependencies.

Firebase Configuration

The application requires Firebase configuration to use Firebase-backed functionality.

Add the appropriate Firebase configuration file to the Android application module according to the project's Firebase setup.

Do not commit private credentials, API keys, service-account files, or other secrets to the repository.

Build

Build the application using Android Studio or Gradle:

./gradlew build

To install a debug build on a connected Android device:

./gradlew installDebug

Project Status

Ratibu is an ongoing application project.

The current codebase contains the core structure for:

- Authentication
- User management
- Provider profiles
- Appointment booking
- Booking management
- Chat
- Notifications
- Background reminders
- Caching
- Analytics
- Media handling

Further development can extend the application with additional product functionality, stronger offline support, improved testing, and production deployment.

What This Project Demonstrates

Ratibu demonstrates practical experience with:

- Kotlin and Android development
- Mobile application architecture
- Repository pattern
- ViewModel-based state management
- Firebase integration
- Realtime application features
- Push notifications
- Background work
- Local caching
- Network error handling
- Cloud media integration
- Navigation and multi-screen application design
- Collaborative software development

