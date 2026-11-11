# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.aarogyasanjeevani.doctor.**$$serializer { *; }
-keepclassmembers class com.aarogyasanjeevani.doctor.** { *** Companion; }
-keepclasseswithmembers class com.aarogyasanjeevani.doctor.** { kotlinx.serialization.KSerializer serializer(...); }
