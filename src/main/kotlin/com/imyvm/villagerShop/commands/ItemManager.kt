package com.imyvm.villagerShop.commands

import com.imyvm.economy.EconomyMod
import com.imyvm.villagerShop.apis.Translator.tr
import com.imyvm.villagerShop.apis.DataBase
import com.imyvm.villagerShop.apis.ModConfig
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.server.command.ServerCommandSource
import kotlin.math.min

fun handleItemOperation(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    item: ItemStackArgument? = null,
    count: Int = 0,
    price: Int = 0,
    operation: DataBase.ItemOperation
): Int {
    val player = context.source.player
    val playerUUID = player!!.uuidAsString
    val message = DataBase().modifyItems(shopname, item, count, price, playerUUID = playerUUID, operation = operation)
    player.sendMessage(tr(message))

    return if (operation == DataBase.ItemOperation.DELETE) 0 else if (message == "commands.playershop.create.limit") -1 else Command.SINGLE_SUCCESS
}

fun itemAdd(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    item: ItemStackArgument,
    count: Int,
    price: Int
) :Int = handleItemOperation(context, shopname, item, count, price, DataBase.ItemOperation.ADD)

fun itemDelete(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    item: ItemStackArgument
) {
    handleItemOperation(context, shopname, item, operation = DataBase.ItemOperation.DELETE)
}

fun itemChange(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    item: ItemStackArgument,
    count: Int,
    price: Int
) :Int = handleItemOperation(context, shopname, item, count, price, DataBase.ItemOperation.CHANGE)

fun itemQuantityAdd(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    item: ItemStackArgument,
    count: Int
) :Int {
    val player = context.source.player!!
    val inventory = player.inventory
    val sourceData = EconomyMod.data.getOrCreate(player)
    var itemPrice = 0
    var itemCount = 0
    var itemStock = 0
    val amountToConsume = if(count == -1){
        inventory.count(item.item)
    } else {
        count
    }
    for (i in DataBase().dataBaseInquire(targetValueString = shopname)){
        val (type,data) = i.split(":", limit = 2)
            if (type == "items"){
                val itemInfo = DataBase().stringToJson(data)
                for (j in itemInfo){
                    if (j.item == item.asString()){
                        itemCount = j.count
                        itemPrice = j.price
                        itemStock = j.stock
                        break
                    }
                }
            }
    }
    val amount = (itemPrice*amountToConsume*ModConfig.TAX_RATE.value).toLong()
    if (amount > sourceData.money){
        player.sendMessage(tr("commands.shop.stock.failed.lack"))
        return -1
    }
    DataBase().modifyItems(
        shopname,
        playerUUID = player.uuidAsString,
        operation = DataBase.ItemOperation.CHANGE,
        stock = amountToConsume + itemStock,
        item = item,
        price = itemPrice,
        count =itemCount
    )
    removeItemFromInventory(player,item.item,amountToConsume)
    player.sendMessage(tr("commands.stock.add.ok"))
    sourceData.addMoney(-amount)
    player.sendMessage(tr("commands.balance.consume",amount))
    return Command.SINGLE_SUCCESS
}

fun removeItemFromInventory(player: PlayerEntity, itemToRemove: Item, quantity: Int) {
    val inventory = player.inventory
    var count = quantity
    for (i in 0 until inventory.size()) {
        val currentItem = inventory.getStack(i)
        if (currentItem.item == itemToRemove) {
            val itemsToRemoveFromSlot = min(count, currentItem.count)
            currentItem.decrement(itemsToRemoveFromSlot)
            if (itemsToRemoveFromSlot == count) {
                break
            } else {
                count -= itemsToRemoveFromSlot
            }
        }
    }
}