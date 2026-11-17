# AGP's default rules already cover AndroidX/Compose/Hilt. The rules
# below are specific to this app's own kotlinx.serialization DTOs
# (core:network/core:data's `@Serializable` request/response models) --
# the serialization plugin's own consumer rules already keep most of
# this, but these are the official, documented kotlinx.serialization
# rules (https://github.com/Kotlin/kotlinx.serialization#serialization-of-3rd-party-classes),
# kept explicit here rather than trusted entirely to the dependency's
# defaults: a model that works fine in an unminified debug build but
# silently loses a field to R8 in release is exactly the kind of bug
# that only ever shows up in production.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <1>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Every core:network/core:data DTO -- field names/order must survive
# intact since they drive Supabase PostgREST's JSON mapping; keeping the
# classes themselves (not just their serializers, above) is the extra
# margin against a future DTO that R8's full-mode shrinking reaches some
# other way these generated rules don't anticipate.
-keep,includedescriptorclasses class com.aarogya.sanjeevani.core.network.**$$serializer { *; }
-keep,includedescriptorclasses class com.aarogya.sanjeevani.core.data.**$$serializer { *; }
-keepclassmembers class com.aarogya.sanjeevani.core.network.** {
    *** Companion;
}
-keepclasseswithmembers class com.aarogya.sanjeevani.core.data.** {
    @kotlinx.serialization.Serializable <fields>;
}

# androidx.security.crypto (EncryptedSessionManager's own session storage)
# pulls in Google Tink, whose KeyTemplate and a few other classes
# reference error-prone's @Immutable annotation purely for static
# analysis -- it's compile-time-only and never present at runtime, so
# R8 correctly can't find it; safe to tell it not to warn about this
# one, rather than keep the whole (unused at runtime) annotation module.
-dontwarn com.google.errorprone.annotations.Immutable
