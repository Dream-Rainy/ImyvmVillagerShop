package com.imyvm.villagerShop.apis

import com.imyvm.villagerShop.VillagerShopMain
import com.imyvm.villagerShop.apis.Translator.tr
import com.imyvm.villagerShop.commands.pendingOperations
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.*
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.TimeUnit

val coroutineContext = SupervisorJob() + Dispatchers.Default
val customScope = CoroutineScope(coroutineContext)
fun coroutineScope(context: CommandContext<ServerCommandSource>) {
    val playerUUID = context.source.player!!.uuid
    customScope.launch {
        val result = withTimeoutOrNull(TimeUnit.SECONDS.toMillis(61)) {
            delay(TimeUnit.SECONDS.toMillis(60))
            pendingOperations.remove(playerUUID)
            "Cancel ok"
        }
        if (result == null) {
            pendingOperations.remove(playerUUID)
            VillagerShopMain.LOGGER.warn("Operation auto cancel failed！")
        }
        context.source.player!!.sendMessage(tr("commands.confirm.autocancel"))
    }
}