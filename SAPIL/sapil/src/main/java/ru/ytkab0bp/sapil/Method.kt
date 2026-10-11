package ru.ytkab0bp.sapil

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Method(
    val value: String,
    val requestType: RequestType = RequestType._DEFAULT,
    val data: String = ""
)
