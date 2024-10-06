package com.imyvm.villagerShop.apis

import com.imyvm.economy.EconomyMod
import net.minecraft.server.network.ServerPlayerEntity

class EconomyData(player: ServerPlayerEntity) {
    val playerEconomyData = EconomyMod.data.getOrCreate(player)

    fun getMoney(): Long {
        return playerEconomyData.money
    }

    fun addMoney(money: Long) {
        playerEconomyData.addMoney(money)
    }

    fun takeMoney(money: Long) {
        playerEconomyData.addMoney(-money)
    }

    fun isLargerThanTheAmountOfMoney(money: Long): Boolean {
        return getMoney() >= money
    }
}