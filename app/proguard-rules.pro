# قوانین ProGuard / R8 برای نسخه Release برنامه Dashboard 112
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod

# حفظ مدل‌های Serialization برای پشتیبان‌گیری و بازیابی JSON
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class ir.dashboard112.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class ir.dashboard112.app.** { *; }

# حفظ Entityها و کلاس‌های Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**
