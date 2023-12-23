package me.ders.event.impl

import kotlinx.coroutines.*
import me.ders.event.impl.KEvent.Builder
import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@DslMarker
/**
 * Denotes that [Type][AnnotationTarget.TYPE], [Class][AnnotationTarget.CLASS], or [Function][AnnotationTarget.FUNCTION]
 * is within DSL context of [KEvent].
 *
 * @author Der_s
 */
annotation class KEventDsl

@KEventDsl
/**
 * Denotes requirement of [invoke] function for all [handlers][handlerRegistry].
 *
 * @author Der_s
 */
fun interface EventListener<in E : Event> : (E) -> Unit

private val handlerRegistry: ConcurrentHashMap<Class<*>, MutableList<EventListener<Event>>> = ConcurrentHashMap<Class<*>, MutableList<EventListener<Event>>>()

private val handlerNode: EventNode<Event> = EventNode.type("KEvent-Node", EventFilter.ALL).also {
    MinecraftServer.getGlobalEventHandler().addChild(it)
}

@KEventDsl
/**
 * Defines structure for DSL implementation of [EventNode]
 * @param E the type of event for this KEvent
 *
 * @constructor Creates a new KEvent. KEvent should avoid being created
 * within an instance of itself. Should only be constructed
 * by [Builder] or other local class.
 *
 * @property executeFunction Defines a callback to be run on [invoke].
 * @property filterFunction Defines a callback of [Boolean] to be run on [invoke] to qualify [Event] of type [E].
 * @property asyncFunction Defines an optional object of [Continuation] for suspending execution within a [CoroutineScope].
 * @property removeAfter Defines the number of times [invoke] should be run before removing from [handlerRegistry].
 *
 * @see KEvent.Builder
 *
 * @author Der_s
 */
class KEvent<E : Event> private constructor(
    private val executeFunction: (E) -> Unit,
    private val filterFunction: (E) -> Boolean,
    private val asyncFunction: Continuation<E>? = null,
    private val removeAfter: Int
) : EventListener<E> {

    /**
     * Describes if KEvent should call [resume][Continuation.resume] on [asyncFunction] after [invoking][invoke].
     *
     * @param isAsync [Boolean] denoting if [asyncFunction] is null
     *
     * @author Der_s
     */
    private val isAsync: Boolean = asyncFunction != null

    /**
     * Describes if KEvent should remove itself in the future.
     *
     * @param shouldRemove [Boolean] denoting if [removeAfter] is above or equal to zero.
     *
     * @author Der_s
     */
    private val shouldRemove: Boolean = removeAfter>=0

    /**
     * @param timesRun Counts the number of times [invoke] has been successfully run.
     *
     * @see filterFunction
     * @see shouldRemove
     *
     * @author Der_s
     */
    private var timesRun: Int = 0

    /**
     * Defines code to run when [Event] of type [E] is triggered and passed in through [handlerNode]
     *
     * @param event the [Event] of type [E] that was triggered.
     *
     * @author Der_s
     */
    override fun invoke(event: E) {
        if(!filterFunction(event)) return;

        executeFunction(event)

        if(!shouldRemove) return;

        /**
         * Assert that [asyncFunction] is not null if [isAsync] is true.
        */

        if(isAsync && timesRun++ == 0) asyncFunction!!.resume(event)

        if(timesRun <= removeAfter - 1) return;

        /**
         * Assert that registry is not null because it must be valid if invoke(event) was run.
         * Avoid ConcurrentModificationException by using Iterator
         */

        val registry = handlerRegistry[event.javaClass]!!.toMutableList()

        with(registry.iterator()) {
            forEach {

                /**
                 * Cast this@KEvent to EventListener<E> because this inherits
                 * EventListener, and function invoke(E) cannot be called unless
                 * this <E> is equal to result(s) of handlerRegistry(Class<E>)
                 */

                if(it == this@KEvent as EventListener<E>) {
                    remove()
                }
            }
        }

        handlerRegistry[event.javaClass] = registry
    }

    @KEventDsl
    /**
     * Represents a mutable builder for [KEvent] DSL.
     *
     * @property eventClass the class of the [E] which must inherit [Event].
     * @property executeFunction Defines a callback to be run on [invoke] in [KEvent].
     * @property filterFunction Defines a callback of [Boolean] to be run on [invoke] to qualify [Event] of type [E] in [KEvent].
     * @property asyncFunction Defines an optional object of [Continuation] for suspending execution within a [CoroutineScope] in [KEvent].
     *
     * @see KEvent
     *
     * @author Der_s
     */
    class Builder<E : Event>(private val eventClass: Class<E>) {
        private var executeFunction: (E) -> Unit = { }
        private var filterFunction: (E) -> Boolean = { true }
        private var asyncFunction: Continuation<E>? = null

        /**
         * Defines the number of times [Event] should be run before removing from [handlerRegistry].
         *
         * Suppresses Warning 'Property could be private' because removeAfter should not have a functional setter.
         *
         * @property removeAfter Denotes variable with type [Int] with default value of -1.
         *
         * @see timesRun
         * @see filterFunction
         * @see shouldRemove
         * @see invoke
         *
         * @author Der_s
         */
        @SuppressWarnings("WeakerAccess")
        var removeAfter: Int = -1

        /**
         * Defines an extension function on Builder to modify [executeFunction] without exposing the variable.
         *
         * Uses [extension functions](https://kotlinlang.org/docs/extensions.html) instead of class functions to ensure context within DSL is limited to the function.
         *
         * Annotates [lambda] with [KEventDsl] to provide a DSL context receiver that defines the scope of the lambda.
         *
         * @param lambda the callback function for [this] to be passed to [KEvent] when constructed using [build]
         *
         * @author Der_s
         */
        infix fun <E : Event> Builder<E>.execute(lambda: @KEventDsl E.() -> Unit) {
            executeFunction = lambda
        }

        /**
         * Defines an extension function on Builder to modify [filterFunction] without exposing the variable.
         *
         * Uses [extension functions](https://kotlinlang.org/docs/extensions.html) instead of class functions to ensure context within DSL is limited to the function.
         *
         * Annotates [lambda] with [KEventDsl] to provide a DSL context receiver that defines the scope of the lambda.
         *
         * @param lambda the callback function for [this] to be passed to [KEvent] when constructed using [build]
         *
         * @author Der_s
         */
        infix fun <E : Event> Builder<E>.filter(lambda: @KEventDsl E.() -> Boolean) {
            this.filterFunction = lambda
        }

        /**
         * Defines an extension function on Builder to modify [asyncFunction] without exposing the variable.
         *
         * Uses [context] of [CoroutineScope] to limit the scope of the function to [CoroutineScope].
         *
         * Annotates [continuation] with [KEventDsl] to provide a DSL context receiver that defines the scope.
         *
         * Sets variable [removeAfter] to 1 as async functions must be removed after [Continuation.resume] is called.
         *
         * @param continuation a Continuation to be passed to [KEvent] when constructed using [build]
         *
         * @author Der_s
         */
        context(CoroutineScope)
        fun async(continuation: @KEventDsl Continuation<E>) {
            this.asyncFunction = continuation
            removeAfter = 1
        }

        /**
         * Builds a new [KEvent] of type [E] using [asyncFunction],[executeFunction],[filterFunction], and [removeAfter]
         *
         * Adds return to [handlerRegistry] and [adds listener][EventNode.addListener] of [eventClass] if one does not already
         * exist.
         *
         * Suppresses UNCHECKED_CAST because [KEvent] always extends [EventListener] with a subtype of [Event].
         *
         * @return a new [KEvent] of type [E].
         *
         * @author Der_s
         */
        fun build(): KEvent<E> {
            val event = KEvent(executeFunction, filterFunction, asyncFunction, removeAfter)
            var registry = handlerRegistry[eventClass]

            if (registry.isNullOrEmpty()) registry = arrayListOf()

            @Suppress("UNCHECKED_CAST")
            registry.add(event as EventListener<Event>)
            handlerRegistry[eventClass] = registry

            if (!handlerNode.hasListener(eventClass))
                handlerNode.addListener(eventClass) {
                    handlerRegistry[it.javaClass]!!.forEach { handler -> handler.invoke(it) }
                }

            return event
        }
    }
}