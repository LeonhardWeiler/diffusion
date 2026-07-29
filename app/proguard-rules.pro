# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class io.github.leonhardweiler.diffusion.ui.model.Cred { *; }
-keep class io.github.leonhardweiler.diffusion.ui.model.Cred$* { *; }

# JGit and jsch both decide by name at runtime what to load: JGit finds its
# transports through META-INF/services, and jsch names the class of every cipher,
# mac and key type in a config string. Shrunk by what the code visibly calls,
# both of them lose exactly the parts that are only ever named.
-keep class org.eclipse.jgit.** { *; }
-keep class com.jcraft.jsch.** { *; }

# What those two reference and Android does not have: a system git to ask for its
# config, the jcraft agent proxy, the http transport that is never built here.
-dontwarn org.eclipse.jgit.**
-dontwarn com.jcraft.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**
-dontwarn javax.naming.**
-dontwarn java.lang.management.**