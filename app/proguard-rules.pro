# Firebase Realtime Database
-keep class com.ik0ha.ratibu.data.** { *; }

# Cloudinary
-keep class com.cloudinary.** { *; }

# Hilt (if used later)
-keep class dagger.hilt.** { *; }
-keep class com.google.dagger.hilt.** { *; }

# Retain Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Retain names of ViewModel subclasses
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
