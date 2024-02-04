package com.imyvm.villagerShop.gui

import com.imyvm.economy.EconomyMod
import com.imyvm.villagerShop.VillagerShopMain
import com.imyvm.villagerShop.apis.*
import com.imyvm.villagerShop.apis.Translator.tr
import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.gui.MerchantGui
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.village.MerchantInventory
import net.minecraft.village.TradeOffer
import java.time.LocalDate
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.random.Random

class ShopGui(playerEntity: ServerPlayerEntity) {
    private val gui = object : MerchantGui(playerEntity, false) {
        override fun onAnyClick(index: Int, type: ClickType?, action: SlotActionType?): Boolean {
            if (this.merchantInventory.getStack(0).item == Items.PAPER) {
                this.merchantInventory.removeStack(0)
                if (this.merchantInventory.getStack(1).item == Items.BARRIER) {
                    this.merchantInventory.removeStack(1)
                }
            } else if (this.merchantInventory.getStack(2).item == Items.PAPER && index == 2) {
                val moneyShouldGetString = this.merchantInventory.getStack(2).name.string
                val pattern = Pattern.compile("[0-9]+\\.?[0-9]*")
                val matcher = pattern.matcher(moneyShouldGetString)
                if (matcher.find()) {
                    val moneyShouldGet = matcher.group().toDoubleOrNull() ?: return false
                    val playerBalance = EconomyMod.data.getOrCreate(this.player)
                    playerBalance.addMoney((moneyShouldGet * 100).toLong())
                    player.sendMessage(tr("shop.purchase.success", moneyShouldGet.toLong()))
                    val itemStack1 = merchantInventory.getStack(0)
                    val offer = merchant.offers.getValidOffer(itemStack1, Items.AIR.defaultStack, 0) ?: return false
                    println(itemStack1.count)
                    println(offer.adjustedFirstBuyItem.count)
                    itemStack1.count -= offer.adjustedFirstBuyItem.count
                    merchantInventory.setStack(0, itemStack1)
                }
                this.merchantInventory.removeStack(2)
            }
            return super.onAnyClick(index, type, action)
        }

        override fun onTrade(offer: TradeOffer?): Boolean {
            if (tradeCount == 0) {
                val imyvmCurry = offer?.adjustedFirstBuyItem
                val moneyShouldTakeString = imyvmCurry!!.name.string
                val pattern = Pattern.compile("[0-9]+\\.?[0-9]*")
                val matcher = pattern.matcher(moneyShouldTakeString)
                var moneyShouldTake: Double? = null
                while (matcher.find()) {
                    moneyShouldTake = matcher.group().toDoubleOrNull() ?: return false
                }
                if (moneyShouldTake != null) {
                    val playerBalance = EconomyMod.data.getOrCreate(this.player)
                    if (playerBalance.money < (moneyShouldTake * 100).toLong()) {
                        player.sendMessage(tr("shop.buy.failed.lack"))
                        clearMerchantInventory(this.merchantInventory)
                        return false
                    }
                    playerBalance.addMoney((-moneyShouldTake * 100).toLong())
                    player.sendMessage(tr("shop.buy.success", moneyShouldTake.toLong(), offer.sellItem.count, offer.sellItem.toHoverableText()))
                    val shop = dataBaseInquireById(id).firstOrNull()
                    val itemList = shop?.items?.let { strungToItemsMutableList(it) }
                    itemList?.forEach { items ->
                        if (stringToItem(items.item) == offer.sellItem.item && items.stock != 0) {
                            items.stock -= items.count
                            val income = shop.income + items.price - items.price * ModConfig.TAX_RESTOCK.value
                            dataBaseChangeIncomeById(id, income)
                            if (items.stock < items.count) {
                                offer.disable()
                            }
                        }
                    }
                    itemList?.let { dataBaseChangeItemById(id, it) }
                } else {
                    this.merchantInventory.removeStack(0)
                    this.merchantInventory.removeStack(1)
                    this.merchantInventory.removeStack(2)
                }
            }
            tradeCount += 1
            if (tradeCount == 3) {
                tradeCount = 0
            }
            return super.onTrade(offer)
        }

        override fun onClose() {
            villagerEntity?.let { removeGui(it) }
            super.onClose()
        }
    }
    private var villagerEntity: VillagerEntity? = null
    var id = -1
    private var type = -1
    var tradeCount = 0
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

        val shop = dataBaseInquireById(id).firstOrNull()
        val itemList = shop?.items?.let { strungToItemsMutableList(it) }

        itemList?.forEach { items ->
            // Set currency
            val buyItem = Items.PAPER.defaultStack
            buyItem.addEnchantment(Enchantments.MENDING, 1)
            buyItem.addHideFlag(ItemStack.TooltipSection.ENCHANTMENTS)
            val lore = NbtList()
            lore.add(NbtString.of(Text.Serializer.toJson(tr("shop.curry.lore_1"))))
            lore.add(NbtString.of(Text.Serializer.toJson(tr("shop.curry.lore_2"))))
            lore.add(NbtString.of(Text.Serializer.toJson(tr("shop.curry.lore_3"))))
            lore.add(NbtString.of(Text.Serializer.toJson(tr("shop.curry.lore_4"))))
            val sellItemName = tr("shop.curry.displayname", items.price)
            buyItem.setCustomName(sellItemName)
            buyItem.getOrCreateSubNbt(ItemStack.DISPLAY_KEY).put(ItemStack.LORE_KEY, lore)
            buyItem.getOrCreateSubNbt("imyvmCurry").putInt("price", items.price)
            val localDate = LocalDate.now()
            buyItem.getOrCreateSubNbt("imyvmCurry").putString("securityCode", Random(localDate.year + localDate.dayOfYear + localDate.monthValue + items.count + items.stock + Random.nextInt()).nextInt().toString())

            val sellItem = ItemStack(stringToItem(items.item), items.count)
            if (type == 0) {
                val tradeOffer = if (items.stock >= items.count) {
                    TradeOffer(buyItem, sellItem, items.stock, 0 ,0f)
                } else {
                    TradeOffer(buyItem, sellItem, 0, 0, 0f)
                }
                gui.addTrade(tradeOffer)
            } else {
                gui.addTrade(TradeOffer(sellItem, buyItem, Int.MAX_VALUE, 0, 0f))
            }
        }

        gui.title = Text.of(shop?.shopname)
        gui.open()

        addGui(villager)
    }

    private fun addGui(villager: VillagerEntity) {
        VillagerShopMain.guiSet.add(villager)
    }

    fun removeGui(villager: VillagerEntity) {
        VillagerShopMain.guiSet.remove(villager)
    }

    fun clearMerchantInventory(merchantInventory: MerchantInventory) {
        merchantInventory.removeStack(0)
        merchantInventory.removeStack(1)
        merchantInventory.removeStack(2)
    }
}