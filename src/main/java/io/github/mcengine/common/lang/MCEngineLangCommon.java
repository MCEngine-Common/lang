package io.github.mcengine.common.lang;

import io.github.mcengine.common.lang.database.IMCEngineLangDB;
import io.github.mcengine.common.lang.database.mysql.MCEngineLangMySQL;
import io.github.mcengine.common.lang.database.postgresql.MCEngineLangPostgreSQL;
import io.github.mcengine.common.lang.database.sqlite.MCEngineLangSQLite;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Wires a Bukkit {@link Plugin} to an {@link IMCEngineLangDB} backend and exposes
 * simple methods to get/set/change player language preferences.
 */
public final class MCEngineLangCommon {

    /** Singleton instance of the Lang common API. */
    private static MCEngineLangCommon instance;

    /** Owning plugin used for configuration and logging. */
    private final Plugin plugin;

    /** Database interface used by the Lang module. */
    private final IMCEngineLangDB db;

    /**
     * Constructs the Lang API and selects the database implementation from config
     * ({@code database.type}: sqlite | mysql | postgresql).
     *
     * @param plugin Bukkit plugin instance
     */
    public MCEngineLangCommon(Plugin plugin) {
        instance = this;
        this.plugin = plugin;

        String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        switch (dbType) {
            case "sqlite" -> this.db = new MCEngineLangSQLite(plugin);
            case "mysql" -> this.db = new MCEngineLangMySQL(plugin);
            case "postgresql" -> this.db = new MCEngineLangPostgreSQL(plugin);
            default -> throw new IllegalArgumentException("Unsupported database type for Lang: " + dbType);
        }
    }

    /** Returns the global Lang API singleton instance. */
    public static MCEngineLangCommon getApi() { return instance; }

    /** Returns the Bukkit plugin instance. */
    public Plugin getPlugin() { return plugin; }

    /** Returns the database interface used by this module. */
    public IMCEngineLangDB getDB() { return db; }

    // ------------------------------
    // Delegated operations
    // ------------------------------

    /** @return player's saved language or "en_US" if absent */
    public String getLang(Player player) { return db.getLang(player); }

    /** Insert/update player's language. */
    public void setLang(Player player, String langType) { db.setLang(player, langType); }

    /** Change player's language if different; returns true if updated. */
    public boolean changeLang(Player player, String newLangType) { return db.changeLang(player, newLangType); }
}
