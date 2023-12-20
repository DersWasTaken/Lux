package me.ders

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.ders.event.await
import me.ders.event.on
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    val server = MinecraftServer.init()

    server.start("0.0.0.0", 25565)

    val instanceManager = MinecraftServer.getInstanceManager()

    val instanceContainer = instanceManager.createInstanceContainer()

    instanceContainer.setGenerator { unit: GenerationUnit ->
        unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK)
    }

    instanceContainer.setChunkSupplier { instance ,chunkX, chunkZ ->
        LightingChunk(instance,chunkX,chunkZ)
    }

    on<PlayerLoginEvent> {
        execute {
            setSpawningInstance(instanceContainer)
            player.respawnPoint = Pos(0.0, 42.0, 0.0)
            player.gameMode = GameMode.CREATIVE
        }
    }

    GlobalScope.launch {
        println("waiting")
        val event = await<PlayerLoginEvent>() {

        }
        println("continuing")
        event.player.sendMessage("HELLO!")
    }

}