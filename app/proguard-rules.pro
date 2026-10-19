# ==============================================================================
# Unravel (com.unravel.app) — R8 / ProGuard Optimization & Keep Rules Suite
# Target Release: v0.6.0-beta.0 | AGP 9.4.0 | R8 Full Mode
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Bytecode Attributes & Generic Signatures
# ------------------------------------------------------------------------------
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

# ------------------------------------------------------------------------------
# 2. Kotlinx Serialization
# ------------------------------------------------------------------------------
# Preserve classes annotated with @Serializable
-keep @kotlinx.serialization.Serializable class * {
    <init>(...);
    *** Companion;
    *** serializer(...);
}

# Preserve companion objects that hold serializers
-keepclassmembers class * {
    *** Companion;
}

# Preserve synthetic generated serializers
-keep class *$$serializer {
    public static final *** INSTANCE;
    public static final *** Companion;
    public final *** descriptor;
    <init>(...);
}

# Preserve custom and built-in KSerializer implementations
-keep class * implements kotlinx.serialization.KSerializer {
    public static final *** INSTANCE;
    public final *** descriptor;
    <init>(...);
    *;
}

# Preserve Core DSA Engine domain models and polymorphic contracts
# (Crucial: prevents obfuscation of polymorphic discriminators like VisualStructureState and ExpectedAction)
-keep class com.unravel.core.engine.model.** { *; }
-keep interface com.unravel.core.engine.model.** { *; }
-keep enum com.unravel.core.engine.model.** { *; }

# Preserve Core Database persisted models and serializers
-keep class com.unravel.core.database.model.** { *; }
-keep class com.unravel.core.database.datastore.** { *; }
-keep class com.unravel.core.database.converter.** { *; }

# ------------------------------------------------------------------------------
# 3. AndroidX Room Database 2.8.5
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.EntityDeletionOrUpdateAdapter {
    <init>(...);
}
-keep class * extends androidx.room.EntityInsertionAdapter {
    <init>(...);
}
-keep class * extends androidx.room.SharedSQLiteStatement {
    <init>(...);
}
-keep @androidx.room.Entity class * {
    <fields>;
    <init>(...);
}
-keep @androidx.room.Dao interface * {
    *;
}
-keep class * implements androidx.room.RoomDatabase$Callback {
    *;
}
-keep class com.unravel.core.database.**_Impl {
    <init>(...);
    *;
}
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 4. AndroidX DataStore & Preferences
# ------------------------------------------------------------------------------
-keep class androidx.datastore.** { *; }
-keep class * implements androidx.datastore.core.Serializer {
    public static final *** INSTANCE;
    <init>(...);
    *;
}

# ------------------------------------------------------------------------------
# 5. Jetpack Compose & Strong Skipping Mode
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.Stable *;
    @androidx.compose.runtime.Immutable *;
}
-keep class androidx.compose.runtime.** { *; }

# ------------------------------------------------------------------------------
# 6. Syntax Highlighting (Snipme Highlights)
# ------------------------------------------------------------------------------
-keep class dev.snipme.highlights.** { *; }
-dontwarn dev.snipme.highlights.**

# ------------------------------------------------------------------------------
# 7. AGSL RuntimeShader & Canvas Renderers
# ------------------------------------------------------------------------------
# Preserve AGSL shader source strings and color resolution logic
-keepclassmembers class com.unravel.feature.racemode.ui.GlassPrismDividerKt {
    static final java.lang.String PRISM_REFRACTION_SHADER_SRC;
    *;
}
-keep class com.unravel.core.canvasrenderer.** { *; }

# ------------------------------------------------------------------------------
# 8. Haze Glassmorphism & Visual Effects
# ------------------------------------------------------------------------------
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# ------------------------------------------------------------------------------
# 9. Kotlin Coroutines & Asynchronous Dispatch
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 10. AndroidX Navigation & Lifecycle ViewModels
# ------------------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.navigation3.** { *; }
