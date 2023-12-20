package me.ders.event.impl

import kotlinx.coroutines.*
import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerLoginEvent
import org.jetbrains.annotations.Async
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@DslMarker
annotation class KEventDsl
@KEventDsl
fun interface EventListener<in E : Event> : (E) -> Unit

private val handlerRegistry = ConcurrentHashMap<Class<*>, MutableList<EventListener<Event>>>()

private val handlerNode: EventNode<Event> = EventNode.type("KEvent-Node", EventFilter.ALL).also {
    MinecraftServer.getGlobalEventHandler().addChild(it)
}

class KEvent<E : Event> private constructor(
    private val executeFunction: (E) -> Unit,
    private val filterFunction: (E) -> Boolean,
    private var asyncFunction: Continuation<E>? = null,
    private val removeAfter: Int
) : EventListener<E> {
    private val isAsync = asyncFunction != null
    private val shouldRemove = removeAfter>=0
    private var timesRun = 0

    override fun invoke(event: E) {
        if(filterFunction.invoke(event)) {
            executeFunction.invoke(event)
            if(timesRun == 0) { if(isAsync) asyncFunction!!.resume(event); }
            if(!shouldRemove) return;

            timesRun++
            if(timesRun <= removeAfter - 1) return;

            /**
             * Assert that registry is not null because it cannot be if invoke(event) was run.
             * Avoid ConcurrentModificationException by using List<E>.removeAll
             */

            val registry = handlerRegistry[event.javaClass]!!.toMutableList()
            with(registry.iterator()) {
                forEach {
                    if(it == this@KEvent as EventListener<E>) {
                        remove()
                    }
                }
            }
            handlerRegistry[event.javaClass] = registry
        }
    }

    class Builder<E : Event>(private val eventClass: Class<E>) {
        private var executeFunction: (E) -> Unit = {  }
        private var filterFunction: (E) -> Boolean = { true }
        private var asyncFunction: Continuation<E>? = null

        var removeAfter: Int = -1
        @KEventDsl
        infix fun <E : Event> Builder<E>.execute(lambda: @KEventDsl E.() -> Unit) {
            executeFunction = lambda
        }

        @KEventDsl
        infix fun <E : Event> Builder<E>.filter(lambda: @KEventDsl E.() -> Boolean) {
            this.filterFunction = lambda
        }
        context (CoroutineScope)
        @KEventDsl
        fun async(lambda: @KEventDsl Continuation<E>) {
            this.asyncFunction = lambda
            removeAfter = 1
        }
        fun build(): KEvent<E> {
            val event = KEvent(executeFunction, filterFunction, asyncFunction, removeAfter)
            var registry = handlerRegistry[eventClass]

            if(registry.isNullOrEmpty()) registry = arrayListOf()

            registry.add(event as EventListener<Event>)
            handlerRegistry[eventClass] = registry

            if(!handlerNode.hasListener(eventClass))
                handlerNode.addListener(eventClass) {
                    handlerRegistry[it.javaClass]!!.forEach { handler -> handler.invoke(it) }
                }

            return event
        }
    }

}