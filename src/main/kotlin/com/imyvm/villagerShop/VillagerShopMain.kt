package com.imyvm.villagerShop

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import com.imyvm.villagerShop.apis.ModConfig
import com.imyvm.villagerShop.commands.register
import com.imyvm.villagerShop.commands.itemPurchaseMain
import com.imyvm.villagerShop.shops.spawnInvulnerableVillager
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VillagerShopMain : ModInitializer {
	override fun onInitialize() {
		CONFIG.loadAndSave()
		CommandRegistrationCallback.EVENT.register { dispatcher, commandRegistryAccess, registrationEnvironment ->
			register(dispatcher, commandRegistryAccess, registrationEnvironment)
		}
		itemPurchaseMain()
		LOGGER.info("Imyvm Villager Shop initialized")
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			println("服务器已启动: ${server.worlds}")
		}
	}
	companion object {
		@JvmField
		val LOGGER: Logger = LoggerFactory.getLogger("imyvm-villagershop")
		const val MOD_ID = "imyvm_villagershop"
		val CONFIG: ModConfig = ModConfig()
	}
}
