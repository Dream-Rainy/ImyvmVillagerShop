package com.imyvm.villagerShop.apis

import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_PASSWORD
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_URL
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_USER
import com.mojang.brigadier.StringReader
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.command.argument.ItemStackArgumentType
import net.minecraft.util.math.BlockPos
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

enum class ItemOperation {
    ADD, DELETE, CHANGE
}
enum class DataSaveOperation {
    SHOPNAME,ITEM,ADMIN,POS
}
@Serializable
data class Items(val item: String, val count: Int, val price: Int, val stock: Int = 0)

class DataBase {
    private val serializer = ListSerializer(Items.serializer())
    object DbSettings {
        val db by lazy {
            Database.connect(
                DATABASE_URL.value,
                driver = "org.postgresql.Driver",
                user = DATABASE_USER.value,
                password = DATABASE_PASSWORD.value
            )
        }
    }

    object Shops : IntIdTable() {
        val shopname = varchar("shopname", 20)
        val owner = varchar("owner", 40)
        val admin = integer("admin")
        val pos = varchar("pos", 255)
        val items = text("items")
    }

    fun adminShopCreateSave(
        sellItemList: MutableList<Items>,
        shopname: String,
        pos: BlockPos,
        uuid: String
    ): String {
        dataBaseSave(shopname, blockPosToString(pos), Json.encodeToString(serializer,sellItemList), uuid,1)
        return "commands.shop.create.success"
    }

    fun playerShopCreateSave(
        item: ItemStackArgument,
        count: Int,
        price: Int,
        shopname: String,
        pos: BlockPos,
        uuid: String
    ): String {
        val sellItemList = mutableListOf(Items(item.asString(),count,price))
        dataBaseSave(shopname, blockPosToString(pos), Json.encodeToString(serializer,sellItemList), uuid)
        return "commands.shop.create.success"
    }

    fun modifyItems(
        shopname: String,
        item: ItemStackArgument? = null,
        count: Int = 0,
        price: Int = 0,
        stock: Int = 0,
        playerUUID: String,
        operation: ItemOperation
    ): String {
        val sellItemList = dataBaseInquire(targetValueString = shopname)
        val sellItemListNew = mutableListOf<Items>()
        var itemCount = 0

        for (i in sellItemList) {
            val (type, data) = i.split(":",limit = 2)
            if (type == "items") {
                val currentItem = stringToJson(data)

                when (operation) {
                    ItemOperation.ADD -> {
                        itemCount = currentItem.size
                        sellItemListNew.addAll(currentItem)
                    }
                    ItemOperation.DELETE -> {
                        for (j in currentItem){
                            if (j.item != item?.asString()) {
                                sellItemListNew.add(j)
                            }
                        }
                    }
                    ItemOperation.CHANGE -> {
                        for (j in currentItem){
                            if (j.item == item?.asString()) {
                                sellItemListNew.add(Items(item.asString(), count, price, stock))
                            } else {
                                sellItemListNew.add(j)
                            }
                        }
                    }
                }
            }
        }

        if (operation == ItemOperation.ADD) {
            if (itemCount >= 7) {
                return "commands.playershop.create.limit"
            } else if (sellItemListNew.firstOrNull { it.item == item?.asString() } != null) {
                return "commands.playershop.add.repeat"
            } else {
                sellItemListNew.add(Items(item!!.asString(), count, price))
            }
        }

        if (dataBaseChange(Shops.shopname, shopname, sellItemListNew, playerUUID = playerUUID, operation = DataSaveOperation.ITEM)==-1) {
            return "commands.shops.none"
        }

        return when (operation) {
            ItemOperation.ADD -> "commands.shop.item.add.success"
            ItemOperation.DELETE -> "commands.shop.item.delete.success"
            ItemOperation.CHANGE -> "commands.shop.item.change.success"
        }
    }

    private fun dataBaseSave(shopname: String, pos: String, items: String, owner: String, admin: Int = 0) {
        transaction(DbSettings.db) {
            SchemaUtils.create(Shops)
            Shops.insert {
                it[Shops.shopname] = shopname
                it[Shops.pos] = pos
                it[Shops.items] = items
                it[Shops.owner] = owner
                it[Shops.admin] = admin
            }get Shops.id
        }
    }

    fun dataBaseInquire(
        targetString: Column<String> = Shops.shopname, targetValueString: String = "",
        targetInt: Column<EntityID<Int>> = Shops.id, targetValueInt: Int = -1): MutableList<String> {
        val shopInfo = mutableListOf<String>()
        fun processRow(row: ResultRow) {
            shopInfo.apply {
                add("id:" + row[Shops.id])
                add("shopname:" + row[Shops.shopname])
                add("pos:" + stringToBlockPos(row[Shops.pos]))
                add("admin:" + row[Shops.admin])
                add("ownerUUID:" + row[Shops.owner])
                add("items:" + row[Shops.items])
            }
        }

        transaction(DbSettings.db) {
            SchemaUtils.create(Shops)
            if (targetValueString != "") {
                Shops.select { targetString eq targetValueString }.forEach(::processRow)
            } else {
                Shops.select { targetInt eq targetValueInt }.forEach(::processRow)
            }
        }
        return shopInfo
    }

    fun dataBaseChange(
        target: Column<String> = Shops.shopname,
        targetValue: String = "",
        sellItemListNew: MutableList<Items> = mutableListOf(),
        shopNameNew: String = "",
        targetInt: Column<EntityID<Int>> = Shops.id,
        targetValueInt: Int = -1,
        playerUUID: String = "",
        blockPos: BlockPos = BlockPos(0, 0, 0),
        operation: DataSaveOperation
    ): Int {
        val condition: Op<Boolean> = when {
            targetInt == Shops.id -> { targetInt eq targetValueInt }
            blockPos != BlockPos(0, 0, 0) -> { Shops.pos eq blockPosToString(blockPos) and (Shops.owner eq playerUUID) }
            else -> { target eq targetValue and (Shops.owner eq playerUUID) }
        }

        val numberOfRowsUpdated = transaction(DbSettings.db) {
            SchemaUtils.create(Shops)
            Shops.update({condition}) {
                when (operation) {
                    DataSaveOperation.SHOPNAME -> it[shopname] = shopNameNew
                    DataSaveOperation.ITEM -> it[items] = Json.encodeToString(serializer, sellItemListNew)
                    DataSaveOperation.ADMIN -> it[admin] = 1
                    DataSaveOperation.POS -> it[pos] = blockPosToString(blockPos)
                }
            }
        }
        return when (numberOfRowsUpdated) {
            0 -> -1
            else -> 1
        }
    }

    fun dataBaseDelete(
        targetString: Column<String> = Shops.shopname, targetValueString: String = "", uuid: String = "",
        targetInt: Column<EntityID<Int>> = Shops.id, targetValueInt: Int = -1
    ): String {
        var returnMessage = "commands.deleteshop.ok"
        transaction(DbSettings.db) {
            SchemaUtils.create(Shops)
            if (targetValueString != ""){
                for(i in dataBaseInquire(targetValueString = targetValueString)){
                    val (type,data) = i.split(":", limit = 2)
                    if (type == "ownerUUID"){
                        if (data == uuid){
                            Shops.deleteWhere { targetString eq targetValueString and (owner eq data) }
                        } else {
                            returnMessage = "commands.shops.none"
                        }
                    }
                }
            } else {
                Shops.deleteWhere { targetInt eq targetValueInt }
            }
        }
        return returnMessage
    }

    private fun blockPosToString(blockPos: BlockPos): String {
        return "${blockPos.x},${blockPos.y},${blockPos.z}"
    }

    private fun stringToBlockPos(blockPosString: String): BlockPos {
        val coordinates = blockPosString.split(",").map { it.toInt() }
        return BlockPos(coordinates[0], coordinates[1], coordinates[2])
    }
    fun stringToJson(data: String): List<Items> {
        return Json.decodeFromString(data)
    }
    fun stringToItemStackArgument(itemStackString: String, registryAccess: CommandRegistryAccess): ItemStackArgument {
        val stringReader = StringReader(itemStackString)
        val itemStackArgument = ItemStackArgumentType(registryAccess)
        return itemStackArgument.parse(stringReader)
    }
}