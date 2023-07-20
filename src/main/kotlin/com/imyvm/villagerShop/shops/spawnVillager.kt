package com.imyvm.villagerShop.shops

import com.imyvm.villagerShop.apis.Items
import net.minecraft.entity.EntityType
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun spawnInvulnerableVillager(
    pos: BlockPos,world: World,
    sellItemList: MutableList<Items>,
    shopname: String,
    type: Int = 0
) {
    val villager = VillagerEntity(EntityType.VILLAGER, world)
    villager.setPos(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
    villager.isInvulnerable = true
    villager.breedingAge = -1
    villager.isBaby = false
    villager.horizontalCollision = false
    villager.addCommandTag("VillagerShop")
    villager.customName = Text.of(shopname)
    world.spawnEntity(villager)
}