package io.github.mcengine.common.lang.database.sqlite;

import io.github.mcengine.common.lang.database.IMCEngineLangDB;
import io.github.mcengine.common.lang.database.sqlite.util.changeLangUtil;
import io.github.mcengine.common.lang.database.sqlite.util.getLangUtil;
import io.github.mcengine.common.lang.database.sqlite.util.setLangUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;

/**
 * SQLite implementation of the Lang database.
 *
 * <p>Schema (ensured on init):</p>
 * <pre>
 * CREATE TABLE IF NOT EXISTS lang (
 *   player_uuid TEXT NOT NULL PRIMARY KEY,
 *   lang        TEXT NOT NULL DEFAULT 'en_US'
 * );
 * </pre>
 *
 * <p>
 * Contract methods delegate their SQL to focused util classes under
 * {@code io.github.mcengine.common.lang.database.sqlite.util}.
 * </p>
 */
public final class MCEngineLangSQLite implements IMCEngineLangDB {

    /** Owning plugin for configuration and logging. */
    private final Plugin plugin;

    /** JDBC URL for the SQLite database file. */
    private final String databaseUrl;

    /**
     * Persistent SQLite JDBC connection shared by this implementation.
     * <p>
     * @implNote Contract methods delegate their SQL to small, focused utility classes
     * (one util per method) that expose a static {@code invoke(...)} entrypoint.
     */
    private final Connection conn;

    /**
     * Builds the SQLite connection from config:
     * <ul>
     *   <li>{@code database.sqlite.path} → DB file in plugin data folder (default: {@code lang.db})</li>
     * </ul>
     *
     * @param plugin Bukkit plugin instance
     */
    public MCEngineLangSQLite(Plugin plugin) {
        this.plugin = plugin;
        String fileName = plugin.getConfig().getString("database.sqlite.path", "lang.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);

        try {
            if (!dbFile.exists()) {
                if (dbFile.getParentFile() != null) dbFile.getParentFile().mkdirs();
                dbFile.createNewFile();
                plugin.getLogger().info("SQLite (Lang) created: " + dbFile.getAbsolutePath());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create SQLite Lang DB file: " + e.getMessage());
        }

        this.databaseUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        Connection tmp = null;
        try {
            tmp = DriverManager.getConnection(databaseUrl);
            try (Statement pragma = tmp.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }
            ensureSchema(tmp);
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite (Lang) connect/ensure failed: " + e.getMessage());
            e.printStackTrace();
        }
        this.conn = tmp;
    }

    /** Ensures the {@code lang} table exists. */
    private void ensureSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS lang (" +
                "  player_uuid TEXT NOT NULL PRIMARY KEY," +
                "  lang        TEXT NOT NULL DEFAULT 'en_US'" +
                ")"
            );
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_lang_lang ON lang(lang)");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection(Connection conn) {
        return (conn != null) ? conn : this.conn;
    }

    /** {@inheritDoc} */
    @Override
    public String getLang(Player player) {
        return getLangUtil.invoke(this.conn, this.plugin, player);
    }

    /** {@inheritDoc} */
    @Override
    public void setLang(Player player, String langType) {
        setLangUtil.invoke(this.conn, this.plugin, player, langType);
    }

    /** {@inheritDoc} */
    @Override
    public boolean changeLang(Player player, String newLangType) {
        return changeLangUtil.invoke(this.conn, this.plugin, player, newLangType);
    }
}
