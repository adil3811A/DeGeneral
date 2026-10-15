#
# R8 rules for DeGeneral's release build.
#
# These rules are UNVERIFIED by the build machine that wrote them — the first
# `./gradlew :androidApp:bundleRelease` with this file exercises them on real
# code, and the app must then be exercised by hand before publishing (model
# download, streaming chat, journal save, navigation of every route).
#

# Keep original line numbers in stack traces. R8 obfuscation alone changes
# names; line numbers are what make Play's deobfuscated reports readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Llamatik / llama.cpp (JNI)
#
# `LlamaBridge` and friends are `expect`/`actual` Kotlin objects whose Android
# actual talks to llama.cpp through JNI. Native code looks classes and methods
# up by their exact names at runtime; renaming here breaks model loading and
# streaming with no compile-time signal.
#
# `GenStream` is the worst case: `streamOnce()` passes an anonymous object
# implementing it, and llama.cpp calls `onDelta`/`onComplete`/`onError` on that
# object from the native thread. If R8 renames the impl class or its methods,
# linking fails at JNI lookup and generation dies mid-stream.
# ---------------------------------------------------------------------------
-keep class com.llamatik.** { *; }
-keep class * implements com.llamatik.library.platform.GenStream { *; }

# ---------------------------------------------------------------------------
# kotlinx.serialization — type-safe navigation routes
#
# Navigation 2.9 resolves each `@Serializable data object` route's serializer at
# runtime. The companion and the generated `$$serializer` classes must survive
# with their names untouched or route resolution fails at the first
# `navController.navigate(...)`.
# ---------------------------------------------------------------------------
-keepattributes *Annotation*,InnerClasses
-keep,includedescriptorclasses class com.adll.de_general.**$$serializer { *; }
-keepclassmembers class com.adll.de_general.** {
    *** Companion;
}
-keepclasseswithmembers class com.adll.de_general.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Coroutines
#
# The suspend-function invoke bridge is looked up by exact signature; without
# this rule R8 can restructure it and break `withContext`/`launch` in ways that
# only appear on real devices.
# ---------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.SafeContinuation {
    volatile java.lang.Object result;
}

