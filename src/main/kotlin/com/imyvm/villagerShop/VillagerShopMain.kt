package com.imyvm.villagerShop

import com.imyvm.villagerShop.apis.EconomyData
import com.imyvm.villagerShop.apis.ModConfig
import com.imyvm.villagerShop.apis.ShopService
import com.imyvm.villagerShop.apis.ShopService.Companion.resetRefreshableSellAndBuy
import com.imyvm.villagerShop.apis.Translator.tr
import com.imyvm.villagerShop.commands.register
import com.imyvm.villagerShop.events.PlayerConnectCallback
import com.imyvm.villagerShop.gui.ShopGui
import com.imyvm.villagerShop.items.ItemManager
import com.imyvm.villagerShop.shops.ShopEntity.Companion.shopDBService
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.entity.Entity
import net.minecraft.entity.Entity.RemovalReason
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.math.Box
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class VillagerShopMain : ModInitializer {
	val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
	override fun onInitialize() {
		CONFIG.loadAndSave()
		CommandRegistrationCallback.EVENT.register { dispatcher, commandRegistryAccess, _ ->
			register(dispatcher, commandRegistryAccess)
		}
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			itemList.addAll(purchaseItemLoad(server))
			scheduleDailyTask(server.registryManager)
			ServerChunkEvents.CHUNK_LOAD.register { serverWorld, chunk ->
				val xStart = chunk.pos.startX
				val zStart = chunk.pos.startZ
				val xEnd = chunk.pos.endX
				val zEnd = chunk.pos.endZ

				val chunkBox = Box(xStart.toDouble(), 0.0, zStart.toDouble(), xEnd.toDouble(), serverWorld.height.toDouble(), zEnd.toDouble())

				serverWorld.getEntitiesByClass(VillagerEntity::class.java, chunkBox, Entity::isAlive).forEach {
					if (it.commandTags.contains("VillagerShop")) {
						val id = it.commandTags.firstOrNull { it.startsWith("id:") }?.split(":")?.getOrNull(1)?.toIntOrNull() ?: -1
						it.remove(RemovalReason.KILLED)
						if (id != -1) {
							val shopEntity = shopDBService.readById(id, serverWorld.registryManager)
							shopEntity?.spawnOrRespawn(serverWorld)
						}
					}
				}
			}
		}
		ServerLifecycleEvents.SERVER_STOPPED.register {
			scheduler.shutdownNow()
		}
		UseEntityCallback.EVENT.register { player, world, _, entity, _ ->
			if (entity.commandTags.contains("VillagerShop") && player is ServerPlayerEntity && entity is VillagerEntity && !containGui(entity)) {
				ShopGui(player, world.registryManager).open(entity)
				ActionResult.SUCCESS
			} else {
				ActionResult.PASS
			}
		}
		PlayerConnectCallback.EVENT.register { _, player ->
			var incomeTotal = 0.0
			shopDBService.readByOwner(player.nameForScoreboard, player.registryManager).forEach {
				incomeTotal += it.income
				it.income = 0.0
				shopDBService.update(it)
			}

			if (incomeTotal != 0.0) {
				EconomyData(player).addMoney((incomeTotal * 100).toLong())
				player.sendMessage(tr("commands.balance.add", incomeTotal.toLong()))
			}
		}
		LOGGER.info("Imyvm Villager Shop initialized")
	}

	companion object {
		@JvmField
		val LOGGER: Logger = LoggerFactory.getLogger("Imyvm-VillagerShop")
		const val MOD_ID = "imyvm_villagershop"
		val CONFIG: ModConfig = ModConfig()
		val itemList: MutableList<ItemManager> = mutableListOf()
		val guiSet: ConcurrentHashMap.KeySetView<VillagerEntity, Boolean> = ConcurrentHashMap.newKeySet()
	}

	private fun purchaseItemLoad(server: MinecraftServer): MutableList<ItemManager> {
		val itemList: MutableList<ItemManager> = mutableListOf()
		for (i in shopDBService.readByType(server.registryManager,
			listOf(ShopService.Companion.ShopType.REFRESHABLE_BUY, ShopService.Companion.ShopType.UNLIMITED_BUY))
		) {
			itemList.addAll(i.items)
		}
		return itemList
	}

	private fun containGui(villager: VillagerEntity): Boolean {
		return guiSet.contains(villager)
	}

	private fun scheduleDailyTask(registries: RegistryWrapper.WrapperLookup) {

		val midnightTask = Runnable {
			// Reset daily limit
			resetRefreshableSellAndBuy(registries)
		}

		val now = LocalDateTime.now()
		val nextMidnight = now.plusDays(1).toLocalDate().atStartOfDay()
		val delayMillis = Duration.between(now, nextMidnight).toMillis()

		scheduler.scheduleAtFixedRate(midnightTask, delayMillis, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS)
	}
}

