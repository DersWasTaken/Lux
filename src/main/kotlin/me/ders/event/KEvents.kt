package me.ders.event

import kotlinx.coroutines.CoroutineScope
import me.ders.event.impl.KEvent
import me.ders.event.impl.KEventDsl
import net.minestom.server.event.Event
import kotlin.coroutines.suspendCoroutine

/**
 * Defines duplicate of [await] as an extension of [KEvent.Builder] and deprecates to ensure context safety within the DSL.
 *
 * @author Der_s
 */
@Deprecated(message = "Incorrect context", level = DeprecationLevel.ERROR)
inline infix fun <E : Event> KEvent.Builder<*>.await(init: @KEventDsl KEvent.Builder<E>.() -> Unit) {}

/**
 * Defines duplicate of [on] as an extension of [KEvent.Builder] and deprecates to ensure context safety within the DSL.
 *
 * @author Der_s
 */
@Deprecated(message = "Incorrect context", level = DeprecationLevel.ERROR)
inline infix fun <E : Event> KEvent.Builder<*>.on(init: @KEventDsl KEvent.Builder<E>.() -> Unit) {}

/**
 * Denotes inline function for building [KEvent] using [KEvent.Builder] without using improper class construction to comply with DSL syntax.
 *
 * Annotates [init] with [KEventDsl] to provide a DSL context receiver that defines the scope of the builder.
 *
 * @param E defines the [Event] type for KEvent.
 *
 * @param init Lambda constructor for instantiating [KEvent.Builder] class within a lambda.
 *
 * @return a new [KEvent] object.
 *
 * @author Der_s
 */
inline fun <reified E : Event> on(init: @KEventDsl KEvent.Builder<E>.() -> Unit) : KEvent<E> {
    val builder = KEvent.Builder(E::class.java)
    builder.apply(init)
    return builder.build()
}

/**
 * Denotes inline function for building [KEvent] using [KEvent.Builder] without using improper class construction to comply with DSL syntax.
 *
 * Annotates [init] with [KEventDsl] to provide a DSL context receiver that defines the scope of the builder.
 *
 * Uses [context] of [CoroutineScope] to limit the scope of the function to [CoroutineScope].
 *
 * Resumes the execution of the [CoroutineScope] using a [Continuation][kotlin.coroutines.Continuation] of type [E].
 *
 * @param E defines the [Event] type for KEvent.
 *
 * @param init Lambda constructor for instantiating [KEvent.Builder] class within a lambda.
 *
 * @return the object [E] of type [Event] that was triggered
 *
 * @author Der_s
 */
context(CoroutineScope)
suspend inline fun <reified E : Event> await(
    crossinline init: @KEventDsl KEvent.Builder<E>.() -> Unit = {}
) : E = suspendCoroutine { continuation ->
    val builder = KEvent.Builder(E::class.java)
    builder.apply(init)
    builder.async(continuation)

    builder.build()
}