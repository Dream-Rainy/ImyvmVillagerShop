package com.imyvm.villagerShop.commands

import com.imyvm.economy.EconomyMod
import com.imyvm.villagerShop.apis.DataBase
import com.imyvm.villagerShop.apis.SearchOperation
import com.imyvm.villagerShop.apis.Translator.tr
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import net.minecraft.network.message.MessageType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import java.util.*
import kotlin.math.floor
import kotlin.random.Random


val itemList = mutableListOf<String>()
var itemInfo : String = ""
var flag = false
fun itemPurchaseMain(
    server: MinecraftServer
) {
    for (i in DataBase().dataBaseInquire(operation = SearchOperation.ADMIN)){
        val (type,data) = i.split(":", limit = 2)
        if (type == "items"){
            val itemInfo = DataBase().stringToJson(data)
            for (j in itemInfo){
                itemList.add("${j.item},${(j.price.toLong()/j.count.toLong())*0.8}")
            }
        }
    }
    val timer = Timer()
    val task = object : TimerTask() {
        override fun run() {
            itemInfo = itemChoose()
            flag = true
            val (itemString,_) = itemInfo.split(",")
            server.playerManager.broadcast(tr("commands.sell.start", DataBase().stringToItem(itemString).name),MessageType.SYSTEM)
            val task = object : TimerTask() {
                override fun run() {
                    server.playerManager.broadcast(tr("commands.sell.finish"),MessageType.SYSTEM)
                    flag = false
                }
            }
            val delay = 5 * 60 * 1000L
            timer.schedule(task, delay)
        }
    }
    val delay = 30 * 60 * 1000L
    timer.scheduleAtFixedRate(task, delay, delay)
}
fun itemPurchase(
    context: CommandContext<ServerCommandSource>,
    count: Int
    ) :Int {
    val player = context.source.player!!
    if (flag){
        val inventory = player.inventory
        val sourceData = EconomyMod.data.getOrCreate(player)
        val (itemString,price) = itemInfo.split(",")
        val item = DataBase().stringToItem(itemString)
        val amountToConsume = if(count == -1){
            inventory.count(item)
        } else {
            count
        }
        val amount = if (floor(price.toDouble()).toInt() < 1) amountToConsume.toLong() else floor(price.toDouble()).toLong()*amountToConsume
        if (removeItemFromInventory(player,item,amountToConsume)==1){
            player.sendMessage(tr("commands.sell.ok",amountToConsume))
            sourceData.addMoney(amount)
            player.sendMessage(tr("commands.balance.add", amount))
        } else {
            player.sendMessage(tr("commands.item.lack"))
            return -1
        }
    } else {
        player.sendMessage(tr("commands.sell.none"))
        return -1
    }
    return Command.SINGLE_SUCCESS
}
fun itemChoose(): String {
    val randomIndex = Random.nextInt(itemList.size)
    return itemList[randomIndex]
}