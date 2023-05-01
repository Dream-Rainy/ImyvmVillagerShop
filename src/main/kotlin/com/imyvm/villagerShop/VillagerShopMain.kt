package com.imyvm.villagerShop

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import com.imyvm.villagerShop.apis.ModConfig
import com.imyvm.villagerShop.commands.register
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VillagerShopMain : ModInitializer {
	override fun onInitialize() {
		CONFIG.loadAndSave()
		CommandRegistrationCallback.EVENT.register { dispatcher, commandRegistryAccess, registrationEnvironment ->
			register(dispatcher, commandRegistryAccess, registrationEnvironment)
		}
		LOGGER.info("Imyvm Villager Shop initialized")
	}
	companion object {
		@JvmField
		val LOGGER: Logger = LoggerFactory.getLogger("imyvm-villagershop")
		const val MOD_ID = "imyvm-villagershop"
		val CONFIG: ModConfig = ModConfig()
	}
}
