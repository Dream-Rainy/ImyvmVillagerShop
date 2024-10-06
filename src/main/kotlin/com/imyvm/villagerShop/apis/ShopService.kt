package com.imyvm.villagerShop.apis

import com.imyvm.villagerShop.apis.Translator.tr
import com.imyvm.villagerShop.items.ItemManager.Companion.restoreItemList
import com.imyvm.villagerShop.items.ItemManager.Companion.storeItemList
import com.imyvm.villagerShop.shops.ShopEntity
import com.imyvm.villagerShop.shops.ShopEntity.Companion.sendMessageByType
import com.imyvm.villagerShop.shops.ShopEntity.Companion.shopDBService
import com.mojang.brigadier.context.CommandContext
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.command.ServerCommandSource
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ShopService(private val database: Database) {
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

    init {
        transaction(database) {
            SchemaUtils.create(Shops)
        }
    }

    private fun createShopEntity(resultRow: ResultRow, registries: RegistryWrapper.WrapperLookup): ShopEntity {
        return ShopEntity(
            resultRow[Shops.id].value,
            resultRow[Shops.shopname],
            resultRow[Shops.posX],
            resultRow[Shops.posY],
            resultRow[Shops.posZ],
            resultRow[Shops.world],
            resultRow[Shops.admin],
            ShopType.entries[resultRow[Shops.type]],
            resultRow[Shops.owner],
            restoreItemList(resultRow[Shops.items], registries),
            resultRow[Shops.income]
        )
    }

    fun <T> dbQuery(block: () -> T): T =
        transaction(database) { block() }

    @Suppress("DuplicatedCode")
    fun create(shop: ShopEntity): Int = dbQuery {
        Shops.insert {
            it[shopname] = shop.shopname
            it[posX] = shop.posX
            it[posY] = shop.posY
            it[posZ] = shop.posZ
            it[world] = shop.world
            it[owner] = shop.owner
            it[admin] = shop.admin
            it[type] = shop.type.ordinal
            it[items] = storeItemList(shop.items)
            it[income] = shop.income
        }[Shops.id].value
    }

    fun readById(id: Int, registries: RegistryWrapper.WrapperLookup): ShopEntity? = dbQuery {
        Shops.selectAll()
            .where { Shops.id eq id }
            .map {
                createShopEntity(it, registries)
        }.singleOrNull()
    }

    fun readByShopName(shopName: String,
                       playerName: String = "",
                       registries: RegistryWrapper.WrapperLookup
    ): List<ShopEntity?> = dbQuery {
        val condition = if (playerName != "") {
            Shops.selectAll()
                .where { (Shops.shopname eq shopName) and (Shops.owner eq playerName) }
        } else {
            Shops.selectAll()
                .where { Shops.shopname eq shopName }
        }
        condition.map {
            createShopEntity(it, registries)
        }
    }

    fun readByOwner(playerName: String, registries: RegistryWrapper.WrapperLookup): List<ShopEntity> = dbQuery {
        Shops.selectAll()
            .where { Shops.owner eq playerName }
            .map {
                createShopEntity(it, registries)
            }
    }

    fun readByLocation(
        pos: String,
        rangeX: Int, rangeY: Int, rangeZ: Int, world: String,
        registries: RegistryWrapper.WrapperLookup
    ): List<ShopEntity> = dbQuery {
        val (x, y, z) = pos.split(",").map { it.toInt() }
        Shops.selectAll()
            .where { (Shops.posX.between(x - rangeX, x + rangeX)) and
                    (Shops.posY.between(y - rangeY, y + rangeY)) and
                    (Shops.posZ.between(z - rangeZ, z + rangeZ)) and
                    (Shops.world eq world) }
            .map {
                createShopEntity(it, registries)
            }
    }

    fun readByType(registries: RegistryWrapper.WrapperLookup, shopTypes: List<ShopType>): List<ShopEntity> = dbQuery {
        Shops.selectAll()
            .where { Shops.type inList shopTypes.map { it.ordinal } }
            .map {
                createShopEntity(it, registries)
            }
    }

    @Suppress("DuplicatedCode")
    fun update(shop: ShopEntity) = dbQuery {
        Shops.update({ Shops.id eq shop.id }) {
            it[shopname] = shop.shopname
            it[posX] = shop.posX
            it[posY] = shop.posY
            it[posZ] = shop.posZ
            it[world] = shop.world
            it[owner] = shop.owner
            it[admin] = shop.admin
            it[type] = shop.type.ordinal
            it[items] = storeItemList(shop.items)
            it[income] = shop.income
        }
    }

    fun delete(id: Int) = dbQuery {
        Shops.deleteWhere { Shops.id.eq(id) }
    }

    companion object {

        enum class ShopType {
            SELL, UNLIMITED_BUY, REFRESHABLE_SELL, REFRESHABLE_BUY
        }

        fun rangeSearch(
            context: CommandContext<ServerCommandSource>,
            searchCondition: String,
        ): Int {
            val player = context.source.player!!
            val results = mutableListOf<ShopEntity?>()
            val registries = context.source.registryManager
            for (i in searchCondition.split(" ")) {
                if (i.contains(":")) {
                    val (condition, parameter) = i.split(":", limit = 2)
                    val temp = when (condition) {
                        "id" -> mutableListOf<ShopEntity?>(shopDBService.readById(parameter.toInt(), registries))
                        "shopname" -> shopDBService.readByShopName(parameter, registries = registries)
                        "owner" -> shopDBService.readByOwner(parameter, registries)
                        "location" -> shopDBService.readByLocation(parameter, 0, 0, 0, player.world.asString(), registries)
                        "range" -> {
                            val (rangeX,rangeY,rangeZ) = parameter.split(",").map {it.toInt()}
                            shopDBService.readByLocation(
                                "${player.pos.x},${player.pos.y},${player.pos.z}",
                                rangeX, rangeY, rangeZ ,player.world.asString(), registries)
                        }
                        else -> mutableListOf<ShopEntity?>()
                    }
                    if (!results.containsAll(temp)) results.addAll(temp)
                } else {
                    player.sendMessage(tr("commands.range.search.failed", i))
                }
            }
            if (results.isEmpty()) {
                player.sendMessage(tr("commands.search.none"))
                return -1
            }
            for (shop in results) {
                shop?.let { sendMessageByType(it, player) }
            }
            return 1
        }

        fun resetRefreshableSellAndBuy(registries: RegistryWrapper.WrapperLookup) {
            shopDBService.readByType(registries, listOf(ShopType.UNLIMITED_BUY, ShopType.REFRESHABLE_BUY,
                ShopType.REFRESHABLE_SELL)).forEach { shop ->
                shop.items.forEach { item ->
                    item.stock.keys.drop(1).forEach { item.stock.remove(it) }
                }
                shopDBService.update(shop)
            }
        }
    }
}