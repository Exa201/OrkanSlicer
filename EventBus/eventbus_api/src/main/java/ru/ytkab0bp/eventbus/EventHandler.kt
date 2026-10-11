package ru.ytkab0bp.eventbus

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class EventHandler(val runOnMainThread: Boolean = false)
