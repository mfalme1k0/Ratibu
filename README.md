# Ratibu

Ratibu is an Android appointment-booking application built with Kotlin and Jetpack Compose.

The application is designed to connect customers with service providers and provide a single platform for discovering providers, booking appointments, managing sessions, communicating through chat, and receiving appointment notifications and reminders.

Ratibu was developed as an **individual software-engineering project** and has gone through multiple iterations and versioned releases during development.

> **Current release:** v1.0.30

## Features

### Authentication & User Management

- User registration and login
- User profiles
- Service-provider profiles
- User-specific application flows
- Firebase Authentication integration

### Appointment Management

- Browse service providers
- View provider profiles
- Book appointments
- View upcoming bookings
- View today's appointments
- Manage booking information

### Realtime Chat

- Chat between users and service providers
- Conversation list
- Individual chat screens
- Firebase Realtime Database integration

### Notifications & Reminders

- Firebase Cloud Messaging integration
- Push notifications
- Appointment reminders
- Background reminder processing using WorkManager
- Notification channels for Android

### Additional Functionality

- Local caching
- Application analytics
- Network operation result handling
- Cloudinary image uploads
- Location-related functionality
- Application settings
- Multi-screen navigation

---

# Technology Stack

- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- **Android SDK**
- **Firebase Authentication**
- **Firebase Realtime Database**
- **Firebase Cloud Messaging**
- **Firebase Crashlytics**
- **Android ViewModel**
- **Navigation Compose**
- **WorkManager**
- **Coroutines**
- **Cloudinary**
- **Coil**
- **Gson**
- **Gradle**

---

# Architecture

Ratibu separates UI, state management, data access, and supporting application services.

A simplified application flow is:

```text
┌─────────────────────┐
│      UI / Compose   │
│       Screens       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│      ViewModels     │
│   Application State │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│     Repositories    │
│                     │
│ BookingRepository   │
│ ChatRepository      │
│ UserRepository      │
└──────────┬──────────┘
           │
           ├──────────────► Firebase
           │
           └──────────────► Local Cache