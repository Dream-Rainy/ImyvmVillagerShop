package com.imyvm.villagerShop

import com.imyvm.economy.EconomyMod
import com.imyvm.villagerShop.apis.DataBase
import com.imyvm.villagerShop.apis.ModConfig
import com.imyvm.villagerShop.apis.ModConfig.Companion.ADMIN_NAME
import com.imyvm.villagerShop.apis.SearchOperation
import com.imyvm.villagerShop.apis.Translator.tr
import com.imyvm.villagerShop.commands.itemPurchaseMain
import com.imyvm.villagerShop.commands.register
import com.imyvm.villagerShop.shops.spawnInvulnerableVillager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Box
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import java.io.File
import java.io.FileWriter

class VillagerShopMain : ModInitializer {
	override fun onInitialize() {
		CONFIG.loadAndSave()
		CommandRegistrationCallback.EVENT.register { dispatcher, commandRegistryAccess, _ ->
			register(dispatcher, commandRegistryAccess)
		}
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			itemPurchaseMain(server)
			ServerChunkEvents.CHUNK_LOAD.register { serverWorld,chunk ->
				val scheduledExecutorService = Executors.newScheduledThreadPool(1)
				scheduledExecutorService.schedule({
					server.execute {
						val boundingBox = Box(chunk.pos.startPos, chunk.pos.startPos.add(16, 256, 16))
						val entitiesInChunk = serverWorld.getEntitiesByType(EntityType.VILLAGER,boundingBox) {entity -> entity.scoreboardTags.contains("VillagerShop")}
						entitiesInChunk.forEach { it.remove(Entity.RemovalReason.KILLED) }
						val worldUUID: String = serverWorld.registryKey.value.toString()
						val shopInfo = DataBase().dataBaseInquire(
							targetValueString = chunk.pos.centerX.toString() + ",0," + chunk.pos.centerZ,
							rangeX = 8, rangeY = 32767, rangeZ = 8, operation = SearchOperation.LOCATION, world = worldUUID
						)
						for (i in shopInfo) {
							val (type,value) = i.split(":")
							if (type == "pos") {
								spawnInvulnerableVillager(DataBase().stringToBlockPos(value), serverWorld)
							}
						}
					}
				}, 500, TimeUnit.MILLISECONDS)
			}
			ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
				if (handler.player.name.toString() == ADMIN_NAME.value) {
					LOGGER.info(handler.player.entityName)
					ADMIN = handler.player
					val file = File("../world/tax.txt")
					val amount = file.readText().toLong()
					val scheduledExecutorService = Executors.newScheduledThreadPool(1)
					scheduledExecutorService.schedule({
						server.execute {
							ADMIN?.let { admin ->
								val fileWriter = FileWriter("../world/tax.txt", false)
								val bufferedWriter = BufferedWriter(fileWriter)
								val adminData = EconomyMod.data.getOrCreate(admin)
								adminData.addMoney(amount)
								admin.sendMessage(tr("admin.tax.offline",amount))
								bufferedWriter.write("0")
							}
						}
					},2,TimeUnit.SECONDS)
				}
			}
			ServerPlayConnectionEvents.DISCONNECT.register { handler,_ ->
				if (handler.player.name.toString() == ADMIN_NAME.value) {
					ADMIN = null
				}
			}
		}
		LOGGER.info("Imyvm Villager Shop initialized")
	}
	companion object {
		@JvmField
		val LOGGER: Logger = LoggerFactory.getLogger("Imyvm-VillagerShop")
		const val MOD_ID = "imyvm_villagershop"
		val CONFIG: ModConfig = ModConfig()
		var ADMIN: ServerPlayerEntity? =null
	}
}
