package com.imyvm.villagerShop

import com.imyvm.economy.api.TradeTypeEnum.TradeTypeExtension

enum class TradeType : TradeTypeExtension{
    STOCK;

    private var tax: Double = 0.0

    override fun getTax(): Double {
        return this.tax
    }

    override fun setTax(tax: Double) {
        this.tax = tax
    }
}