# Proguard rules for Inventario Pool
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
