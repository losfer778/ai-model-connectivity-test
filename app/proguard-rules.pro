# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\tools\adt-bundle-windows-x86_64-20131030\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#如果你想开启下面某项规则 把#删除即可 不过下面的只是例子 具体规则还得你自己根据项目写


# ==============================
# 1. 警告抑制（减少构建日志噪音）---默认开启
# ==============================
-dontnote **                           # 忽略所有"note"级别的警告
-dontwarn **                           # 忽略所有"warning"级别的警告
# 典型使用场景：
# - 当第三方库存在潜在兼容性问题但不影响运行时
# - 当明确知道某些警告可以安全忽略时

# ==============================
# 2. 字典文件配置（增强混淆强度）默认使用ConfusionDictionary.txt字典混淆
# ==============================
# 需要提前准备一个文本文件（如 ConfusionDictionary.txt），每行一个单词
-packageobfuscationdictionary ConfusionDictionary.txt  # 包名混淆字典
-classobfuscationdictionary ConfusionDictionary.txt    # 类名混淆字典
-obfuscationdictionary ConfusionDictionary.txt         # 方法/字段名混淆字典
# 字典文件示例内容：
# a
# b
# myApp
# secure
# alpha

# ==============================
# 3. 基础配置
# ==============================
#-optimizationpasses 5                  # 优化迭代次数（默认1次）
#-dontusemixedcaseclassnames            # 不使用大小写混合类名（兼容性）
#-dontskipnonpubliclibraryclasses       # 不跳过非公共库类
#-dontpreverify                         # Android不需要预校验
#-verbose                               # 输出详细日志


# ==============================
# 4. Android 关键组件保留（防止崩溃）
# ==============================
#-keep public class * extends android.app.Activity
#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider

# ==============================
# 5. 序列化相关保留
# ==============================
#-keepclassmembers class * implements java.io.Serializable {
#    static final long serialVersionUID;
#    private static final java.io.ObjectStreamField[] serialPersistentFields;
#    private void writeObject(java.io.ObjectOutputStream);
#    private void readObject(java.io.ObjectInputStream);
#    java.lang.Object writeReplace();
#    java.lang.Object readResolve();
#}

# ==============================
# 6. 反射相关保留（根据项目实际反射情况调整）
# ==============================
#-keepclassmembers class ** {
#    @android.webkit.JavascriptInterface <methods>;  # WebView JS接口
#    public *;                                      # 保留所有public方法（谨慎使用）
#}

# ==============================
# 7. 第三方库专用规则
# ==============================
# Gson
#-keepattributes Signature
#-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Retrofit
#-keep class retrofit2.** { *; }
#-dontwarn retrofit2.**

# OkHttp
#-keep class okhttp3.** { *; }
#-keep interface okhttp3.** { *; }
#-dontwarn okhttp3.**

# ==============================
# 8. 调试辅助（发布时建议移除）
# ==============================
#-renamesourcefileattribute SourceFile    # 混淆后保持源文件名（调试用）
#-keepattributes SourceFile,LineNumberTable  # 保留行号信息（调试用）

# ==============================
# 9. 允许内联短方法
# ==============================
# 允许内联短方法（默认在-optimize.txt中启用）
#在基础规则上增加优化配置，可能影响调试（建议仅 Release 使用）
#-optimizations method/inlining/short,method/inlining/unique