package com.imyvm.villagerShop.commands

import com.imyvm.economy.EconomyMod
import com.imyvm.economy.Translator
import com.imyvm.villagerShop.apis.DataBase
import com.imyvm.villagerShop.apis.DataSaveOperation
import com.imyvm.villagerShop.apis.Translator.tr
import com.imyvm.villagerShop.apis.checkParameterLegality
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import java.util.*

fun adminShopCreate(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    pos: BlockPos,
    args: String
): Int {
    val player = context.source.player
    val (compare,itemList) = checkParameterLegality(args)
    when (compare) {
        0 -> throw SimpleCommandExceptionType(tr("commands.shop.create.no_item")).create()
        1 -> throw SimpleCommandExceptionType(tr("commands.shop.create.count_not_equal")).create()
        else -> player!!.sendMessage(tr(DataBase().adminShopCreateSave(itemList,shopname,pos,player.uuidAsString)))
    }
    return Command.SINGLE_SUCCESS
}

fun playerShopCreate(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    pos: BlockPos,
    item: ItemStackArgument,
    count: Int,
    price: Int,
) {
    val player = context.source.player
    val sourceData = EconomyMod.data.getOrCreate(player)
    val shopCount = DataBase().dataBaseInquire(targetString = DataBase.Shops.owner,targetValueString = player!!.uuidAsString)
    for (i in shopCount){
        val (type,data) = i.split(":")
        if (type == "shopname" && data == shopname){
            player.sendMessage(tr("commands.shop.create.failed.duplicate_name"))
            return
        }
    }
    val amount = if (shopCount.size <3 ){
        40L
    } else {
        Math.pow(2.0, shopCount.size.toDouble()-1).toLong()
    }
    if (amount > sourceData.money) {
        player.sendMessage(tr("commands.shop.create.failed.lack"))
        return
    }
    player.sendMessage(tr(DataBase().playerShopCreateSave(item,count,price,shopname,pos,player.uuidAsString)))
    sourceData.addMoney(-amount)
    player.sendMessage(tr("commands.balance.consume",amount))
}

fun sendMessageByType(type: String, data: String, player: ServerPlayerEntity, server: MinecraftServer) {
    when (type) {
        "id" -> player.sendMessage(tr("commands.shopinfo.id", data))
        "shopname" -> player.sendMessage(tr("commands.shopinfo.shopname", data))
        "pos" -> player.sendMessage(tr("commands.shopinfo.pos", data))
        "ownerUUID" -> {
            val uuid = UUID.fromString(data)
            val playername = server.playerManager.getPlayer(uuid)
            player.sendMessage(tr("commands.shopinfo.owner", playername?.entityName))
        }
    }
}

fun rangeSearch(
    context: CommandContext<ServerCommandSource>,
    searchCondition: String,
): Int {
    val player = context.source.player!!
    val server = context.source.server!!

    for (i in searchCondition.split(" ")) {
        if (i.contains(":")) {
            val (condition, parameter) = i.split(":", limit = 2)
            val results = when (condition) {
                "id" -> DataBase().dataBaseInquire(targetInt = DataBase.Shops.id, targetValueInt = parameter.toInt())
                else -> DataBase().dataBaseInquire(targetString = DataBase.Shops.shopname, targetValueString = parameter)
            }
            if (results.isEmpty()){
                player.sendMessage(tr("commands.search.none"))
                return -1
            }

            for (j in results) {
                val (type, data) = j.split(":", limit = 2)
                sendMessageByType(type, data, player, server)
            }
        } else {
            player.sendMessage(tr("commands.range.search.failed"))
            return -1
        }
    }
    return Command.SINGLE_SUCCESS
}

fun shopInfo(
    context: CommandContext<ServerCommandSource>,
    registryAccess: CommandRegistryAccess,
    number: Int
): Int {
    val player = context.source.player!!
    val server = context.source.server!!
    val results = DataBase().dataBaseInquire(targetInt = DataBase.Shops.id, targetValueInt = number)
    if (results.isEmpty()){
        player.sendMessage(tr("commands.search.none"))
        return -1
    }

    for (i in results) {
        val (type, data) = i.split(":", limit = 2)
        if (type == "items") {
            val itemInfo = DataBase().stringToJson(data)
            for (j in itemInfo) {
                val item = DataBase().stringToItemStackArgument(j.item, registryAccess)
                player.sendMessage(tr("commands.shopinfo.items", item.item, j.count, j.price, j.stock))
            }
        } else {
            sendMessageByType(type, data, player, server)
        }
    }
    return Command.SINGLE_SUCCESS
}

fun shopDelete(
    context: CommandContext<ServerCommandSource>,
    number: Int = -1,
    shopname: String = ""
) {
    val player = context.source.player!!
    val uuid = context.source.player!!.uuidAsString
    if (number != -1){
        player.sendMessage(tr(DataBase().dataBaseDelete(uuid = uuid, targetValueInt = number)))
    } else {
        player.sendMessage(tr(DataBase().dataBaseDelete(uuid = uuid, targetValueString = shopname)))
    }
}

fun shopSetAdmin(
    context: CommandContext<ServerCommandSource>,
    number: Int
){
    val player = context.source.player!!
    if (DataBase().dataBaseChange(targetValueInt = number, operation = DataSaveOperation.ADMIN) == 1){
        context.source.sendFeedback(tr("commands.setadmin.ok"),true)
    } else {
        context.source.sendFeedback(tr("commands.shops.none"),true)
    }
}

fun shopInfoChange(
    context: CommandContext<ServerCommandSource>,
    infoname: String,
    shopnameNew: String = "",
    shopname: String = "",
    blockPos: BlockPos = BlockPos(0,0,0)
) :Int {
    val player = context.source.player!!
    val playerUUID = player.uuidAsString
    val result = when (infoname){
        "shopname" -> DataBase().dataBaseChange(targetValue = shopname, shopNameNew = shopnameNew, playerUUID = playerUUID, operation = DataSaveOperation.SHOPNAME)
        "pos" -> DataBase().dataBaseChange(targetValue = shopname, blockPos = blockPos, playerUUID = playerUUID, operation = DataSaveOperation.POS)
        else -> 114514
    }
    if (result == -1){
        player.sendMessage(tr("commands.shops.none"))
    } else {
        player.sendMessage(tr("commands.shopinfo.change.success"))
    }
    return Command.SINGLE_SUCCESS
}