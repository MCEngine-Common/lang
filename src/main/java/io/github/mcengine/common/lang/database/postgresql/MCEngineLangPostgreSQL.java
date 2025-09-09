package io.github.mcengine.common.lang.database.postgresql;

import io.github.mcengine.common.lang.database.IMCEngineLangDB;
import io.github.mcengine.common.lang.database.postgresql.util.changeLangUtil;
import io.github.mcengine.common.lang.database.postgresql.util.getLangUtil;
import io.github.mcengine.common.lang.database.postgresql.util.setLangUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * PostgreSQL implementation of the Lang database.
 *
 * <p>Schema (ensured on init):</p>
 * <pre>
 * CREATE TABLE IF NOT EXISTS lang (
 *   player_uuid VARCHAR(36) PRIMARY KEY,
 *   lang        VARCHAR(32) NOT NULL DEFAULT 'en_US'
 * );
 * </pre>
 *
 * <p>
 * Contract methods delegate their SQL to focused util classes under
 * {@code io.github.mcengine.common.lang.database.postgresql.util}.
 * </p>
 */
public final class MCEngineLangPostgreSQL implements IMCEngineLangDB {

    /** Owning plugin for configuration and logging. */
    private final Plugin plugin;

    /** JDBC URL and credentials from config. */
    private final String jdbcUrl;
    private final String user, pass;

    /** Shared PostgreSQL connection for this instance. */
    private final Connection conn;

    public MCEngineLangPostgreSQL(Plugin plugin) {
        this.plugin = plugin;

        String host = plugin.getConfig().getString("database.postgresql.host", "localhost");
        int port = plugin.getConfig().getInt("database.postgresql.port", 5432);
        String db   = plugin.getConfig().getString("database.postgresql.database", "mcengine");
        this.user   = plugin.getConfig().getString("database.postgresql.user", "postgres");
        this.pass   = plugin.getConfig().getString("database.postgresql.password", "");

        this.jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db;

        Connection tmp = null;
        try {
            tmp = DriverManager.getConnection(jdbcUrl, user, pass);
            ensureSchema(tmp);
        } catch (SQLException e) {
            plugin.getLogger().warning("PostgreSQL (Lang) connect/ensure failed: " + e.getMessage());
            e.printStackTrace();
        }
        this.conn = tmp;
    }

    /** Ensures the {@code lang} table exists. */
    private void ensureSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS lang (" +
                "  player_uuid VARCHAR(36) PRIMARY KEY," +
                "  lang        VARCHAR(32) NOT NULL DEFAULT 'en_US'" +
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
