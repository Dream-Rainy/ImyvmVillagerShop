package com.imyvm.villagerShop.commands

import com.imyvm.villagerShop.VillagerShopMain
import com.imyvm.villagerShop.apis.DataBase
import com.imyvm.villagerShop.apis.Translator.tr
import com.imyvm.villagerShop.apis.coroutineScope
import com.imyvm.villagerShop.apis.customScope
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg
import com.mojang.brigadier.arguments.DoubleArgumentType.getDouble
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.*
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.cancel
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.BlockPosArgumentType.blockPos
import net.minecraft.command.argument.BlockPosArgumentType.getBlockPos
import net.minecraft.command.argument.ItemStackArgumentType.getItemStackArgument
import net.minecraft.command.argument.ItemStackArgumentType.itemStack
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

data class PendingOperation(val playerUuid: UUID, val operation: () -> Unit)
val pendingOperations = ConcurrentHashMap<UUID, PendingOperation>()
fun register(dispatcher: CommandDispatcher<ServerCommandSource>,
             registryAccess: CommandRegistryAccess) {
        val builder = literal("villagershop")
        .requires(ServerCommandSource::isExecutedByPlayer)
            .then(literal("create")
                .then(literal("adminshop")
                    .requires(Permissions.require(VillagerShopMain.MOD_ID + ".admin", 3))
                    .then(argument("shopname", string())
                        .then(argument("pos", blockPos())
                            .then(argument("items", greedyString())
                                .then(argument("type", integer())
                                .executes { context ->
                                    adminShopCreate(
                                        context,
                                        getString(context, "shopname"),
                                        getBlockPos(context, "pos"),
                                        getString(context, "items"),
                                        getInteger(context, "type")
                                    )
                                }
                                )
                            )
                        )
                    )
                )
                .then(literal("shop")
                    .then(argument("shopname", string())
                        .then(argument("pos", blockPos())
                            .then(argument("item", itemStack(registryAccess))
                                .then(argument("count", integer(1))
                                    .then(argument("price", integer(1))
                                        .executes{ context ->
                                            val action = {
                                                playerShopCreate(
                                                    context,
                                                    getString(context,"shopname"),
                                                    getBlockPos(context,"pos"),
                                                    getItemStackArgument(context,"item"),
                                                    getInteger(context,"count"),
                                                    getInteger(context,"price")
                                                )
                                            }
                                            addPendingOperation(context,action)
                                            1
                                        }
                                    )
                                )
                            )
                        )
                    )
                )
            )
            .then(literal("config")
                .requires(Permissions.require(VillagerShopMain.MOD_ID + ".admin", 3))
                .then(literal("taxrate")
                    .then(literal("set")
                        .then(argument("taxrate", doubleArg())
                            .executes { context ->
                                taxRateChange(
                                    context,
                                    getDouble(context, "taxrate")
                                )
                            }
                        )
                    )
                )
                .then(literal("reload")
                    .executes{ context ->
                        reload(
                            context
                        )
                    }
                )
            )
            .then(literal("manager")
                .then(literal("info")
                    .then(literal("setshopname")
                        .then(argument("shopnameold", string())
                            .then(argument("shopnamenew", string())
                                .executes { context ->
                                    if (DataBase().dataBaseChangeShopnameByShopname(
                                            getString(context, "shopnameold"),
                                            getString(context, "shopnamenew"),
                                            context.source.player!!.entityName
                                        ) !=0 ) {
                                        context.source.player?.sendMessage(tr("commands.execute.success"))
                                    } else {
                                        context.source.player?.sendMessage(tr("commands.shops.none"))
                                    }
                                    1
                                }
                            )
                        )
                        .then(argument("id", integer())
                            .then(argument("shopnamenew", string())
                                .executes { context ->
                                    if (DataBase().dataBaseChangeShopnameById(
                                            getInteger(context, "id"),
                                            getString(context, "shopnamenew")
                                        ) !=0 ) {
                                        context.source.player?.sendMessage(tr("commands.execute.success"))
                                    } else {
                                        context.source.player?.sendMessage(tr("commands.shops.none"))
                                    }
                                    1
                                }
                            )
                        )
                    )
                    .then(literal("setshoppos")
                        .then(literal("id")
                            .then(argument("id", integer())
                                .then(argument("newshoppos", blockPos())
                                    .executes { context ->
                                        if (DataBase().dataBaseChangePosById(
                                                getInteger(context, "id"),
                                                getBlockPos(context, "newshoppos")) !=0 ) {
                                            context.source.player?.sendMessage(tr("commands.execute.success"))
                                        } else {
                                            context.source.player?.sendMessage(tr("commands.shops.none"))
                                        }
                                        1
                                    }
                                )
                            )
                        )
                        .then(literal("shopname")
                            .then(argument("shopanme", string())
                                .then(argument("newshoppos", blockPos())
                                    .executes { context ->
                                        if (DataBase().dataBaseChangePosByShopname(
                                                getString(context, "shopanme"),
                                                getBlockPos(context, "newshoppos"),
                                                context.source.player!!.entityName) !=0 ) {
                                            context.source.player?.sendMessage(tr("commands.execute.success"))
                                        } else {
                                            context.source.player?.sendMessage(tr("commands.shops.none"))
                                        }
                                        1
                                    }
                                )
                            )
                        )
                    )
                )
                .then(literal("delete")
                    .then(argument("number", integer(1))
                        .requires(Permissions.require(VillagerShopMain.MOD_ID + ".manager",2))
                        .executes { context ->
                            val action = {
                                shopDelete(
                                    context,
                                    id = getInteger(context,"number")
                                )
                            }
                            addPendingOperation(context,action)
                            1
                        }
                    )
                    .then(argument("shopname",string())
                        .executes { context ->
                            val action = {
                                shopDelete(
                                    context,
                                    shopname = getString(context,"shopname")
                                )
                            }
                            addPendingOperation(context,action)
                            1
                        }
                    )
                )
                .then(literal("search")
                    .requires(Permissions.require(VillagerShopMain.MOD_ID + ".manage",2))
                    .then(argument("searchCondition", greedyString())
                        .executes { context ->
                            rangeSearch(
                                context,
                                getString(context,"searchCondition")
                            )
                        }
                    )
                )
                .then(literal("inquire")
                    .requires(Permissions.require(VillagerShopMain.MOD_ID + ".manage",2))
                    .then(argument("number", integer(1))
                        .executes { context ->
                            shopInfo(
                                context,
                                getInteger(context,"number")
                            )
                        }
                    )
                )
                .then(literal("setAdmin")
                    .requires(Permissions.require(VillagerShopMain.MOD_ID + ".admin",3))
                    .then(argument("number", integer(1))
                        .executes { context ->
                            val action = {
                                shopSetAdmin(
                                    context,
                                    getInteger(context,"number")
                                )
                            }
                            addPendingOperation(context,action)
                            1
                        }
                    )
                )
            )
            .then(literal("item")
                .then(literal("addstock")
                    .then(argument("shopname", string())
                        .then(argument("item", itemStack(registryAccess))
                            .executes { context ->
                                itemQuantityAdd(
                                    context,
                                    getString(context,"shopname"),
                                    getItemStackArgument(context, "item"),
                                    -1
                                )
                            }
                            .then(argument("count", integer(1))
                                .executes { context ->
                                    itemQuantityAdd(
                                        context,
                                        getString(context,"shopname"),
                                        getItemStackArgument(context,"item"),
                                        getInteger(context,"count")
                                    )
                                }
                            )
                        )
                    )
                )
                .then(literal("add")
                    .then(argument("shopname", string())
                        .then(argument("item", itemStack(registryAccess))
                            .then(argument("count", integer(1))
                                .then(argument("price", integer(1))
                                    .executes { context ->
                                        itemAdd(
                                            context,
                                            getString(context,"shopname"),
                                            getItemStackArgument(context,"item"),
                                            getInteger(context,"count"),
                                            getInteger(context,"price")
                                        )
                                    }
                                )
                            )
                        )
                    )
                )
                .then(literal("delete")
                    .then(argument("shopname", string())
                        .then(argument("item", itemStack(registryAccess))
                            .executes {context ->
                                val action = {
                                    itemDelete(
                                        context,
                                        getString(context,"shopname"),
                                        getItemStackArgument(context,"item")
                                    )
                                }
                                addPendingOperation(context,action)
                                1
                            }
                        )
                    )
                )
                .then(literal("change")
                    .then(argument("shopname", string())
                        .then(argument("item", itemStack(registryAccess))
                            .then(argument("count", integer(1))
                                .then(argument("price", integer(1))
                                    .executes { context ->
                                        itemChange(
                                            context,
                                            getString(context,"shopname"),
                                            getItemStackArgument(context,"item"),
                                            getInteger(context,"count"),
                                            getInteger(context,"price")
                                        )
                                    }
                                )
                            )
                        )
                    )
                )
            )
            .then(literal("confirm")
                .executes { context ->
                    val playerUUID = context.source.player?.uuid
                    val pendingOperation = pendingOperations.remove(playerUUID)
                    if (pendingOperation != null) {
                        pendingOperation.operation()
                        val textSupplier = Supplier<Text> { tr("commands.confirm.ok") }
                        context.source.sendFeedback(textSupplier, false)
                    } else {
                        val textSupplier = Supplier<Text> { tr("commands.confirm.none") }
                        context.source.sendFeedback(textSupplier, false)
                    }
                    customScope.cancel()
                    1
                }
            )
            .then(literal("cancel")
                .executes { context ->
                    val playerUUID = context.source.player?.uuid
                    if (pendingOperations.remove(playerUUID) != null) {
                        val textSupplier = Supplier<Text> { tr("commands.cancel.ok") }
                        context.source.sendFeedback(textSupplier, false)
                    } else {
                        val textSupplier = Supplier<Text> { tr("commands.cancel.none") }
                        context.source.sendFeedback(textSupplier, false)
                    }
                    customScope.cancel()
                    1
                }
            )
    val VillagerShopCommandNode = dispatcher.register(builder)
    dispatcher.register(literal("vlsp").redirect(VillagerShopCommandNode))
}

private fun addPendingOperation(context: CommandContext<ServerCommandSource>, operation: () -> Unit) {
    val player = context.source.player!!
    val playerUUID = player.uuid
    if (pendingOperations.containsKey(playerUUID)) {
        context.source.sendError(tr("commands.confirm.already.have"))
    } else {
        pendingOperations[playerUUID] = PendingOperation(playerUUID, operation)
        player.sendMessage(tr("commands.confirm.need"))
        coroutineScope(context)
    }
}

private fun cancelOperation(playerUUID: UUID): Boolean {
    return if (pendingOperations.containsKey(playerUUID)) {
        pendingOperations.remove(playerUUID)
        true
    } else {
        false
    }
}