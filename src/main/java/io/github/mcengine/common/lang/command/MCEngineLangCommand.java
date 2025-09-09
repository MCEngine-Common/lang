package io.github.mcengine.common.lang.command;

import io.github.mcengine.common.lang.MCEngineLangCommon;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Handles the {@code /lang} command.
 *
 * <p>Usage:</p>
 * <pre>
 *   /lang                       - show current language
 *   /lang set &lt;lang&gt;           - set language (normalized to lower-case with hyphens)
 *   /lang change &lt;lang&gt;        - change language only if different
 * </pre>
 *
 * <p>
 * Language strings are passed to {@link MCEngineLangCommon}, which persists them through the
 * configured DB backend and normalizes the format (e.g., {@code en_us → en-us}).
 * </p>
 */
public final class MCEngineLangCommand implements CommandExecutor {

    /** Owning plugin for logging/messages. */
    private final Plugin plugin;

    /** Shared Lang API facade. */
    private final MCEngineLangCommon langCommon;

    /**
     * Constructs the {@code /lang} command executor.
     *
     * @param plugin     bukkit plugin instance
     * @param langCommon shared lang API (use {@link MCEngineLangCommon#getApi()} or new instance)
     */
    public MCEngineLangCommand(Plugin plugin, MCEngineLangCommon langCommon) {
        this.plugin = plugin;
        this.langCommon = langCommon;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            String current = langCommon.getLang(player);
            sender.sendMessage(ChatColor.YELLOW + "Your language is: " + ChatColor.GREEN + current);
            sender.sendMessage(ChatColor.GRAY + "Usage: /" + label + " set <lang>  or  /" + label + " change <lang>");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (("set".equals(sub) || "change".equals(sub))) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Missing <lang>. Example: /" + label + " " + sub + " en-us");
                return true;
            }
            String langArg = args[1];

            if ("set".equals(sub)) {
                langCommon.setLang(player, langArg);
                String now = langCommon.getLang(player);
                sender.sendMessage(ChatColor.GREEN + "Language set to: " + ChatColor.AQUA + now);
                return true;
            } else {
                boolean changed = langCommon.changeLang(player, langArg);
                String now = langCommon.getLang(player);
                if (changed) {
                    sender.sendMessage(ChatColor.GREEN + "Language changed to: " + ChatColor.AQUA + now);
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "No change applied. Current language: " + ChatColor.AQUA + now);
                }
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
        sender.sendMessage(ChatColor.GRAY + "Usage: /" + label + " set <lang>  or  /" + label + " change <lang>");
        return true;
    }
}
