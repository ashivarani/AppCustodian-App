# ==============================================================================
# App Custodian ProGuard / R8 Optimization Rules
# ==============================================================================
# Code obfuscation and shrinking (`isMinifyEnabled = true`) are active for
# release builds. The rules below preserve critical reflection metadata, model
# structures, and stack trace line numbers required at runtime.
#
# AUTOMATICALLY PRESERVED COMPONENTS (No explicit rules required):
# - AndroidManifest.xml components (MainActivity, ForceStopAccessibilityService).
# - Jetpack Compose & Kotlin Coroutines (bundled consumer ProGuard rules).
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Data Models & Sealed Class Hierarchies
# ------------------------------------------------------------------------------
# Preserve ForceStopResult and its child sealed classes (Success, Failed)
# to guarantee intact type checking and pattern matching across boundaries.
-keep class com.appcustodian.app.data.model.ForceStopResult { *; }
-keep class com.appcustodian.app.data.model.ForceStopResult$* { *; }

# ------------------------------------------------------------------------------
# 2. Kotlin Runtime Metadata & Reflection Hygiene
# ------------------------------------------------------------------------------
# Retain Kotlin metadata annotations and class structures to ensure data class
# utility methods (copy(), componentN()) and sealed class checks remain fully
# operational after R8 optimization pass.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep class kotlin.Metadata { *; }

# ------------------------------------------------------------------------------
# 3. Stack Trace Preservation & Source Masking
# ------------------------------------------------------------------------------
# Preserve line numbers in crash stack traces while stripping raw source file
# path attributes to prevent exposing local build file structures.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile