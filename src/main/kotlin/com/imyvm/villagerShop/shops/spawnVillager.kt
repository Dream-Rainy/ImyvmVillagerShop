package com.imyvm.villagerShop.shops

import net.minecraft.entity.EntityType
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World


fun spawnInvulnerableVillager(
    pos: BlockPos,world: World
) {
    val villager = VillagerEntity(EntityType.VILLAGER, world)
    villager.setPos(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) // 设置实体位置
    villager.isInvulnerable = true
    villager.breedingAge = -1
    villager.addScoreboardTag("VillagerShop")
    world.spawnEntity(villager)
}