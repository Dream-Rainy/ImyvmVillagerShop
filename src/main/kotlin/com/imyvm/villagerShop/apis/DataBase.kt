package com.imyvm.villagerShop.apis

import com.imyvm.villagerShop.VillagerShopMain.Companion.LOGGER
import com.imyvm.villagerShop.VillagerShopMain.Companion.itemList
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_MAXPOOLSIZE
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_PASSWORD
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_TYPE
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_URL
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_USER
import com.imyvm.villagerShop.shops.spawnInvulnerableVillager
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists

enum class ItemOperation {
    ADD, DELETE, CHANGE
}
@Serializable
data class Items(
    var item: String,
    var count: Int,
    var price: Int,
    var stock: Int
)

data class ShopInfo(
    val id: EntityID<Int>,
    val shopname: String,
    val posX: Int,
    val posY: Int,
    val posZ: Int,
    val world: String,
    val admin: Int,
    val type: Int,
    val owner: String,
    val items: String,
    val income: Double
)

private val serializer = ListSerializer(Items.serializer())
object DbSettings {
    val db by lazy {
        var database: Database
        when (DATABASE_TYPE.value) {
            "POSTGRESQL" -> {
                database = configInitialization("org.postgresql.Driver")
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
            else -> {
                LOGGER.error("Unsupported database type: ${DATABASE_TYPE.value}. Defaulting to SQLite.")
                LOGGER.error("We do not recommend you to use SQLite, please choose another database")
                val path = Paths.get("./world")
                if (!path.exists()) {
                    Files.createDirectory(path)
                }
                database = Database.connect(
                    url = "jdbc:sqlite:./world/imyvm_villagershop.db",
                    driver = "org.sqlite.JDBC"
                )
            }
        }
        try {
            transaction(database) {
                SchemaUtils.create(Shops)
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to connect to specified database due to error: ${e.message}. Defaulting to SQLite.")
            val path = Paths.get("./world")
            if (!path.exists()) {
                Files.createDirectory(path)
            }
            database = Database.connect(
                url = "jdbc:sqlite:./world/imyvm_villagershop.db",
                driver = "org.sqlite.JDBC"
            )
            transaction(database) {
                SchemaUtils.create(Shops)
            }
        }
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
    val posX = integer("posX")
    val posY = integer("posY")
    val posZ = integer("posZ")
    val world = varchar("world", 100)
    val owner = varchar("owner", 40)
    val admin = integer("admin")
    val type = integer("type")
    val items = text("items")
    val income = double("income")
}

fun adminShopCreateSave(
    sellItemList: MutableList<Items>,
    shopname: String,
    pos: BlockPos,
    owner: ServerPlayerEntity,
    worldUUID: String,
    type: Int
): String {
    val id = dataBaseSave(shopname, pos, worldUUID, Json.encodeToString(serializer, sellItemList), owner.entityName,1, type)
    spawnInvulnerableVillager(pos, owner.world, shopname, type, id)
    return "commands.shop.create.success"
}

fun playerShopCreateSave(
    item: ItemStackArgument,
    count: Int,
    price: Int,
    stock: Int,
    shopname: String,
    pos: BlockPos,
    owner: ServerPlayerEntity,
    worldUUID: String
): String {
    val sellItemList = mutableListOf(Items(item.asString(), count, price, stock))
    val id = dataBaseSave(shopname, pos, worldUUID, Json.encodeToString(serializer,sellItemList), owner.entityName)
    spawnInvulnerableVillager(pos, owner.world, shopname, id = id)
    return "commands.shop.create.success"
}

fun modifyItems(
    shopname: String,
    item: ItemStackArgument,
    count: Int = 0,
    price: Int = 0,
    stock: Int = 0,
    playerName: String,
    operation: ItemOperation
): String {
    println(item.item)
    val shop = dataBaseInquireByShopname(shopname, playerName).firstOrNull() ?: return "commands.shops.none"
    val currentItem = strungToItemsMutableList(shop.items)
    when (operation) {
        ItemOperation.ADD -> {
            if (itemList.firstOrNull {
                    stringToItem(it.item) == item.item && it.price.toLong() <= price / count * 0.8
                } != null)
            {
                return "commands.shop.create.item.price.toolow"
            }
            if (currentItem.size >= 7) {
                return "commands.playershop.item.limit"
            } else if (currentItem.firstOrNull { stringToItem(it.item) == item.item } != null) {
                return "commands.playershop.add.repeat"
            } else {
                currentItem.add(Items(item.asString(), count, price, stock))
            }
            dataBaseChangeItemByShopname(shopname, currentItem, playerName)
            return "commands.shop.item.add.success"
        }
        ItemOperation.DELETE -> {
            val itemNeedToDelete = currentItem.firstOrNull {
                stringToItem(it.item) == item.item
            }
            if (itemNeedToDelete == null) {
                return "commands.shop.item.none"
            }
            currentItem.remove(itemNeedToDelete)
            dataBaseChangeItemByShopname(shopname, currentItem, playerName)
            return "commands.shop.item.delete.success, ${itemNeedToDelete.stock}"
        }
        ItemOperation.CHANGE -> {
            var itemToChange = currentItem.firstOrNull {
                stringToItem(it.item) == item.item
            }
            return if (itemToChange != null) {
                itemToChange = Items(item.asString(), count, price, itemToChange.stock)
                println(itemToChange)
                currentItem.removeIf {
                    stringToItem(it.item) == item.item
                }
                currentItem.add(itemToChange)
                dataBaseChangeItemByShopname(shopname, currentItem, playerName)
                "commands.shop.item.change.success"
            } else {
                "commands.shop.item.none"
            }
        }
    }
}

private fun dataBaseSave(shopname: String, pos: BlockPos, worldUUID:String, items: String, owner: String, admin: Int = 0, type: Int = 0): Int {
    return transaction(DbSettings.db) {
        Shops.insertAndGetId {
            it[Shops.shopname] = shopname
            it[posX] = pos.x
            it[posY] = pos.y
            it[posZ] = pos.z
            it[world] = worldUUID
            it[Shops.items] = items
            it[Shops.owner] = owner
            it[Shops.admin] = admin
            it[Shops.type] = type
            it[income] = 0.0
        }
    }.value
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
        type = row[Shops.type],
        owner = row[Shops.owner],
        items = row[Shops.items],
        income = row[Shops.income]
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

fun dataBaseInquireByChunk(
    chunkPos: ChunkPos, world: String
): List<ShopInfo> {
    return executeQuery {
        Shops.select {
            (Shops.posX.between(chunkPos.startX, chunkPos.endX)) and (Shops.posZ.between(chunkPos.startZ, chunkPos.endZ)) and
                    (Shops.world eq world)
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

fun dataBaseChangeItemById(
    id: Int,
    sellItemListNew: MutableList<Items>,
): Int {
    return updateDatabase({ Shops.id eq id }) {
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

fun dataBaseChangeIncomeById(
    id: Int,
    income: Double
) {
    updateDatabase({ Shops.id eq id }) {
        it[this.income] = income
    }
}

fun strungToItemsMutableList(data: String): MutableList<Items> {
    return Json.decodeFromString(data)
}
fun stringToItem(itemString: String): Item {
    return Registries.ITEM[Identifier(itemString)]
}