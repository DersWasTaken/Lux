package me.ders.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import me.ders.event.impl.KEvent
import me.ders.event.impl.KEventDsl
import net.minestom.server.event.Event
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine

@Deprecated(message = "Incorrect context", level = DeprecationLevel.ERROR)
fun <E : Event> KEvent.Builder<*>.await(init: @KEventDsl KEvent.Builder<E>.() -> Unit) {}

@Deprecated(message = "Incorrect context", level = DeprecationLevel.ERROR)
fun <E : Event> KEvent.Builder<*>.on(init: @KEventDsl KEvent.Builder<E>.() -> Unit) {}

inline fun <reified E : Event> on(init: @KEventDsl KEvent.Builder<E>.() -> Unit) : KEvent<E> {
    val builder = KEvent.Builder<E>(E::class.java)
    builder.apply(init)
    return builder.build()
}
context(CoroutineScope)
suspend inline fun <reified E : Event> await(
    crossinline init: @KEventDsl KEvent.Builder<E>.() -> Unit = {}
) : E = suspendCoroutine { continuation ->
    val builder = KEvent.Builder<E>(E::class.java)
    builder.apply(init)
    builder.async(continuation)

    builder.build()
}