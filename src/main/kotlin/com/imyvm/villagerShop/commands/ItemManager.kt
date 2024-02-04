package com.imyvm.villagerShop.commands

import com.imyvm.economy.EconomyMod
import com.imyvm.villagerShop.apis.*
import com.imyvm.villagerShop.apis.ModConfig.Companion.TAX_RESTOCK
import com.imyvm.villagerShop.apis.Translator.tr
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.command.ServerCommandSource
import kotlin.math.min

fun handleItemOperation(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    item: ItemStackArgument,
    count: Int = 0,
    price: Int = 0,
    operation: ItemOperation
): Int {
    val player = context.source.player
    val message = modifyItems(shopname, item, count, price, playerName = player!!.entityName, operation = operation)
    if (operation == ItemOperation.DELETE && message != "commands.shop.item.none") {
        val(temp, itemCount) = message.split(",", limit = 2)
        player.sendMessage(tr(temp))
        val inventory = player.inventory
        val stackToAdd = ItemStack(item.item, itemCount.toInt())
        inventory.offerOrDrop(stackToAdd)
        return Command.SINGLE_SUCCESS
    }
    if (operation == ItemOperation.ADD && message == "commands.shop.create.item.price.toolow") {
        player.sendMessage(tr(message,item.item?.name))
    }
    player.sendMessage(tr(message))
    if (operation == ItemOperation.ADD) {
        val inventory = player.inventory
        val itemCount = inventory.count(item.item)
        if (removeItemFromInventory(player, item.item, itemCount) == 1) {
            modifyItems(
                shopname,
                item = item,
                count = count,
                price = price,
                playerName = player.entityName,
                operation = ItemOperation.CHANGE
            )
        }
        player.sendMessage(tr("commands.stock.add.ok", itemCount))
    }

    return if (operation == ItemOperation.DELETE) 0 else if (message == "commands.playershop.item.limit") -1 else Command.SINGLE_SUCCESS
}

fun itemAdd(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    item: ItemStackArgument,
    count: Int,
    price: Int
) :Int = handleItemOperation(context, shopname, item, count, price, ItemOperation.ADD)

fun itemDelete(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    item: ItemStackArgument
) {
    handleItemOperation(context, shopname, item, operation = ItemOperation.DELETE)
}

fun itemChange(
    context: CommandContext<ServerCommandSource>,
    shopname: String,
    item: ItemStackArgument,
    count: Int,
    price: Int
) :Int = handleItemOperation(context, shopname, item, count, price, ItemOperation.CHANGE)

fun itemStockAdd(
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
    for (i in dataBaseInquireByShopname(shopname, player.entityName)) {
        val itemInfo = strungToItemsMutableList(i.items)
        for (j in itemInfo){
            if (j.item == item.asString()){
                itemCount = j.count
                itemPrice = j.price
                itemStock = j.stock
                break
            }
        }
    }
    if (itemCount == 0) {
        player.sendMessage(tr("commands.shop.item.none"))
        return -1
    }
    val amount = (itemPrice * amountToConsume * TAX_RESTOCK.value).toLong() * 100
    if (amount > sourceData.money) {
        player.sendMessage(tr("commands.shop.stock.failed.lack"))
        return -1
    }
    if (removeItemFromInventory(player, item.item, amountToConsume) == 1) {
        modifyItems(
            shopname,
            playerName = player.entityName,
            operation = ItemOperation.CHANGE,
            stock = amountToConsume + itemStock,
            item = item,
            count =itemCount,
            price = itemPrice
        )
        player.sendMessage(tr("commands.stock.add.ok", amountToConsume))
        sourceData.addMoney(-amount)
        player.sendMessage(tr("commands.balance.consume", amount / 100))
    } else {
        player.sendMessage(tr("commands.item.lack"))
        return -1
    }
    return Command.SINGLE_SUCCESS
}

fun removeItemFromInventory(player: PlayerEntity, itemToRemove: Item, quantity: Int) :Int {
    val inventory = player.inventory
    var count = quantity
    for (i in 0 until inventory.size()) {
        val currentItem = inventory.getStack(i)
        if (currentItem.item == itemToRemove) {
            val itemsToRemoveFromSlot = min(count, currentItem.count)
            currentItem.decrement(itemsToRemoveFromSlot)
            if (itemsToRemoveFromSlot == count) {
                return 1
            } else {
                count -= itemsToRemoveFromSlot
            }
        }
    }
    if ( count != 0 ){
        val stackToAdd = ItemStack(itemToRemove,quantity-count)
        inventory.offerOrDrop(stackToAdd)
        return -1
    }
    return 1
}