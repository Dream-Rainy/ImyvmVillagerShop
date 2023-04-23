package com.imyvm.villagerShop.commands

import com.imyvm.villagerShop.VillagerShopMain.Companion.CONFIG
import com.imyvm.villagerShop.apis.ModConfig.Companion.TAX_RATE
import com.imyvm.villagerShop.apis.Translator.tr
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.ServerCommandSource

fun taxRateChange(context: CommandContext<ServerCommandSource>, taxrate:Double):Int {
    TAX_RATE.setValue(taxrate)
    CONFIG.loadAndSave()
    context.source.sendFeedback(tr("commands.imyvm_villager_shop.tax_change.success"), true)
    return Command.SINGLE_SUCCESS
}
fun reload(context: CommandContext<ServerCommandSource>): Int {
    CONFIG.loadAndSave()
    context.source.sendFeedback(tr("commands.imyvm_villager_shop.reload.success"), true)
    return Command.SINGLE_SUCCESS
}