# Fintage keeps the defaults of R8 and of the libraries; kotlinx.serialization, OkHttp, Coil and Media3
# bring their own rules. The models are only reached through their generated serializers.
-keepattributes *Annotation*, InnerClasses, Signature
-dontwarn org.slf4j.**
