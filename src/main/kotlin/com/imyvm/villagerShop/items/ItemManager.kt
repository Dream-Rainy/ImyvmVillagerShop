package com.imyvm.villagerShop.items

import com.imyvm.villagerShop.apis.Translator.tr
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.predicate.ComponentPredicate
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.village.TradedItem
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min
import kotlin.ranges.until

class ItemManager(
    var item: TradedItem,
    var count: Int,
    var price: Double,
    var stock: MutableMap<String, Int>,
    var registries: RegistryWrapper.WrapperLookup
) {
    constructor(
        item: ItemStackArgument,
        count: Int,
        price: Double,
        stock: MutableMap<String, Int> = mutableMapOf<String, Int>(),
        registries: RegistryWrapper.WrapperLookup
    ): this(
        TradedItem(
            Registries.ITEM.getEntry(Registries.ITEM.getKey(item.item).get()).get(),
            count,
            ComponentPredicate.of(item.createStack(count, false).components),
            item.createStack(count, false)
        ),
        count, price, stock,
        registries)

    @Serializable
    data class ItemData(
        val itemNbt: String,
        val count: Int,
        val price: Double,
        val stock: MutableMap<String, Int>
    )

    fun toJsonString(): String {
        val nbt = item.itemStack.encode(registries).asString()
        val itemData = ItemData(nbt, count, price, stock)
        return Json.encodeToString(itemData)
    }

    companion object {
        fun removeItemFromInventory(player: PlayerEntity, itemToRemove: ItemStack, quantity: Int) :Int {
            val inventory = player.inventory
            var count = quantity
            for (i in 0 until inventory.size()) {
                val currentItem = inventory.getStack(i)
                if (currentItem.components == itemToRemove.components) {
                    val itemsToRemoveFromSlot = min(count, currentItem.count)
                    currentItem.decrement(itemsToRemoveFromSlot)
                    if (itemsToRemoveFromSlot == count) {
                        player.sendMessage(tr("commands.stock.add.ok", count))
                        return 1
                    } else {
                        count -= itemsToRemoveFromSlot
                    }
                }
            }
            if ( count != 0 ) {
                itemToRemove.count = quantity - count
                inventory.offerOrDrop(itemToRemove)
                return -1
            }
            player.sendMessage(tr("commands.stock.add.ok", count))
            return 1
        }

        fun offerItemToPlayer(player: PlayerEntity, itemToGiveList: MutableList<ItemManager>) {
            val inventory = player.inventory
            for (item in itemToGiveList) {
                inventory.offerOrDrop(
                    ItemStack(item.item.item, item.stock["default"]!!)
                )
            }
        }

        fun storeItemList (itemList: MutableList<ItemManager>): String {
            return Json.encodeToString(itemList.map { it.toJsonString() })
        }

        fun restoreItemList(jsonString: String, registries: RegistryWrapper.WrapperLookup): MutableList<ItemManager> {
            val stringList: List<String> = Json.decodeFromString(jsonString)
            val itemDataList = stringList.map { jsonItem ->
                Json.decodeFromString<ItemData>(jsonItem)
            }

            val itemManagerList = mutableListOf<ItemManager>()

            itemDataList.map { itemData ->
                val nbt = StringNbtReader.parse(itemData.itemNbt)
                val itemStack = ItemStack.fromNbt(registries, nbt)

                itemStack.getOrNull()?.let {
                    itemManagerList.add(
                        ItemManager(
                            TradedItem(
                                Registries.ITEM.getEntry(Registries.ITEM.getKey(it.item).get()).get(),
                                itemData.count,
                                ComponentPredicate.of(it.components),
                                it
                            ),
                            itemData.count,
                            itemData.price,
                            itemData.stock,
                            registries
                        )
                    )
                }
            }

            return itemManagerList
        }
    }
}