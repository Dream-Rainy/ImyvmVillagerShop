package com.imyvm.villagerShop.apis

import com.imyvm.hoki.i18n.HokiLanguage
import com.imyvm.hoki.i18n.HokiTranslator
import net.minecraft.text.Text
import com.imyvm.villagerShop.VillagerShopMain
import com.imyvm.villagerShop.apis.ModConfig.Companion.LANGUAGE


object Translator : HokiTranslator() {
    private var languageInstance = createLanguage(LANGUAGE.value)

    init {
        LANGUAGE.changeEvents.register { option, _, _ ->
            languageInstance = createLanguage(option.value)
        }
    }

    fun tr(key: String?, vararg args: Any?): Text {
        return translate(languageInstance, key, *args)
    }

    private fun createLanguage(languageId: String): HokiLanguage {
        val path = HokiLanguage.getResourcePath(VillagerShopMain.MOD_ID, languageId)
        val inputStream = Translator::class.java.getResourceAsStream(path)
        return HokiLanguage.create(inputStream)
    }
}
