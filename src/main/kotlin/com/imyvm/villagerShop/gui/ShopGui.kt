package com.imyvm.villagerShop.gui

import com.imyvm.villagerShop.VillagerShopMain
import com.imyvm.villagerShop.apis.EconomyData
import com.imyvm.villagerShop.apis.ShopService.Companion.ShopType
import com.imyvm.villagerShop.apis.Translator.tr
import com.imyvm.villagerShop.shops.ShopEntity
import com.imyvm.villagerShop.shops.ShopEntity.Companion.shopDBService
import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.gui.MerchantGui
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.predicate.ComponentPredicate
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.village.TradeOffer
import net.minecraft.village.TradedItem
import java.time.LocalDate
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.random.Random

class ShopGui(private val playerEntity: ServerPlayerEntity, private val registries: RegistryWrapper.WrapperLookup) {
    private val gui = object : MerchantGui(playerEntity, false) {
        override fun onAnyClick(index: Int, type: ClickType?, action: SlotActionType?): Boolean {
            if (index == 0 || index == 1) {
                return false
            } else if (index == 2 &&
                this.merchantInventory.getStack(2).item == this.selectedTrade?.sellItem?.item &&
                action != SlotActionType.SWAP
            ) {
                if (shopEntity?.type == ShopType.SELL || shopEntity?.type == ShopType.REFRESHABLE_SELL) { // Sell
                    val imyvmCurry = this.merchantInventory.getStack(0)
                    val moneyShouldTake = imyvmCurry.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt()?.getDouble("price") ?: return false
                    val stock = imyvmCurry.get(DataComponentTypes.REPAIR_COST) ?: return false
                    val sellItem = this.merchantInventory.getStack(2)
                    if (stock == 0) {
                        return false
                        this.close()
                        player.sendMessage(tr("shop.buy.stock.lack"))
                    }
                    val economyData = EconomyData(player)
                    val playerBalance = economyData.getMoney()
                    var tradeNumber = if (action == SlotActionType.PICKUP && playerBalance >= moneyShouldTake) {
                        1
                    } else if (action == SlotActionType.QUICK_MOVE) {
                        if (playerBalance/moneyShouldTake >= 64) {
                            64
                        } else {
                            (playerBalance/moneyShouldTake).toInt()
                        }
                    } else {
                        return false
                    }
                    if (tradeNumber > stock) {
                        tradeNumber = stock
                    }
                    shopEntity?.items?.find { it.item.itemStack.isOf(sellItem.item) }?.let { itemEntry ->
                        if (shopEntity?.admin == 0) {
                            itemEntry.stock["default"] = itemEntry.stock.getOrPut("default") { stock } - tradeNumber
                        } else {
                            itemEntry.stock[player.uuid.toString()] = itemEntry.stock.getOrPut(player.uuid.toString()) {
                                stock
                            } - tradeNumber
                        }
                    }
                    economyData.addMoney((-moneyShouldTake * 100 * tradeNumber).toLong())
                    sellItem.count = tradeNumber * sellItem.count
                    player.inventory.offerOrDrop(sellItem)
                    player.sendMessage(tr("shop.buy.success", moneyShouldTake*tradeNumber, tradeNumber, sellItem.toHoverableText()))

                    imyvmCurry.set(DataComponentTypes.REPAIR_COST, stock-tradeNumber)
                    this.merchantInventory.setStack(0, imyvmCurry)
                } else { // Buy
                    val imyvmCurry = this.merchantInventory.getStack(2)
                    val moneyShouldGet = imyvmCurry.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt()?.getDouble("price") ?: return false
                    val stock = imyvmCurry.get(DataComponentTypes.REPAIR_COST) ?: return false
                    val buyItem = this.merchantInventory.getStack(0)
                    val economyData = EconomyData(player)
                    val playerBalance = economyData.getMoney()
                    var sellNumber = if (action == SlotActionType.PICKUP && playerBalance >= moneyShouldGet) {
                        1
                    } else if (action == SlotActionType.QUICK_MOVE) {
                        (player.inventory.count(buyItem.item)/buyItem.count).toInt()
                    } else {
                        return false
                    }
                    buyItem.count = sellNumber * buyItem.count
                    player.inventory.removeOne(buyItem)
                    shopEntity?.items?.find { it.item.itemStack.isOf(buyItem.item) }?.let { itemEntry ->
                        itemEntry.stock[player.uuid.toString()] = itemEntry.stock.getOrPut(player.uuid.toString()) {
                            stock
                        } - sellNumber
                    }
                    economyData.addMoney((moneyShouldGet * 100 * sellNumber).toLong())
                    player.sendMessage(tr("shop.purchase.success", moneyShouldGet*sellNumber))

                    imyvmCurry.set(DataComponentTypes.REPAIR_COST, stock-sellNumber)
                    this.merchantInventory.setStack(2, imyvmCurry)
                }

            }
            return super.onAnyClick(index, type, action)
        }

        override fun onTrade(offer: TradeOffer?): Boolean {
            return false
        }

        override fun onClose() {
            villagerEntity?.let { removeGui(it) }
            shopEntity?.update()
            super.onClose()
        }
    }
    private var villagerEntity: VillagerEntity? = null
    private var type = -1
    private var shopEntity: ShopEntity? = null
    var id = -1
    fun open(villager: VillagerEntity) {
        this.villagerEntity = villager
        villagerEntity!!.commandTags.forEach { value ->
            val idPattern: Pattern = Pattern.compile("id:[0-9]+")
            val typePattern: Pattern = Pattern.compile("type:[0-9]+")
            val idMatcher: Matcher = idPattern.matcher(value)
            if (idMatcher.find()) {
                this.id = idMatcher.group().split(":")[1].toInt()
            }
            val typeMatcher: Matcher = typePattern.matcher(value)
            if (typeMatcher.find()) {
                this.type = typeMatcher.group().split(":")[1].toInt()
            }
        }
        if (id == -1 || type == -1) {
            return
        }

        shopEntity = shopDBService.readById(id, this.registries)

        shopEntity?.items?.forEach { items ->
            // Get stock

            val stock = if (shopEntity?.admin == 0) {
                items.stock.getOrPut("default") { 0 }
            } else {
                items.stock.getOrPut(playerEntity.uuid.toString()) {
                    items.stock.getOrPut("default") { -1 }
                }.let { stockValue ->
                    if (stockValue == -1) Int.MAX_VALUE else stockValue
                }
            }

            // Set currency

            val itemStack = ItemStack(Items.BAMBOO)
            itemStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            val lore = LoreComponent(
                listOf(
                    tr("shop.curry.lore_1"),
                    tr("shop.curry.lore_2"),
                    tr("shop.curry.lore_3"),
                    tr("shop.curry.lore_4")
                )
            )
            val sellItemName = tr("shop.curry.displayname", items.price)
            itemStack.set(DataComponentTypes.CUSTOM_NAME, sellItemName)
            itemStack.set(DataComponentTypes.LORE, lore)
            itemStack.set(DataComponentTypes.REPAIR_COST, stock)
            val nbtTemp = NbtCompound()
            nbtTemp.putDouble("price", items.price)
            val localDate = LocalDate.now()
            nbtTemp.putString("securityCode",
                Random(localDate.year + localDate.dayOfYear + localDate.monthValue + items.count + stock + Random.nextInt()).nextInt().toString()
            )
            itemStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtTemp))
            val buyItem = TradedItem(
                Registries.ITEM.getEntry(Registries.ITEM.getKey(Items.BAMBOO).get()).get(),
                items.count, ComponentPredicate.of(itemStack.components), itemStack
            )

            val sellItem = items.item
            if (type == 0) {
                val tradeOffer =
                    if (stock >= items.count) {
                        TradeOffer(buyItem, sellItem.itemStack, stock, 0 ,0f)
                    } else {
                        TradeOffer(buyItem, sellItem.itemStack, 0, 0, 0f)
                    }
                gui.addTrade(tradeOffer)
            } else {
                gui.addTrade(TradeOffer(sellItem, buyItem.itemStack, stock, 0, 0f))
            }
        }

        gui.title = Text.of(shopEntity?.shopname)
        gui.open()

        addGui(villager)
    }

    private fun addGui(villager: VillagerEntity) {
        VillagerShopMain.guiSet.add(villager)
    }

    fun removeGui(villager: VillagerEntity) {
        VillagerShopMain.guiSet.remove(villager)
    }
}