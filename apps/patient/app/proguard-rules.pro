# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.aarogyasanjeevani.patient.**$$serializer { *; }
-keepclassmembers class com.aarogyasanjeevani.patient.** { *** Companion; }
-keepclasseswithmembers class com.aarogyasanjeevani.patient.** { kotlinx.serialization.KSerializer serializer(...); }
