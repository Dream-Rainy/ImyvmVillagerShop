package com.imyvm.villagerShop.apis

import com.imyvm.villagerShop.VillagerShopMain.Companion.LOGGER
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_MAXPOOLSIZE
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_PASSWORD
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_TYPE
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_URL
import com.imyvm.villagerShop.apis.ModConfig.Companion.DATABASE_USER
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DbSettings {
    val db by lazy {
        var database: Database = initializeDatabase()
        if (!checkDatabaseConnection(database)) {
            LOGGER.error("Failed to connect to specified database. Defaulting to H2.")
            database = initializeH2Database()
        }
        database
    }

    private fun initializeDatabase(): Database {
        return when (DATABASE_TYPE.value) {
            "POSTGRESQL" -> configInitialization("org.postgresql.Driver", "jdbc:postgresql://${DATABASE_URL.value}")
            "MYSQL" -> initializeMySQLDatabase()
            "H2" -> initializeH2Database()
            else -> {
                LOGGER.error("Unsupported database type: ${DATABASE_TYPE.value}. Defaulting to H2.")
                initializeH2Database()
            }
        }
    }

    private fun initializeMySQLDatabase(): Database {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${DATABASE_URL.value}"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = DATABASE_USER.value
            password = DATABASE_PASSWORD.value
            maximumPoolSize = DATABASE_MAXPOOLSIZE.value
        }
        val dataSource = HikariDataSource(config)
        return Database.connect(dataSource)
    }

    private fun initializeH2Database(): Database {
        return configInitialization("org.h2.Driver", "jdbc:h2:./world/imyvm_villagershop")
    }

    private fun configInitialization(driver: String, url: String): Database {
        return Database.connect(
            url,
            driver = driver,
            user = DATABASE_USER.value,
            password = DATABASE_PASSWORD.value
        )
    }

    private fun checkDatabaseConnection(database: Database): Boolean {
        return try {
            transaction(database) {
                exec("SELECT 1") { rs ->
                    if (rs.next()) rs.getInt(1) == 1 else false
                }
            } == true
        } catch (e: Exception) {
            LOGGER.error("Database connection check failed: ${e.message}")
            false
        }
    }
}