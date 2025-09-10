package io.github.mcengine.common.lang.database;

import org.bukkit.entity.Player;

/**
 * Contract for the MCEngine Lang database layer.
 *
 * <p>
 * This interface abstracts how player language preferences are stored and retrieved
 * from a database. Implementations must ensure the following table exists:
 * </p>
 *
 * <pre>
 * CREATE TABLE IF NOT EXISTS lang (
 *   player_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
 *   lang        VARCHAR(32) NOT NULL DEFAULT 'en_US'
 * );
 * </pre>
 *
 * <p>
 * Notes:
 * <ul>
 *   <li>{@code player_uuid} is the player's {@link java.util.UUID#toString()}.</li>
 *   <li>{@code lang} holds the player's language tag (e.g., {@code en_US}, {@code en_GB}, {@code zh_TW}).</li>
 *   <li>Default language is {@code en_US} to match the requested schema.</li>
 * </ul>
 * </p>
 */
public interface IMCEngineLangDB {

    /**
     * Executes a backend-specific non-returning command (DDL/DML).
     * <p>For SQL backends, this is raw SQL. For NoSQL backends, this could be JSON/DSL.</p>
     *
     * @param query command to execute
     */
    void executeQuery(String query);

    /**
     * Executes a backend-specific query that returns a single value (one row, one column).
     * <p>For SQL backends, this is a {@code SELECT} returning a single column.</p>
     *
     * @param query query/command to execute
     * @param type  expected Java type ({@code String, Integer, Long, Double, Boolean}, etc.)
     * @param <T>   generic return type
     * @return value if present; otherwise {@code null}
     * @throws IllegalArgumentException if {@code type} is unsupported
     */
    <T> T getValue(String query, Class<T> type);

    /**
     * Reads the player's language code from the {@code lang} table.
     *
     * @param player Bukkit player
     * @return the stored language (e.g., {@code en_US}); if not present, return {@code "en_US"}
     */
    String getLang(Player player);

    /**
     * Sets (inserts or updates) the player's language code in the {@code lang} table.
     *
     * @param player    Bukkit player
     * @param langType  language code to persist (e.g., {@code en_US})
     */
    void setLang(Player player, String langType);

    /**
     * Changes the player's language code only if the new value differs from the current.
     *
     * @param player       Bukkit player
     * @param newLangType  target language code (e.g., {@code en_US})
     * @return {@code true} if a change was applied; {@code false} if no row was updated or value unchanged
     */
    boolean changeLang(Player player, String newLangType);
}
