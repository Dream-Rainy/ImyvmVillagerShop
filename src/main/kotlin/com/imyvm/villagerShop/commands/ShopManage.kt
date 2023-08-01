package com.imyvm.villagerShop.commands

import com.imyvm.economy.EconomyMod
import com.imyvm.economy.api.TradeTypeEnum.TradeType
import com.imyvm.villagerShop.VillagerShopMain
import com.imyvm.villagerShop.VillagerShopMain.Companion.itemList
import com.imyvm.villagerShop.apis.DataBase
import com.imyvm.villagerShop.apis.Items
import com.imyvm.villagerShop.apis.ShopInfo
import com.imyvm.villagerShop.apis.Translator.tr
import com.imyvm.villagerShop.apis.checkParameterLegality
import com.imyvm.villagerShop.shops.spawnInvulnerableVillager
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.item.ItemStack
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import java.util.function.Supplier
import kotlin.math.pow

fun adminShopCreate(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    pos: BlockPos,
    args: String,
    type: Int
): Int {
    val player = context.source.player
    val worldUUID = player!!.world.registryKey.value.toString()
    val (compare,itemList) = checkParameterLegality(args)
    when (compare) {
        0 -> throw SimpleCommandExceptionType(tr("commands.shop.create.no_item")).create()
        1 -> throw SimpleCommandExceptionType(tr("commands.shop.create.count_not_equal")).create()
        else -> player.sendMessage(tr(DataBase().adminShopCreateSave(itemList, shopname, pos, player.entityName, worldUUID, type)))
    }
    if (type == 1) VillagerShopMain.itemList.addAll(itemList)
    spawnInvulnerableVillager(pos, player.world, itemList, shopname)
    return Command.SINGLE_SUCCESS
}

fun playerShopCreate(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    pos: BlockPos,
    item: ItemStackArgument,
    count: Int,
    price: Int
) {
    val player = context.source.player!!
    val inventory = player.inventory
    val sourceData = EconomyMod.data.getOrCreate(player)

    if (DataBase().dataBaseInquireByShopname(shopname,player.entityName).isNotEmpty()) {
        player.sendMessage(tr("commands.shop.create.failed.duplicate_name"))
        return
    }

    for (i in itemList) {
        if ((DataBase().stringToItem(i.item) == item.item) && i.price.toLong() <= price / count * 0.8) {
            player.sendMessage(tr("commands.shop.create.item.price.toolow",item.item.name))
            return
        }
    }

    val shopCount = DataBase().dataBaseInquireByOwner(player.entityName)
    val amount = if (shopCount.size < 3 ){
        40L
    } else {
        2.0.pow(shopCount.size.toDouble() - 1).toLong()
    }
    if (amount > sourceData.money) {
        player.sendMessage(tr("commands.shop.create.failed.lack"))
        return
    }

    val worldUUID = player.world.registryKey.value.toString()
    player.sendMessage(tr(DataBase().playerShopCreateSave(item,count,price,inventory.count(item.item),shopname,pos,player.entityName,worldUUID)))
    sourceData.addMoney(-amount, TradeType.DUTY_FREE)
    player.sendMessage(tr("commands.balance.consume", amount))
    removeItemFromInventory(player, item.item, inventory.count(item.item))
    player.sendMessage(tr("commands.stock.add.ok", inventory.count(item.item)))
    val sellItemList = mutableListOf<Items>()
    sellItemList.add(Items(item.toString(), count, price, inventory.count(item.item)))
    spawnInvulnerableVillager(pos, player.world, sellItemList, shopname)
}

fun sendMessageByType(shopInfo: ShopInfo, player: ServerPlayerEntity) {
    player.sendMessage(tr("commands.shopinfo.id", shopInfo.id))
    player.sendMessage(tr("commands.shopinfo.shopname", shopInfo.shopname))
    player.sendMessage(tr("commands.shopinfo.owner", shopInfo.owner))
    player.sendMessage(tr("commands.shopinfo.pos", BlockPos(shopInfo.posX, shopInfo.posY, shopInfo.posZ)))
}

fun rangeSearch(
    context: CommandContext<ServerCommandSource>,
    searchCondition: String,
): Int {
    val player = context.source.player!!
    val results = mutableListOf<ShopInfo>()
    for (i in searchCondition.split(" ")) {
        if (i.contains(":")) {
            val (condition, parameter) = i.split(":", limit = 2)
                val temp = when (condition) {
                    "id" -> DataBase().dataBaseInquireById(parameter.toInt())
                    "shopname" -> DataBase().dataBaseInquireByShopname(parameter)
                    "owner" -> DataBase().dataBaseInquireByOwner(parameter)
                    "location" -> DataBase().dataBaseInquireByLocation(parameter, 0, 0, 0, player.world.asString())
                    "range" -> {
                        val (rangeX,rangeY,rangeZ) = parameter.split(",").map {it.toInt()}
                        DataBase().dataBaseInquireByLocation(
                            "${player.pos.x},${player.pos.y},${player.pos.z}",
                            rangeX, rangeY, rangeZ ,player.world.asString())
                    }
                    else -> mutableListOf()
                }
            if (!results.containsAll(temp)) results.addAll(temp)
        } else {
            player.sendMessage(tr("commands.range.search.failed", i))
        }
    }
    if (results.isEmpty()) {
        player.sendMessage(tr("commands.search.none"))
        return -1
    }
    for (j in results) {
        sendMessageByType(j, player)
    }
    return Command.SINGLE_SUCCESS
}

fun shopInfo(
    context: CommandContext<ServerCommandSource>,
    id: Int
): Int {
    val player = context.source.player!!
    val results = DataBase().dataBaseInquireById(id)
    if (results.isEmpty()){
        player.sendMessage(tr("commands.search.none"))
        return -1
    }

    for (i in results) {
        sendMessageByType(i, player)
        val itemInfo = DataBase().stringToJson(i.items)
        for (j in itemInfo) {
            val item = DataBase().stringToItem(j.item)
            player.sendMessage(tr("commands.shopinfo.items", item.defaultStack.toHoverableText(), j.count, j.price, j.stock))
        }
    }
    return Command.SINGLE_SUCCESS
}

fun shopDelete(
    context: CommandContext<ServerCommandSource>,
    id: Int = -1,
    shopname: String = ""
) {
    val player = context.source.player!!
    val inventory = player.inventory
    if (id != -1){
        val textSupplier = Supplier<Text> { tr(DataBase().dataBaseDeleteById(id)) }
        context.source.sendFeedback(textSupplier,true)
    } else {
        val shopInfoList = DataBase().dataBaseInquireByShopname(shopname, player.entityName)
        val message = DataBase().dataBaseDeleteByShopname(shopname, player.entityName)
        if (message == "commands.deleteshop.ok"){
            for (i in shopInfoList){
                val itemInfo = DataBase().stringToJson(i.items)
                for (j in itemInfo){
                    val item = DataBase().stringToItem(j.item)
                    val stackToAdd = ItemStack(item,j.stock)
                    inventory.offerOrDrop(stackToAdd)
                }
            }
        }
        player.sendMessage(tr(message))
    }
}

fun shopSetAdmin(
    context: CommandContext<ServerCommandSource>,
    id: Int
) {
    if (DataBase().dataBaseChangeAdminById(id) == 1){
        val textSupplier = Supplier<Text> { tr("commands.setadmin.ok") }
        context.source.sendFeedback(textSupplier,true)
    } else {
        val textSupplier = Supplier<Text> { tr("commands.shops.none") }
        context.source.sendFeedback(textSupplier,true)
    }
}