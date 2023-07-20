package com.imyvm.villagerShop

import com.imyvm.villagerShop.apis.DataBase
import com.imyvm.villagerShop.apis.Items
import com.imyvm.villagerShop.apis.ModConfig
import com.imyvm.villagerShop.apis.ModConfig.Companion.TAX_RESTOCK
import com.imyvm.villagerShop.commands.register
import com.imyvm.villagerShop.shops.spawnInvulnerableVillager
import kotlinx.serialization.json.Json
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VillagerShopMain : ModInitializer {
	override fun onInitialize() {
		CONFIG.loadAndSave()
		CommandRegistrationCallback.EVENT.register { dispatcher, commandRegistryAccess, _ ->
			register(dispatcher, commandRegistryAccess)
		}
		itemList.addAll(purchaseItemLoad())
		TradeType.STOCK.tax = TAX_RESTOCK.value
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			ServerChunkEvents.CHUNK_LOAD.register { serverWorld,chunk ->
				val scheduledExecutorService = Executors.newScheduledThreadPool(1)
				scheduledExecutorService.schedule({
					server.execute {
						val boundingBox = Box(chunk.pos.startPos, chunk.pos.startPos.add(16, 256, 16))
						val entitiesInChunk = serverWorld.getEntitiesByType(EntityType.VILLAGER, boundingBox) {entity -> entity.commandTags.contains("VillagerShop")}
						entitiesInChunk.forEach { it.remove(Entity.RemovalReason.KILLED) }
						val worldUUID: String = serverWorld.registryKey.value.toString()
						val shopInfo = DataBase().dataBaseInquireByLocation(
							chunk.pos.centerX.toString() + ",0," + chunk.pos.centerZ,
							8, 32767, 8 , worldUUID
						)
						for (i in shopInfo) {
							val sellItemList = mutableListOf<Items>(Json.decodeFromString(i.items))
							val shopName = i.shopname
							spawnInvulnerableVillager(BlockPos(i.posX, i.posY, i.posZ), serverWorld, sellItemList, shopName)
						}
					}
				}, 500, TimeUnit.MILLISECONDS)
			}
		}
		LOGGER.info("Imyvm Villager Shop initialized")
	}
	companion object {
		@JvmField
		val LOGGER: Logger = LoggerFactory.getLogger("Imyvm-VillagerShop")
		const val MOD_ID = "imyvm_villagershop"
		val CONFIG: ModConfig = ModConfig()
		val itemList: MutableList<Items> = mutableListOf()
	}
	private fun purchaseItemLoad(): MutableList<Items> {
		val itemList: MutableList<Items> = mutableListOf()
		for (i in DataBase().dataBaseInquireByType()) {
			itemList.addAll(DataBase().stringToJson(i.items))
		}
		return itemList
	}
}
