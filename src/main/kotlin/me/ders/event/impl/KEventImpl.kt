package me.ders.event.impl

import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private val handlerRegistry = ConcurrentHashMap<Class<*>, MutableList<EventListener<Event>>>()

private val handlerNode: EventNode<Event> = EventNode.type("KEvent-Node", EventFilter.ALL).also {
    MinecraftServer.getGlobalEventHandler().addChild(it)
}

fun interface EventListener<in E : Event> : (E) -> Unit
typealias Execute<E> = context (E) () -> Unit
typealias Filter<E> = context(E) () -> Boolean

class KEvent<E : Event> private constructor (
    private val executeFunction: Execute<E>,
    private val filterFunction: Filter<E>,
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
        private var executeFunction: Execute<E> = {}
        private var filterFunction: Filter<E> = {true}
        private var asyncFunction: Continuation<E>? = null

        fun filter(lambda: Filter<E>) {
            filterFunction = lambda
        }

        fun async(continuation:  Continuation<E>) {
            asyncFunction = continuation
        }

        fun execute(lambda: Execute<E>) {
            executeFunction = lambda
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