# Saved config is decoded with kotlinx.serialization; keep the generated serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.unscroll.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.unscroll.**$$serializer { *; }
