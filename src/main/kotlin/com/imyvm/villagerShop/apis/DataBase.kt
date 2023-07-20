package com.imyvm.villagerShop.apis

import com.imyvm.villagerShop.VillagerShopMain.Companion.LOGGER
import com.imyvm.villagerShop.VillagerShopMain.Companion.itemList
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_MAXPOOLSIZE
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_PASSWORD
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_TYPE
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_URL
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_USER
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction

enum class ItemOperation {
    ADD, DELETE, CHANGE
}
@Serializable
data class Items(
    val item: String,
    val count: Int,
    val price: Int,
    val stock: Int
)

data class ShopInfo(
    val id: EntityID<Int>,
    val shopname: String,
    val posX: Int,
    val posY: Int,
    val posZ: Int,
    val world: String,
    val admin: Int,
    val owner: String,
    val items: String
)

class DataBase {
    private val serializer = ListSerializer(Items.serializer())
    object DbSettings {
        val db by lazy {
            var database: Database
            when (DATABASE_TYPE.value) {
                "POSTGRESQL" -> {
                    database = configInitialization("com.impossibl.postgres.jdbc.PGDriver")
                }
                "ORACLE" -> {
                    database = configInitialization("oracle.jdbc.OracleDriver")
                }
                "SQLSERVER" -> {
                    database = configInitialization("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                }
                "MYSQL" -> {
                    val config = HikariConfig().apply {
                        jdbcUrl         = DATABASE_URL.value
                        driverClassName = "com.mysql.cj.jdbc.Driver"
                        username        = DATABASE_USER.value
                        password        = DATABASE_PASSWORD.value
                        maximumPoolSize = DATABASE_MAXPOOLSIZE.value
                    }
                    val dataSource = HikariDataSource(config)
                    database = Database.connect(dataSource)
                }
                else -> throw IllegalArgumentException("Unsupported database type: ${DATABASE_TYPE.value}")
            }
            try {
                transaction(database) {
                    SchemaUtils.create(Shops)
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to connect to specified database due to error: ${e.message}. Defaulting to SQLite.")
                database = Database.connect(
                    url = "jdbc:sqlite:./world/imyvm_villagershop.db",
                    driver = "org.sqlite.JDBC"
                )
            }
            SchemaUtils.create(Shops)
            database
        }

        private fun configInitialization(driver: String) : Database {
            return Database.connect(
                DATABASE_URL.value,
                driver = driver,
                user = DATABASE_USER.value,
                password = DATABASE_PASSWORD.value
            )
        }
    }

    object Shops : IntIdTable() {
        val shopname = varchar("shopname", 20)
        val owner = varchar("owner", 40)
        val admin = integer("admin")
        val type = integer("type")
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
        worldUUID: String,
        type: Int
    ): String {
        dataBaseSave(shopname, pos, worldUUID, Json.encodeToString(serializer,sellItemList), owner,1, type)
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
        val sellItemList = dataBaseInquireByShopname(shopname,playerName)
        val sellItemListNew = mutableListOf<Items>()
        var itemCount = 0

        for (i in sellItemList) {
            val currentItem = stringToJson(i.items)
            when (operation) {
                ItemOperation.ADD -> {
                    itemCount = currentItem.size
                    sellItemListNew.addAll(currentItem)
                }
                ItemOperation.DELETE -> {
                    for (j in currentItem) {
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

        if (operation == ItemOperation.ADD) {
            for (i in itemList) {
                if ((DataBase().stringToItem(i.item) == item?.item) && i.price.toLong() <= price/count*0.8) {
                    return "commands.shop.create.item.price.toolow"
                }
            }
            if (itemCount >= 7) {
                return "commands.playershop.create.limit"
            } else if (sellItemListNew.firstOrNull { it.item == item?.asString() } != null) {
                return "commands.playershop.add.repeat"
            } else {
                sellItemListNew.add(Items(item!!.asString(), count, price, 0))
            }
        }
        if (operation == ItemOperation.DELETE && itemCount == 0) {
            return "commands.shop.item.none"
        }

        if (dataBaseChangeItemByShopname(shopname, sellItemListNew, playerName) == -1) {
            return "commands.shops.none"
        }

        return when (operation) {
            ItemOperation.ADD -> "commands.shop.item.add.success"
            ItemOperation.DELETE -> "commands.shop.item.delete.success,${itemCount}"
            ItemOperation.CHANGE -> "commands.shop.item.change.success"
        }
    }

    private fun dataBaseSave(shopname: String, pos: BlockPos, worldUUID:String, items: String, owner: String, admin: Int = 0, type: Int = 0) {
        transaction(DbSettings.db) {
            Shops.insert {
                it[Shops.shopname] = shopname
                it[posX] = pos.x
                it[posY] = pos.y
                it[posZ] = pos.z
                it[world] = worldUUID
                it[Shops.items] = items
                it[Shops.owner] = owner
                it[Shops.admin] = admin
                it[Shops.type] = type
            } get Shops.id
        }
    }

    private fun executeQuery(queryBuilder: () -> Query): List<ShopInfo> {
        return transaction(DbSettings.db) {
            val query = queryBuilder()
            query.map(::processRow)
        }
    }
    private fun processRow(row: ResultRow): ShopInfo {
        return ShopInfo(
            id = row[Shops.id],
            shopname = row[Shops.shopname],
            posX = row[Shops.posX],
            posY = row[Shops.posY],
            posZ = row[Shops.posZ],
            world = row[Shops.world],
            admin = row[Shops.admin],
            owner = row[Shops.owner],
            items = row[Shops.items]
        )
    }

    fun dataBaseInquireById(
        id: Int
    ): List<ShopInfo> {
        return executeQuery {
            Shops.select { Shops.id eq id }
        }
    }

    fun dataBaseInquireByShopname(
        shopName: String,
        owner: String = ""
    ): List<ShopInfo> {
        return executeQuery {
            if (owner != "") {
                Shops.select { (Shops.shopname eq shopName) and (Shops.owner eq owner) }
            } else {
                Shops.select { Shops.shopname eq shopName }
            }
        }
    }

    fun dataBaseInquireByLocation(
        pos: String,
        rangeX: Int, rangeY: Int, rangeZ: Int, world: String
    ): List<ShopInfo> {
        return executeQuery {
            val (x, y, z) = pos.split(",").map { it.toInt() }
            Shops.select {
                (Shops.posX.between(x - rangeX , x + rangeX)) and (Shops.posY.between(y - rangeY, y + rangeY)) and
                        (Shops.posZ.between(z - rangeZ, z + rangeZ)) and
                        (Shops.world.eq(world))
            }
        }
    }

    fun dataBaseInquireByOwner(
        owner: String,
    ): List<ShopInfo> {
        return executeQuery {
            Shops.select { Shops.owner eq owner }
        }
    }

    fun dataBaseInquireByType(): List<ShopInfo> {
        return executeQuery {
            Shops.select { Shops.type eq 1 }
        }
    }

    private fun updateDatabase(condition: SqlExpressionBuilder.() -> Op<Boolean>, update: Shops.(UpdateStatement) -> Unit): Int {
        val numberOfRowsUpdated = transaction(DbSettings.db) {
            Shops.update (condition) {
                update(it)
            }
        }
        return when (numberOfRowsUpdated) {
            0 -> -1
            else -> 1
        }
    }

    fun dataBaseChangeShopnameByShopname(
        oldShopName: String,
        newShopName: String,
        owner: String = ""
    ): Int {
        return if (owner != "") {
            updateDatabase({ (Shops.shopname eq oldShopName) and (Shops.owner eq owner) }) {
                it[shopname] = newShopName
            }
        } else {
            updateDatabase({ (Shops.shopname eq oldShopName) }) {
                it[shopname] = newShopName
            }
        }
    }

    fun dataBaseChangeShopnameById(
        id: Int,
        newShopName: String,
    ): Int {
        return updateDatabase({ Shops.id eq id }) {
            it[shopname] = newShopName
        }
    }

    private fun dataBaseChangeItemByShopname(
        shopName: String,
        sellItemListNew: MutableList<Items>,
        owner: String
    ): Int {
        return updateDatabase({ Shops.shopname eq shopName and (Shops.owner eq owner) }) {
            it[items] = Json.encodeToString(serializer, sellItemListNew)
        }
    }

    fun dataBaseChangeAdminById(
        id: Int
    ): Int {
        return updateDatabase({ Shops.id eq id }) {
            it[admin] = 1
        }
    }

    fun dataBaseChangePosByShopname(
        shopName: String,
        blockPos: BlockPos,
        owner: String
    ): Int {
        return updateDatabase({ Shops.shopname eq shopName and (Shops.owner eq owner)}) {
            it[posX] = blockPos.x
            it[posY] = blockPos.y
            it[posZ] = blockPos.z
        }
    }

    fun dataBaseChangePosById(
        id: Int,
        blockPos: BlockPos
    ): Int {
        return updateDatabase({ Shops.id eq id }) {
            it[posX] = blockPos.x
            it[posY] = blockPos.y
            it[posZ] = blockPos.z
        }
    }

    fun dataBaseDeleteByShopname(
        shopName: String,
        playerName: String
    ): String {
        var returnMessage = "commands.shops.none"
        transaction(DbSettings.db) {
            for (i in dataBaseInquireByShopname(shopName,playerName)) {
                Shops.deleteWhere { shopname eq shopName and (owner eq i.owner) }
                returnMessage = "commands.deleteshop.ok"
            }
        }
        return returnMessage
    }
    fun dataBaseDeleteById(
        id: Int,
    ): String {
        return if (Shops.deleteWhere { Shops.id eq id } == 0) {
            "commands.shops.none"
        }else {
            "commands.deleteshop.ok"
        }
    }

    fun stringToJson(data: String): List<Items> {
        return Json.decodeFromString(data)
    }
    fun stringToItem(itemString: String): Item {
        return Registries.ITEM[Identifier(itemString)]
    }
}