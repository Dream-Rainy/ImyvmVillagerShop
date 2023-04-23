package com.imyvm.villagerShop

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import com.imyvm.villagerShop.apis.ModConfig
import com.imyvm.villagerShop.commands.register
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VillagerShopMain : ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	override fun onInitialize() {
		CONFIG.loadAndSave()
		CommandRegistrationCallback.EVENT.register { dispatcher, commandRegistryAccess, registrationEnvironment ->
			register(dispatcher, commandRegistryAccess, registrationEnvironment)
		}
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		LOGGER.info("Imyvm Villager Shop initialized")
	}
	companion object {
		@JvmField
		val LOGGER: Logger = LoggerFactory.getLogger("imyvm-villagershop")
		const val MOD_ID = "imyvm-villagershop"
		val CONFIG: ModConfig = ModConfig()
	}
}
