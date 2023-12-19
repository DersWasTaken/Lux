package me.ders.event.impl

import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only to be used by internal DSL structure")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class KEventInternal

private val handlerRegistry = ConcurrentHashMap<Class<*>, MutableList<EventListener<Event>>>()

private val handlerNode: EventNode<Event> = EventNode.type("KEvent-Node", EventFilter.ALL).also {
    MinecraftServer.getGlobalEventHandler().addChild(it)
}

interface EventListener<in E : Event> : (E) -> Unit

class KEvent<E : Event> private constructor (
    private val executeFunction: (E) -> Unit,
    private val filterFunction: (E) -> Boolean,
    private val asyncFunction: Continuation<E>?
) : EventListener<E> {

    private val isAsync = asyncFunction != null

    override fun invoke(event: E) {
        if(filterFunction.invoke(event)) {
            executeFunction.invoke(event)
            if(isAsync) asyncFunction!!.resume(event)
        }
    }

    class Builder<E : Event>(private val eventClass: Class<E>) {
        private var executeFunction: (E) -> Unit = {}
        private var filterFunction: (E) -> Boolean = {true}
        private var asyncFunction: Continuation<E>? = null

        fun filter(lambda: E.() -> Boolean): Builder<E> {
            filterFunction = lambda
            return this;
        }

        @KEventInternal
        fun async(continuation: Continuation<E>): Builder<E> {
            asyncFunction = continuation
            return this;
        }

        fun execute(lambda: E.() -> Unit): Builder<E> {
            executeFunction = lambda
            return this;
        }

        fun build() : KEvent<E> {
            val event = KEvent(executeFunction, filterFunction, asyncFunction)
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