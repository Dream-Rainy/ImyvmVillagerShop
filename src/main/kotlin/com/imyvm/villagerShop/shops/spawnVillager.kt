package com.imyvm.villagerShop.shops

import net.minecraft.entity.EntityType
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
fun spawnInvulnerableVillager(
    player: ServerPlayerEntity,pos: BlockPos
    // server: MinecraftServer
) {
    // 获取玩家所在的世界
    val world = player.world

    // 创建村民实体
    val villager = VillagerEntity(EntityType.VILLAGER, world)
    villager.setPos(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) // 设置实体位置

    // 设置无敌
    villager.isInvulnerable = true

    // 设置不计入繁殖计数
    villager.breedingAge = -1

    // 取消实体碰撞
    // villager.noClip = true

    //防止雷劈，阻止移动
    villager.addScoreboardTag("VillagerShop")

    // 在世界中生成实体
    world.spawnEntity(villager)
}