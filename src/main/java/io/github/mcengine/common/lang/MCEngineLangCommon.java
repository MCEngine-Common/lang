package io.github.mcengine.common.lang;

import io.github.mcengine.common.lang.database.IMCEngineLangDB;
import io.github.mcengine.common.lang.database.mysql.MCEngineLangMySQL;
import io.github.mcengine.common.lang.database.postgresql.MCEngineLangPostgreSQL;
import io.github.mcengine.common.lang.database.sqlite.MCEngineLangSQLite;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * Wires a Bukkit {@link Plugin} to an {@link IMCEngineLangDB} backend and exposes
 * simple methods to get/set/change player language preferences, including resolving
 * text from YAML bundles located under:
 * <pre>{pluginDataFolder}/lang/{lang}.yml</pre>
 *
 * <p>
 * Language codes are normalized before use: they are converted to lower case and
 * underscores are replaced with hyphens (e.g., {@code en_us → en-us}).
 * </p>
 */
public final class MCEngineLangCommon {

    /** Singleton instance of the Lang common API. */
    private static MCEngineLangCommon instance;

    /** Owning plugin used for configuration and logging. */
    private final Plugin plugin;

    /** Database interface used by the Lang module. */
    private final IMCEngineLangDB db;

    /** Name of the language directory under a plugin's data folder. */
    private static final String LANG_DIR_NAME = "lang";

    /** File extension for language bundles. */
    private static final String YAML_EXTENSION = ".yml";

    /** Default language code when none is stored or the bundle is missing. */
    private static final String DEFAULT_LANG = "en-us";

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
    // Pass-through query helpers (parity with Economy)
    // ------------------------------

    /**
     * Executes a backend-specific non-returning command (DDL/DML).
     *
     * @param query SQL (for SQL backends) or DSL/JSON (for NoSQL backends)
     */
    public void executeQuery(String query) {
        db.executeQuery(query);
    }

    /**
     * Executes a backend-specific query that returns a single value.
     *
     * @param query SQL/DSL command string
     * @param type  expected Java type
     * @param <T>   generic type
     * @return value if present; otherwise {@code null}
     */
    public <T> T getValue(String query, Class<T> type) {
        return db.getValue(query, type);
    }

    // ------------------------------
    // Delegated operations
    // ------------------------------

    /** @return player's saved language (normalized) or "en-us" if absent */
    public String getLang(Player player) {
        return normalizeLang(db.getLang(player));
    }

    /** Insert/update player's language (stored normalized). */
    public void setLang(Player player, String langType) {
        db.setLang(player, normalizeLang(langType));
    }

    /** Change player's language if different; returns true if updated. */
    public boolean changeLang(Player player, String newLangType) {
        return db.changeLang(player, normalizeLang(newLangType));
    }

    // ------------------------------
    // YAML resolution
    // ------------------------------

    /**
     * Resolve a localized text from <code>{pluginDataFolder}/lang/{lang}.yml</code> using a YAML key.
     *
     * <p>Language code is normalized (lower case, underscores → hyphens).</p>
     *
     * @param plugin        the plugin owning the language files (data folder root)
     * @param player        the player whose language should be used
     * @param variableName  YAML path/key to look up (e.g., {@code ui.menu.title})
     * @return localized value if found; otherwise {@code null}
     */
    public String getLangTextFromYml(Plugin plugin, Player player, String variableName) {
        String code = getLang(player); // already normalized
        File langFile = resolveLangFile(plugin, code);

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(langFile);
        String value = cfg.getString(variableName);

        if (value == null && !DEFAULT_LANG.equals(code)) {
            File defFile = resolveLangFile(plugin, DEFAULT_LANG);
            FileConfiguration defCfg = YamlConfiguration.loadConfiguration(defFile);
            value = defCfg.getString(variableName);
        }

        return value;
    }

    /**
     * Builds a {@link File} pointing to the bundle for a language code.
     * Falls back to default when missing.
     */
    private File resolveLangFile(Plugin plugin, String code) {
        File baseDir = plugin.getDataFolder();
        File file = new File(new File(baseDir, LANG_DIR_NAME), code + YAML_EXTENSION);
        if (file.isFile()) return file;
        return new File(new File(baseDir, LANG_DIR_NAME), DEFAULT_LANG + YAML_EXTENSION);
    }

    /**
     * Normalizes language codes to lowercase and replaces underscores with hyphens.
     * <ul>
     *   <li>{@code en_US → en-us}</li>
     *   <li>{@code zh_TW → zh-tw}</li>
     *   <li>{@code FR → fr}</li>
     * </ul>
     *
     * @param input raw code from DB or config
     * @return normalized code, never null (falls back to "en-us")
     */
    private static String normalizeLang(String input) {
        if (input == null || input.isBlank()) return DEFAULT_LANG;
        return input.toLowerCase().replace('_', '-');
    }
}
