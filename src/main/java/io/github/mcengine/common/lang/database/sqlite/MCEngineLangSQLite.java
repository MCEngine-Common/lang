package io.github.mcengine.common.lang.database.sqlite;

import io.github.mcengine.common.lang.database.IMCEngineLangDB;
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
 */
public final class MCEngineLangSQLite implements IMCEngineLangDB {

    /** Owning plugin for configuration and logging. */
    private final Plugin plugin;

    /** JDBC URL for the SQLite database file. */
    private final String databaseUrl;

    /** Shared SQLite connection for this instance. */
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
        if (this.conn == null) return "en_US";
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement("SELECT lang FROM lang WHERE player_uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite getLang failed: " + e.getMessage());
        }
        return "en_US";
    }

    /** {@inheritDoc} */
    @Override
    public void setLang(Player player, String langType) {
        if (this.conn == null) return;
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lang (player_uuid, lang) VALUES (?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET lang=excluded.lang")) {
            ps.setString(1, uuid);
            ps.setString(2, langType);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite setLang failed: " + e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean changeLang(Player player, String newLangType) {
        if (this.conn == null) return false;
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE lang SET lang=? WHERE player_uuid=? AND lang<>?")) {
            ps.setString(1, newLangType);
            ps.setString(2, uuid);
            ps.setString(3, newLangType);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                // If row doesn't exist, insert with newLangType
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO lang (player_uuid, lang) VALUES (?, ?) " +
                        "ON CONFLICT(player_uuid) DO UPDATE SET lang=excluded.lang")) {
                    ins.setString(1, uuid);
                    ins.setString(2, newLangType);
                    return ins.executeUpdate() > 0;
                }
            }
            return updated > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite changeLang failed: " + e.getMessage());
            return false;
        }
    }
}
