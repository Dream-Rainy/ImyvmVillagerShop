package com.imyvm.villagerShop.apis

import com.imyvm.hoki.config.ConfigOption
import com.imyvm.hoki.config.HokiConfig
import com.imyvm.hoki.config.Option
import com.typesafe.config.Config


class ModConfig : HokiConfig("Imyvm_VillagerShop.conf") {
    companion object{
        @JvmField
        @ConfigOption
        val LANGUAGE = Option(
            "core.language",
            "en_us",
            "the display language of Imyvm villager shop plugin"
        ) { obj: Config, path: String? ->
            obj.getString(
                path
            )
        }

        @JvmField
        @ConfigOption
        val TAX_RESTOCK = Option(
            "core.tax.restock",
            0.01,
            "The tax rate players should pay when restocking."
        ) { obj: Config, path: String? ->
            obj.getDouble(
                path
            )
        }

        @JvmField
        @ConfigOption
        val DATABASE_TYPE = Option(
            "core.database.database",
            "POSTGRESQL",
            "Database type, support \"POSTGRESQL\" , \"MYSQL\" , \"ORACLE\" or \"SQLSERVER\"\n" +
                    "However, only POSTGRESQL tests are available. Other databases are theoretically available."
        ) { obj: Config, path: String? ->
            obj.getString(
                path
            )
        }

        @JvmField
        @ConfigOption
        val DATABASE_URL = Option(
            "core.database.url",
            "jdbc:pgsql://localhost:5432/imyvmvillagershop",
            "Database connection URL. \n" +
                    "If you want to use Mysql, please use \"jdbc:mysql://localhost:3306/imyvmvillagershop\"\n" +
                    "or Oracle : jdbc:oracle:thin:@//localhost:1521/imyvmvillagershop\n" +
                    "or SQL Server : jdbc:sqlserver://localhost:32768;databaseName=imyvmvillagershop\n" +
                    "If none are available, we will generate default SQLite files in the world folder"
        ) { obj: Config, path: String? ->
            obj.getString(
                path
            )
        }

        @JvmField
        @ConfigOption
        val DATABASE_USER = Option(
            "core.database.username",
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
            "core.database.password",
            "1145141919810",
            "Database password."
        ) { obj: Config, path: String? ->
            obj.getString(
                path
            )
        }

        @JvmField
        @ConfigOption
        val DATABASE_MAXPOOLSIZE = Option(
            "core.database.maximumPoolSize",
            10,
            "Only available in mysql, used to adjust the size of the Hikari pool."
        ) { obj: Config, path: String? ->
            obj.getInt(
                path
            )
        }
    }
}
