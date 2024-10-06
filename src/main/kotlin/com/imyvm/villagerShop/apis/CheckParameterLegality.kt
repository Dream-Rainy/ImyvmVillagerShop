package com.imyvm.villagerShop.apis

import com.imyvm.villagerShop.items.ItemManager
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.predicate.ComponentPredicate
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.village.TradedItem
import kotlin.jvm.optionals.getOrNull

fun checkParameterLegality(args: String, registries: RegistryWrapper.WrapperLookup): Triple<Int, MutableList<ItemManager>, String> {
    val argList: List<String> = args.split(" ")
    val itemList = mutableListOf<ItemManager>()
    val itemR = Regex("^[minecraft]+:[a-z_]+")
    val countPriceStockR = Regex("^[0-9]+:+[0-9]+:+[0-9]+")
    var itemCount = 0
    var priceStockCount = 0
    var compare = -1
    var item = ""
    var errorMessage = ""
    for (i in argList) {
        if (itemR.matches(i)) {
            itemCount += 1
            item = i
        } else if (countPriceStockR.matches(i)) {
            priceStockCount += 1
            val (count, price, stock) = i.split(":")
                .let { list ->
                    Triple(
                        list[0].toInt(),
                        list[1].toDouble(),
                        list.getOrElse(2) { "-1" }.toInt()
                    )
                }
            val nbt = StringNbtReader.parse(item)
            val itemStack = ItemStack.fromNbt(registries, nbt).getOrNull() ?: ItemStack.EMPTY
            if (itemStack.isEmpty) {
                compare = 2
                errorMessage = item
                break
            }
            itemList.add(ItemManager(
                TradedItem(
                    Registries.ITEM.getEntry(Registries.ITEM.getKey(itemStack.item).get()).get(),
                    count,
                    ComponentPredicate.of(itemStack.components),
                    itemStack
                ),
                count,
                price,
                mutableMapOf<String, Int>(Pair("default", stock)),
                registries)
            )
        }
    }
    if (itemCount == 0) {
        compare = 0
    } else if (itemCount != priceStockCount) {
        compare = 1
    }
    return Triple(compare, itemList, errorMessage)
}