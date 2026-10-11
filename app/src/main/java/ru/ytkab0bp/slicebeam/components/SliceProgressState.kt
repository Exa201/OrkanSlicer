package ru.ytkab0bp.slicebeam.components

import ru.ytkab0bp.slicebeam.events.SlicingProgressEvent

/** Immutable state reducer for progress events emitted by the native slicer. */
data class SliceProgressState(
    val statusMessage: String,
    val progress: Int = 0,
    val messages: List<String> = emptyList(),
    val completed: Boolean = false,
) {
    fun apply(event: SlicingProgressEvent, localize: (String) -> String): SliceProgressState {
        val progressMessage = event.message.orEmpty()
        val localizedMessage = progressMessage.takeIf { it.isNotEmpty() }?.let(localize)
        val nextMessages = if (localizedMessage != null && localizedMessage != messages.lastOrNull()) {
            messages + localizedMessage
        } else {
            messages
        }
        return copy(
            statusMessage = localizedMessage ?: statusMessage,
            progress = event.progress.coerceIn(0, 100),
            messages = nextMessages,
            completed = event.progress >= 100,
        )
    }
}
