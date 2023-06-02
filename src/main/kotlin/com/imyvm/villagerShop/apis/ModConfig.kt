package com.imyvm.villagerShop.apis

import com.imyvm.hoki.config.ConfigOption
import com.imyvm.hoki.config.HokiConfig
import com.imyvm.hoki.config.Option
import com.imyvm.villagerShop.VillagerShopMain
import com.typesafe.config.Config


class ModConfig : HokiConfig("imyvm_villagershop.conf") {
    companion object{
        @JvmField
        @ConfigOption
        val LANGUAGE = Option(
            "core.language",
            "en_us",
            "the display language of Imyvm villager shop mod"
        ) { obj: Config, path: String? ->
            obj.getString(
                path
            )
        }

        @JvmField
        @ConfigOption
        val TAX_RATE = Option(
            "core.tax_rate",
            0.01,
            "The tax rate players should pay when restocking."
        ) { obj: Config, path: String? ->
            obj.getDouble(
                path
            )
        }

        @JvmField
        @ConfigOption
        val ADMIN_NAME = Option(
            "core.admin_name",
            "Dream__Rain",
            "Admin name."
        ) { obj: Config, path: String? ->
            obj.getString(
                path
            )
        }

        @JvmField
        @ConfigOption
        val DATABASE_URL = Option(
            "core.database_url",
            "jdbc:postgresql://localhost:12346/imyvmvillagershop",
            "Database connection URL."
        ) { obj: Config, path: String? ->
            obj.getString(
                path
            )
        }

        @JvmField
        @ConfigOption
        val DATABASE_USER = Option(
            "core.database_username",
            "root",
            "Database username."
        ) { obj: Config, path: String? ->
            obj.getString(
                path
            )
        }

        @JvmField
        @ConfigOption
        val DATABASE_PASSWORD = Option(
            "core.database_password",
            "1145141919810",
            "Database password."
        ) { obj: Config, path: String? ->
            obj.getString(
                path
            )
        }
    }
}
