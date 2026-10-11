package ru.ytkab0bp.eventbus

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Event(val canFireParent: Boolean = true)
