package io.github.mcengine.common.lang.tabcompleter;

import io.github.mcengine.common.lang.MCEngineLangCommon;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tab completion for the {@code /lang} command.
 *
 * <p>
 * Suggestions:
 * <ul>
 *   <li>First argument: {@code set}, {@code change}</li>
 *   <li>Second argument: language codes derived from files in {@code {pluginDataFolder}/lang/*.yml}</li>
 * </ul>
 * All suggestions are lower-case with hyphens, matching {@link MCEngineLangCommon}'s normalization.
 * </p>
 */
public final class MCEngineLangTabCompleter implements TabCompleter {

    /** Subcommands offered at arg index 0. */
    private static final List<String> SUBS = Arrays.asList("set", "change");

    /** Owning plugin used to locate the lang directory. */
    private final Plugin plugin;

    public MCEngineLangTabCompleter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            for (String s : SUBS) {
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }

        if (args.length == 2) {
            String prefix = normalize(args[1]);
            List<String> candidates = listAvailableCodes();
            List<String> out = new ArrayList<>();
            for (String c : candidates) {
                if (c.startsWith(prefix)) out.add(c);
            }
            return out;
        }

        return List.of();
    }

    /**
     * Lists available language codes from {@code {dataFolder}/lang/*.yml}.
     * Filenames are converted to lower-case and underscores are replaced with hyphens.
     */
    private List<String> listAvailableCodes() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        File[] files = langDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) return List.of("en-us");

        List<String> out = new ArrayList<>(files.length);
        for (File f : files) {
            String name = f.getName();
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                String base = name.substring(0, dot);
                out.add(normalize(base));
            }
        }
        if (out.isEmpty()) return List.of("en-us");
        return out;
        }

    /** Normalizes a language code to lower-case and replaces underscores with hyphens. */
    private static String normalize(String input) {
        if (input == null || input.isBlank()) return "en-us";
        return input.toLowerCase().replace('_', '-');
    }
}
