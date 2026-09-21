# Permission Watch — R8 configuration.
#
# The app has almost no reflection, so this file is deliberately short. Every rule below exists for
# a named reason; anything that cannot be justified should be deleted rather than kept "just in case".

# --- Kotlinx Serialization ----------------------------------------------------------------------
# Only the export report is serialized. Serializers are generated as nested classes and are looked
# up reflectively by the runtime, so the generated companions must survive.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class app.vazovsky.permaware.domain.export.ReportBuilder$* {
    *** Companion;
}
-keepclasseswithmembers class app.vazovsky.permaware.domain.export.ReportBuilder$* {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room ---------------------------------------------------------------------------------------
# Room generates implementations that reference entity constructors reflectively at open time.
-keep class app.vazovsky.permaware.data.db.** { *; }

# --- WorkManager --------------------------------------------------------------------------------
# ScanWorker is instantiated by name by the Hilt worker factory.
-keep class app.vazovsky.permaware.scan.ScanWorker { *; }

# --- Enum names in persisted data ---------------------------------------------------------------
# Enum *names* are written into the database and into exported reports, and read back with
# valueOf(). Obfuscating them would silently invalidate every stored snapshot on update.
-keepclassmembers enum app.vazovsky.permaware.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public java.lang.String name();
}
-keepclassmembers enum app.vazovsky.permaware.data.prefs.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public java.lang.String name();
}

# --- Diagnostics --------------------------------------------------------------------------------
# Keep line numbers so a crash report from a user is actionable, but hide the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip verbose/debug logging from release builds. Log.w/e are kept: they carry real failures and
# never contain package lists or scan results.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
