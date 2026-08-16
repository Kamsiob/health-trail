# R8 rules for the release build.
#
# **This file was referenced by `build.gradle.kts` and did not exist**, so the
# release variant had never been built at all: `minifyReleaseWithR8` fails
# outright on a missing configuration rather than warning. Found on the first
# attempt to produce a signed APK. D161.
#
# **Written narrow on purpose.** A blanket `-keep class com.kamsiob.**` would
# make the build succeed and turn minification off in everything but name,
# which is worse than not minifying: it looks done. The app's own code uses
# almost no reflection, so it needs no keeps of its own. What follows is the
# two libraries that resolve classes by name at runtime, where R8 cannot see
# the edge and will strip something that is genuinely used.
#
# Kamsiob, AGPL-3.0.

# -- SQLCipher, which is where the notebook lives ----------------------------
#
# **Native methods are bound by name.** JNI looks the Java side up by its exact
# class and method name, so a renamed method is a link error at the first
# query, which on this app means the database will not open and nothing the
# person wrote down can be read. Not a screen going wrong: everything.
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# -- BouncyCastle, which is the export's encryption --------------------------
#
# **The JCE resolves algorithms by string name through reflection**, so the
# implementation classes have no incoming reference R8 can follow and every one
# of them looks dead. Stripped, an export either fails or, worse, fails only on
# the restore months later when the person actually needs it.
#
# The provider's own dead weight is dropped rather than kept: the JDK-only
# entry points are not on Android and warning about them is noise.
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
-keep class org.bouncycastle.crypto.** { *; }
-dontwarn javax.naming.**
-dontwarn java.awt.**
-dontwarn org.bouncycastle.jce.provider.BouncyCastleProvider

# -- Kotlin and Compose ------------------------------------------------------
#
# Both ship their own consumer rules, so this adds only what they leave open:
# the metadata a coroutine stack trace is reconstructed from. Without it a
# crash report from a suspend function points nowhere, which is the one moment
# the report has to be readable.
-keepattributes SourceFile,LineNumberTable,RuntimeVisibleAnnotations
-renamesourcefileattribute SourceFile

# **Kept because the app reads its own version out at runtime** for the export
# container's header, and the container is the thing another copy of the app
# has to be able to open years from now.
-keep class com.kamsiob.healthtrail.BuildConfig { *; }
