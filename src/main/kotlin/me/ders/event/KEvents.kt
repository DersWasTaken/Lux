package me.ders.event

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import me.ders.event.impl.KEvent
import me.ders.event.impl.KEventInternal
import net.minestom.server.event.Event
import kotlin.coroutines.suspendCoroutine

inline fun <reified E : Event> on(init: KEvent.Builder<E>.() -> Unit) {
    KEvent.Builder(E::class.java).apply(init).build()
}

@OptIn(DelicateCoroutinesApi::class, KEventInternal::class)
suspend inline fun <reified E : Event> await(
    noinline init: (KEvent.Builder<E>.() -> Unit) = {}
): E {
    return GlobalScope.async {
        return@async suspendCoroutine<E> { continuation ->
            val builder = KEvent.Builder(E::class.java)
            builder.apply(init)
            builder.async(continuation)

            builder.build()
        }
    }.await()
}