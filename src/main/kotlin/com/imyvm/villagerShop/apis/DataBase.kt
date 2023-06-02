package com.imyvm.villagerShop.apis

import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_PASSWORD
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_URL
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_USER
import com.imyvm.villagerShop.commands.itemList
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
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
enum class SearchOperation {
    ID,SHOPNAME,LOCATION,OWNER,ADMIN
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
        val posX = integer("posX")
        val posY = integer("posY")
        val posZ = integer("posZ")
        val world = varchar("world", 100)
        val items = text("items")
    }

    fun adminShopCreateSave(
        sellItemList: MutableList<Items>,
        shopname: String,
        pos: BlockPos,
        owner: String,
        worldUUID: String
    ): String {
        dataBaseSave(shopname, pos, worldUUID, Json.encodeToString(serializer,sellItemList), owner,1)
        return "commands.shop.create.success"
    }

    fun playerShopCreateSave(
        item: ItemStackArgument,
        count: Int,
        price: Int,
        stock: Int,
        shopname: String,
        pos: BlockPos,
        owner: String,
        worldUUID: String
    ): String {
        val sellItemList = mutableListOf(Items(item.asString(),count,price,stock))
        dataBaseSave(shopname, pos, worldUUID, Json.encodeToString(serializer,sellItemList), owner)
        return "commands.shop.create.success"
    }

    fun modifyItems(
        shopname: String,
        item: ItemStackArgument? = null,
        count: Int = 0,
        price: Int = 0,
        stock: Int = 0,
        playerName: String,
        operation: ItemOperation,
    ): String {
        val sellItemList = dataBaseInquire(targetValueString = shopname, operation = SearchOperation.SHOPNAME, playerName = playerName)
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
                            } else {
                                itemCount = j.stock
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
            for (i in itemList){
                val (itemString,serverPrice) = i.split(",")
                if ((DataBase().stringToItem(itemString) == item?.item) && serverPrice.toLong() <= price/count*0.8){
                    return "commands.shop.create.item.price.toolow"
                }
            }
            if (itemCount >= 7) {
                return "commands.playershop.create.limit"
            } else if (sellItemListNew.firstOrNull { it.item == item?.asString() } != null) {
                return "commands.playershop.add.repeat"
            } else {
                sellItemListNew.add(Items(item!!.asString(), count, price))
            }
        }
        if (operation == ItemOperation.DELETE && itemCount == 0) {
            return "commands.shop.item.none"
        }

        if (dataBaseChange(Shops.shopname, shopname, sellItemListNew, playerName = playerName, operation = DataSaveOperation.ITEM)==-1) {
            return "commands.shops.none"
        }

        return when (operation) {
            ItemOperation.ADD -> "commands.shop.item.add.success"
            ItemOperation.DELETE -> "commands.shop.item.delete.success,${itemCount}"
            ItemOperation.CHANGE -> "commands.shop.item.change.success"
        }
    }

    private fun dataBaseSave(shopname: String, pos: BlockPos, worldUUID:String, items: String, owner: String, admin: Int = 0) {
        transaction(DbSettings.db) {
            SchemaUtils.create(Shops)
            Shops.insert {
                it[Shops.shopname] = shopname
                it[posX] = pos.x
                it[posY] = pos.y
                it[posZ] = pos.z
                it[world] = worldUUID
                it[Shops.items] = items
                it[Shops.owner] = owner
                it[Shops.admin] = admin
            }get Shops.id
        }
    }

    fun dataBaseInquire(
        targetString: Column<String> = Shops.shopname, targetValueString: String = "",
        targetInt: Column<EntityID<Int>> = Shops.id, targetValueInt: Int = -1,
        rangeX: Int = 0,rangeY: Int = 0,rangeZ: Int = 0,
        operation: SearchOperation,
        playerName: String = "", world: String = ""
    ): MutableList<String> {
        val shopInfo = mutableListOf<String>()
        fun processRow(row: ResultRow) {
            shopInfo.apply {
                add("id:${row[Shops.id]}")
                add("shopname:${row[Shops.shopname]}")
                add("pos:${row[Shops.posX]},${row[Shops.posY]},${row[Shops.posZ]}")
                add("world:${row[Shops.world]}")
                add("admin:${row[Shops.admin]}")
                add("ownerName:${row[Shops.owner]}")
                add("items:${row[Shops.items]}")
            }
        }

        transaction(DbSettings.db) {
            SchemaUtils.create(Shops)
            val query = when (operation) {
                SearchOperation.ID -> Shops.select { targetInt eq targetValueInt }
                SearchOperation.SHOPNAME -> {
                    if (playerName ==""){
                        Shops.select { targetString eq targetValueString }
                    } else {
                        Shops.select { (targetString eq targetValueString) and (Shops.owner eq playerName) }
                    }
                }
                SearchOperation.LOCATION -> {
                    val (x, y, z) = targetValueString.split(",").map { it.toInt() }
                    Shops.select {
                        (Shops.posX.between(x - rangeX , x + rangeX)) and (Shops.posY.between(y - rangeY, y + rangeY)) and
                                (Shops.posZ.between(z - rangeZ, z + rangeZ)) and
                                (Shops.world.eq(world))
                    }
                }
                SearchOperation.OWNER -> Shops.select { targetString eq targetValueString }
                SearchOperation.ADMIN -> Shops.select { Shops.admin eq 1}
            }
            query.forEach(::processRow)
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
        playerName: String = "",
        blockPos: BlockPos = BlockPos(0, 0, 0),
        operation: DataSaveOperation
    ): Int {
        val condition: Op<Boolean> = when {
            targetInt == Shops.id -> { targetInt eq targetValueInt }
            blockPos != BlockPos(0, 0, 0) -> { Shops.posX eq blockPos.x and (Shops.posY eq blockPos.y) and (Shops.posZ eq blockPos.z) and (Shops.owner eq playerName) }
            else -> { target eq targetValue and (Shops.owner eq playerName) }
        }

        val numberOfRowsUpdated = transaction(DbSettings.db) {
            SchemaUtils.create(Shops)
            Shops.update({condition}) {
                when (operation) {
                    DataSaveOperation.SHOPNAME -> it[shopname] = shopNameNew
                    DataSaveOperation.ITEM -> it[items] = Json.encodeToString(serializer, sellItemListNew)
                    DataSaveOperation.ADMIN -> it[admin] = 1
                    DataSaveOperation.POS -> {
                        it[posX] = blockPos.x
                        it[posY] = blockPos.y
                        it[posZ] = blockPos.z
                    }
                }
            }
        }
        return when (numberOfRowsUpdated) {
            0 -> -1
            else -> 1
        }
    }

    fun dataBaseDelete(
        targetString: Column<String> = Shops.shopname, targetValueString: String = "", name: String = "",
        targetInt: Column<EntityID<Int>> = Shops.id, targetValueInt: Int = -1
    ): String {
        var returnMessage = "commands.deleteshop.ok"
        transaction(DbSettings.db) {
            SchemaUtils.create(Shops)
            if (targetValueString != ""){
                for(i in dataBaseInquire(targetValueString = targetValueString, operation = SearchOperation.SHOPNAME)){
                    val (type,data) = i.split(":", limit = 2)
                    if (type == "ownerName"){
                        if (data == name){
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

    fun stringToBlockPos(blockPosString: String): BlockPos {
        val coordinates = blockPosString.split(",").map { it.toInt() }
        return BlockPos(coordinates[0], coordinates[1], coordinates[2])
    }
    fun stringToJson(data: String): List<Items> {
        return Json.decodeFromString(data)
    }
    fun stringToItem(itemString: String): Item {
        return Registry.ITEM[Identifier(itemString)]
    }
}