package ru.meloncode.xmas;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.meloncode.xmas.utils.TextUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

public class XMasCommand implements CommandExecutor, TabCompleter {
    public static final String PRIMARY_COMMAND = "xmastree";
    public static final String LEGACY_COMMAND = "xmas";
    private static final List<String> COMMANDS = Arrays.asList("help", "give", "end", "gifts", "reload", "addhand", "debug");
    private static final Set<String> DEBUG_TOGGLE_KEYS = new LinkedHashSet<>(Arrays.asList(
            "core.commands.legacy-command-enabled",
            "core.plugin-enabled",
            "core.holiday-ends.enabled",
            "core.holiday-ends.resource-back",
            "core.particles-enabled",
            "xmas.luck.enabled"
    ));
    private static final int DEBUG_PAGE_SIZE = 8;
    private static XMasCommand registeredExecutor;
    private static PluginCommand legacyAliasCommand;

    private final Main plugin;

    private XMasCommand(Main plugin) {
        this.plugin = plugin;
    }

    public static void register(Main plugin) {
        registeredExecutor = new XMasCommand(plugin);
        PluginCommand primaryCommand = plugin.getCommand(PRIMARY_COMMAND);
        if (primaryCommand == null) {
            throw new IllegalStateException("Primary command '/" + PRIMARY_COMMAND + "' is not defined in plugin.yml");
        }
        primaryCommand.setExecutor(registeredExecutor);
        primaryCommand.setTabCompleter(registeredExecutor);
        refreshCommandConfiguration(plugin);
    }

    public static void refreshCommandConfiguration(Main plugin) {
        if (registeredExecutor == null) {
            register(plugin);
            return;
        }
        syncLegacyAlias(plugin, registeredExecutor);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length > 0) {
            String action = args[0].toLowerCase(Locale.ENGLISH);
            switch (action) {
                case "help": {
                    for (String line : getHelpLines()) {
                        TextUtils.sendRawMessage(sender, line);
                    }
                    break;
                }
                case "give": {
                    if (args.length > 1) {
                        String name = args[1];
                        Player player = Bukkit.getPlayer(name);
                        if (player != null) {
                            player.getInventory().addItem(XMas.XMAS_CRYSTAL);
                        } else {
                            TextUtils.sendMessage(sender, LocaleManager.COMMAND_PLAYER_OFFLINE);
                        }
                    } else {
                        TextUtils.sendMessage(sender, LocaleManager.COMMAND_NO_PLAYER_NAME);
                    }
                    break;
                }
                case "end": {
                    plugin.end();
                    break;
                }
                case "gifts": {
                    Random random = new Random();
                    for (MagicTree magicTree : XMas.getAllTrees()) {
                        for (int i = 0; i < 3 + random.nextInt(4); i++) {
                            magicTree.spawnPresent();
                        }
                    }
                    Bukkit.broadcast(TextUtils.parse(LocaleManager.COMMAND_GIVEAWAY));
                    break;
                }
                case "reload": {
                    if (!sender.hasPermission("xmas.admin")) {
                        TextUtils.sendRawMessage(sender, "<red>You do not have permission to use this command.");
                        break;
                    }
                    plugin.reloadPluginConfig();
                    TextUtils.sendRawMessage(sender, "<green>" + TextUtils.DISPLAY_NAME + " configuration reloaded.");
                    break;
                }
                case "addhand": {
                    if (!(sender instanceof Player player)) {
                        TextUtils.sendRawMessage(sender, "<red>Only players can use this command.");
                        break;
                    }
                    if (!sender.hasPermission("xmas.admin")) {
                        TextUtils.sendRawMessage(sender, "<red>You do not have permission to use this command.");
                        break;
                    }
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType().isAir()) {
                        TextUtils.sendRawMessage(player, "<red>Hold an item before running " + commandPath("addhand") + ".");
                        break;
                    }
                    plugin.addGiftItem(item.clone());
                    TextUtils.sendRawMessage(player, "<green>Added the held item to the gift list.");
                    break;
                }
                case "debug": {
                    handleDebug(sender, args);
                    break;
                }

                default:
                    return false;
            }
        } else {
            sendStatus(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            String typed = args[0].toLowerCase(Locale.ENGLISH);
            for (String subCommand : COMMANDS) {
                if (subCommand.startsWith(typed)) {
                    suggestions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String typed = args[1].toLowerCase(Locale.ENGLISH);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ENGLISH).startsWith(typed)) {
                    suggestions.add(player.getName());
                }
            }
        } else if (args[0].equalsIgnoreCase("debug")) {
            if (args.length == 2) {
                suggestions.addAll(filterStartingWith(Arrays.asList("1", "2", "3", "toggle"), args[1]));
            } else if (args.length == 3 && args[1].equalsIgnoreCase("toggle")) {
                suggestions.addAll(filterStartingWith(new ArrayList<>(DEBUG_TOGGLE_KEYS), args[2]));
            } else if (args.length == 4 && args[1].equalsIgnoreCase("toggle")) {
                suggestions.addAll(filterStartingWith(Arrays.asList("true", "false"), args[3]));
            }
        }
        return suggestions;
    }

    private void sendStatus(CommandSender sender) {
        for (String line : getStatusLines()) {
            TextUtils.sendRawMessage(sender, line);
        }
    }

    private List<String> getStatusLines() {
        int treeCount = XMas.getAllTrees().size();
        Set<UUID> owners = new HashSet<>();
        for (MagicTree magicTree : XMas.getAllTrees()) {
            owners.add(magicTree.getOwner());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy kk-mm-ss");
        List<String> lines = new ArrayList<>();

        lines.add("<dark_green>" + TextUtils.DISPLAY_NAME + " " + plugin.getDescription().getVersion() + " Plugin Status");
        lines.add("");
        lines.add("<gray>Event Status: " + (Main.inProgress ? "<dark_green>In Progress" : "<red>Holidays End"));
        if (Main.inProgress) {
            lines.add("<dark_green>Current Time: <green>" + sdf.format(System.currentTimeMillis()));
            lines.add("<dark_green>Holidays end: <red>" + sdf.format(Main.endTime));
        }
        lines.add("<green>Auto-End: " + (Main.autoEnd ? "<dark_green>Yes" : "<red>No") + "<green>    |    Resource Back: " + (Main.resourceBack ? "<dark_green>Yes" : "<red>No") + "<green>    |    Particles: " + (Main.particlesEnabled ? "<dark_green>Yes" : "<red>No"));
        lines.add("");
        lines.add("<dark_green>There are <green>" + treeCount + "<dark_green> magic trees owned by <red>" + owners.size() + "<dark_green> players");
        lines.add("<dark_green>Use <red>" + commandPath("help") + "<dark_green> for command list");
        return lines;
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xmas.admin")) {
            TextUtils.sendRawMessage(sender, "<red>You do not have permission to use this command.");
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("toggle")) {
            handleDebugToggle(sender, args);
            return;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        sendDebugPage(sender, page);
    }

    private void handleDebugToggle(CommandSender sender, String[] args) {
        if (args.length < 4) {
            TextUtils.sendRawMessage(sender, "<gold>Usage: " + commandPath("debug toggle <key> true|false"));
            TextUtils.sendRawMessage(sender, "<gray>Keys: " + String.join(", ", DEBUG_TOGGLE_KEYS));
            return;
        }

        String key = args[2].toLowerCase(Locale.ENGLISH);
        if (!DEBUG_TOGGLE_KEYS.contains(key)) {
            TextUtils.sendRawMessage(sender, "<red>Unknown toggle key: " + args[2]);
            TextUtils.sendRawMessage(sender, "<gray>Keys: " + String.join(", ", DEBUG_TOGGLE_KEYS));
            return;
        }
        if (!args[3].equalsIgnoreCase("true") && !args[3].equalsIgnoreCase("false")) {
            TextUtils.sendRawMessage(sender, "<gold>Value must be true or false.");
            return;
        }

        boolean value = Boolean.parseBoolean(args[3]);
        plugin.getConfig().set(key, value);
        plugin.saveConfig();
        plugin.reloadPluginConfig();
        TextUtils.sendRawMessage(sender, "<green>Set " + key + " to " + value + " and reloaded " + TextUtils.DISPLAY_NAME + ".");
    }

    private void sendDebugPage(CommandSender sender, int requestedPage) {
        List<String> lines = new ArrayList<>(getStatusLines());
        lines.add("");
        lines.add("<gold>Commands");
        lines.add("<gray>" + commandPath("") + " - status");
        lines.add("<gray>" + commandPath("help") + " - command list");
        lines.add("<gray>" + commandPath("give <player>") + " - give a Christmas Crystal");
        lines.add("<gray>" + commandPath("gifts") + " - spawn presents under all trees");
        lines.add("<gray>" + commandPath("addhand") + " - add held item to gifts");
        lines.add("<gray>" + commandPath("reload") + " - reload config and locale");
        lines.add("<gray>" + commandPath("end") + " - end the event");
        lines.add("<gray>" + commandPath("debug [page]") + " - extended debug output");
        lines.add("<gray>" + commandPath("debug toggle <key> true|false") + " - toggle global booleans");
        if (isLegacyAliasEnabled()) {
            lines.add("<gray>Legacy alias enabled: /" + LEGACY_COMMAND);
        }
        lines.add("");
        lines.add("<gold>Permissions");
        lines.add("<gray>xmas.admin - allows all " + TextUtils.DISPLAY_NAME + " admin commands");
        lines.add("");
        lines.add("<gold>Placeholders");
        lines.add("<gray>Requires PlaceholderAPI. Use '_' after prefix, then dotted keys.");
        for (String placeholder : XMasPlaceholders.EXAMPLES) {
            lines.add("<gray>" + placeholder);
        }
        lines.add("");
        lines.add("<gold>Toggleable Config Keys");
        for (String key : DEBUG_TOGGLE_KEYS) {
            lines.add("<gray>" + key + " = " + plugin.getConfig().getBoolean(key));
        }

        int pages = Math.max(1, (int) Math.ceil((double) lines.size() / DEBUG_PAGE_SIZE));
        int page = Math.max(1, Math.min(requestedPage, pages));
        int start = (page - 1) * DEBUG_PAGE_SIZE;
        int end = Math.min(start + DEBUG_PAGE_SIZE, lines.size());

        TextUtils.sendRawMessage(sender, "<dark_green>" + TextUtils.DISPLAY_NAME + " Debug <gray>page " + page + "/" + pages);
        for (int i = start; i < end; i++) {
            TextUtils.sendRawMessage(sender, lines.get(i));
        }
        if (page < pages) {
            TextUtils.sendRawMessage(sender, "<gray>Next: " + commandPath("debug " + (page + 1)));
        }
    }

    private List<String> getHelpLines() {
        List<String> lines = new ArrayList<>();
        for (String line : LocaleManager.COMMAND_HELP) {
            lines.add(line.replace("/xmas", "/" + PRIMARY_COMMAND));
        }
        if (isLegacyAliasEnabled()) {
            lines.add("<gray>Legacy alias: <red>/" + LEGACY_COMMAND + "</red> still works.</gray>");
        }
        return lines;
    }

    private String commandPath(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return "/" + PRIMARY_COMMAND;
        }
        return "/" + PRIMARY_COMMAND + " " + suffix;
    }

    private boolean isLegacyAliasEnabled() {
        return plugin.getConfig().getBoolean("core.commands.legacy-command-enabled", true);
    }

    private static void syncLegacyAlias(Main plugin, XMasCommand executor) {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            plugin.getLogger().warning("Unable to access the Bukkit command map. Skipping legacy /xmas alias registration.");
            return;
        }
        if (!plugin.getConfig().getBoolean("core.commands.legacy-command-enabled", true)) {
            unregisterLegacyAlias(commandMap);
            return;
        }

        Command existing = commandMap.getCommand(LEGACY_COMMAND);
        if (existing instanceof PluginCommand existingPluginCommand) {
            if (existingPluginCommand.getPlugin() == plugin) {
                existingPluginCommand.setExecutor(executor);
                existingPluginCommand.setTabCompleter(executor);
                legacyAliasCommand = existingPluginCommand;
                return;
            }
            plugin.getLogger().warning("Legacy alias '/" + LEGACY_COMMAND + "' is already owned by plugin '" + existingPluginCommand.getPlugin().getName() + "'. Skipping alias registration.");
            return;
        }
        if (existing != null) {
            plugin.getLogger().warning("Legacy alias '/" + LEGACY_COMMAND + "' is already registered by another command source. Skipping alias registration.");
            return;
        }

        PluginCommand aliasCommand = createPluginCommand(plugin, LEGACY_COMMAND);
        if (aliasCommand == null) {
            plugin.getLogger().warning("Unable to create the legacy /xmas alias command.");
            return;
        }
        aliasCommand.setDescription("Legacy alias for /" + PRIMARY_COMMAND);
        aliasCommand.setUsage("/" + LEGACY_COMMAND + " [help|give|gifts|addhand|reload|debug|end]");
        aliasCommand.setPermission("xmas.admin");
        aliasCommand.setExecutor(executor);
        aliasCommand.setTabCompleter(executor);
        commandMap.register(plugin.getDescription().getName().toLowerCase(Locale.ENGLISH), aliasCommand);
        legacyAliasCommand = aliasCommand;
        plugin.getLogger().info("Registered legacy alias '/" + LEGACY_COMMAND + "' for '/" + PRIMARY_COMMAND + "'.");
    }

    private static void unregisterLegacyAlias(CommandMap commandMap) {
        if (legacyAliasCommand == null) {
            Command existing = commandMap.getCommand(LEGACY_COMMAND);
            if (existing instanceof PluginCommand existingPluginCommand && existingPluginCommand.getPlugin() == Main.getInstance()) {
                legacyAliasCommand = existingPluginCommand;
            }
        }
        if (legacyAliasCommand == null) {
            return;
        }
        legacyAliasCommand.unregister(commandMap);
        if (commandMap instanceof SimpleCommandMap simpleCommandMap) {
            try {
                Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                Object rawKnownCommands = knownCommandsField.get(simpleCommandMap);
                if (rawKnownCommands instanceof Map<?, ?> rawMap) {
                    Iterator<? extends Map.Entry<?, ?>> iterator = rawMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<?, ?> entry = iterator.next();
                        if (entry.getValue() == legacyAliasCommand) {
                            iterator.remove();
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        legacyAliasCommand = null;
    }

    private static CommandMap getCommandMap() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            Object rawCommandMap = commandMapField.get(Bukkit.getServer());
            if (rawCommandMap instanceof CommandMap commandMap) {
                return commandMap;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static PluginCommand createPluginCommand(Main plugin, String name) {
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            constructor.setAccessible(true);
            return constructor.newInstance(name, plugin);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Unable to construct dynamic command '/" + name + "': " + e.getMessage());
            return null;
        }
    }

    private List<String> filterStartingWith(List<String> values, String typed) {
        String lower = typed.toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ENGLISH).startsWith(lower)) {
                matches.add(value);
            }
        }
        return matches;
    }

}
