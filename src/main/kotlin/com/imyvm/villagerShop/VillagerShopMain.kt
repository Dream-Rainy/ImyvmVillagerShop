package com.imyvm.villagerShop

import com.imyvm.economy.EconomyMod
import com.imyvm.villagerShop.apis.*
import com.imyvm.villagerShop.apis.Translator.tr
import com.imyvm.villagerShop.commands.register
import com.imyvm.villagerShop.events.PlayerConnectCallback
import com.imyvm.villagerShop.gui.ShopGui
import com.imyvm.villagerShop.shops.spawnInvulnerableVillager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class VillagerShopMain : ModInitializer {
	override fun onInitialize() {
		CONFIG.loadAndSave()
		CommandRegistrationCallback.EVENT.register { dispatcher, commandRegistryAccess, _ ->
			register(dispatcher, commandRegistryAccess)
		}
		itemList.addAll(purchaseItemLoad())
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			ServerChunkEvents.CHUNK_LOAD.register { serverWorld, chunk ->
				val scheduledExecutorService = Executors.newScheduledThreadPool(1)
				scheduledExecutorService.schedule({
					server.execute {
						val boundingBox = Box(chunk.pos.startPos, chunk.pos.startPos.add(16, 256, 16))
						val entitiesInChunk = serverWorld.getEntitiesByType(EntityType.VILLAGER, boundingBox) { entity -> entity.commandTags.contains("VillagerShop") }
						entitiesInChunk.forEach { it.remove(Entity.RemovalReason.KILLED) }
						val worldUUID: String = serverWorld.registryKey.value.toString()
						val shopInfo = dataBaseInquireByChunk(chunk.pos, worldUUID)
						shopInfo.forEach {
							spawnInvulnerableVillager(BlockPos(it.posX, it.posY, it.posZ), serverWorld, it.shopname, it.type ,it.id.value)
						}
					}
				}, 500, TimeUnit.MILLISECONDS)
			}
		}
		UseEntityCallback.EVENT.register { player, _, _, entity, _ ->
			if (entity.commandTags.contains("VillagerShop") && player is ServerPlayerEntity && entity is VillagerEntity && !containGui(entity)) {
				ShopGui(player).open(entity)
				ActionResult.SUCCESS
			} else {
				ActionResult.PASS
			}
		}
		PlayerConnectCallback.EVENT.register { _, player ->
			var incomeTotal = 0.0
			dataBaseInquireByOwner(player.entityName).forEach {
				incomeTotal += it.income
				dataBaseChangeIncomeById(it.id.value, 0.0)
			}

			if (incomeTotal != 0.0) {
				EconomyMod.data.getOrCreate(player).addMoney((incomeTotal * 100).toLong())
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
		val itemList: MutableList<Items> = mutableListOf()
		val guiSet: ConcurrentHashMap.KeySetView<VillagerEntity, Boolean> = ConcurrentHashMap.newKeySet()
	}

	private fun purchaseItemLoad(): MutableList<Items> {
		val itemList: MutableList<Items> = mutableListOf()
		for (i in dataBaseInquireByType()) {
			itemList.addAll(strungToItemsMutableList(i.items))
		}
		return itemList
	}

	private fun containGui(villager: VillagerEntity): Boolean {
		return guiSet.contains(villager)
	}
}

