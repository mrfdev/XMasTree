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
    public static final String PERMISSION_ADMIN = "onembxmastree.admin";
    public static final String PERMISSION_STATUS = "onembxmastree.command.status";
    public static final String PERMISSION_HELP = "onembxmastree.command.help";
    public static final String PERMISSION_GIVE = "onembxmastree.command.give";
    public static final String PERMISSION_GIFTS = "onembxmastree.command.gifts";
    public static final String PERMISSION_ADDHAND = "onembxmastree.command.addhand";
    public static final String PERMISSION_RELOAD = "onembxmastree.command.reload";
    public static final String PERMISSION_DEBUG = "onembxmastree.command.debug";
    public static final String PERMISSION_DEBUG_TOGGLE = "onembxmastree.command.debug.toggle";
    public static final String PERMISSION_END = "onembxmastree.command.end";
    public static final String PERMISSION_TREE_OVERRIDE = "onembxmastree.tree.override";
    private static final List<String> COMMANDS = Arrays.asList("help", "give", "end", "gifts", "reload", "addhand", "debug");
    private static final Set<String> DEBUG_TOGGLE_KEYS = new LinkedHashSet<>(Arrays.asList(
            "core.commands.legacy-command-enabled",
            "core.plugin-enabled",
            "core.holiday-ends.enabled",
            "core.holiday-ends.resource-back",
            "core.particles-enabled",
            "xmas.luck.enabled"
    ));
    private static final Map<String, String> DEBUG_SECTIONS = createDebugSections();
    private static final Map<String, String> PERMISSIONS = createPermissionDescriptions();
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
                    if (!hasPermission(sender, PERMISSION_HELP)) {
                        sendNoPermission(sender);
                        break;
                    }
                    for (String line : getHelpLines()) {
                        TextUtils.sendRawMessage(sender, line);
                    }
                    break;
                }
                case "give": {
                    if (!hasPermission(sender, PERMISSION_GIVE)) {
                        sendNoPermission(sender);
                        break;
                    }
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
                    if (!hasPermission(sender, PERMISSION_END)) {
                        sendNoPermission(sender);
                        break;
                    }
                    plugin.end();
                    break;
                }
                case "gifts": {
                    if (!hasPermission(sender, PERMISSION_GIFTS)) {
                        sendNoPermission(sender);
                        break;
                    }
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
                    if (!hasPermission(sender, PERMISSION_RELOAD)) {
                        sendNoPermission(sender);
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
                    if (!hasPermission(sender, PERMISSION_ADDHAND)) {
                        sendNoPermission(sender);
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
            if (!hasPermission(sender, PERMISSION_STATUS)) {
                sendNoPermission(sender);
            } else {
                sendStatus(sender);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            String typed = args[0].toLowerCase(Locale.ENGLISH);
            for (String subCommand : COMMANDS) {
                if (subCommand.startsWith(typed) && canUseSubCommand(sender, subCommand)) {
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
                List<String> debugSuggestions = new ArrayList<>(DEBUG_SECTIONS.keySet());
                if (hasPermission(sender, PERMISSION_DEBUG_TOGGLE)) {
                    debugSuggestions.add("toggle");
                }
                suggestions.addAll(filterStartingWith(debugSuggestions, args[1]));
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

        lines.add("<dark_green>" + TextUtils.DISPLAY_NAME + " <white>" + plugin.getPluginMeta().getVersion() + "</white> <dark_green>Plugin Status");
        lines.add("");
        lines.add(formatKeyValue("Event Status", Main.inProgress ? "<green>In Progress" : "<red>Holidays End"));
        if (Main.inProgress) {
            lines.add(formatKeyValue("Current Time", "<white>" + sdf.format(System.currentTimeMillis()) + "</white>"));
            lines.add(formatKeyValue("Holidays End", "<white>" + sdf.format(Main.endTime) + "</white>"));
        }
        lines.add(formatKeyValue("Auto-End", booleanValue(Main.autoEnd)));
        lines.add(formatKeyValue("Resource Back", booleanValue(Main.resourceBack)));
        lines.add(formatKeyValue("Particles", booleanValue(Main.particlesEnabled)));
        lines.add("");
        lines.add(formatKeyValue("Loaded Trees", "<white>" + treeCount + "</white>"));
        lines.add(formatKeyValue("Tree Owners", "<white>" + owners.size() + "</white>"));
        lines.add(formatKeyValue("Help", "<aqua>" + commandPath("help") + "</aqua>"));
        return lines;
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("toggle")) {
            if (!hasPermission(sender, PERMISSION_DEBUG_TOGGLE)) {
                sendNoPermission(sender);
                return;
            }
            handleDebugToggle(sender, args);
            return;
        }

        if (!hasPermission(sender, PERMISSION_DEBUG)) {
            sendNoPermission(sender);
            return;
        }

        if (args.length < 2) {
            sendDebugSection(sender, "status");
            return;
        }

        String requested = args[1];
        try {
            int page = Integer.parseInt(requested);
            sendDebugPage(sender, page);
            return;
        } catch (NumberFormatException ignored) {
        }

        String section = normalizeDebugSection(requested);
        if (section == null) {
            sendInvalidDebugSelection(sender, requested, DEBUG_SECTIONS.size());
            return;
        }
        sendDebugSection(sender, section);
    }

    private void handleDebugToggle(CommandSender sender, String[] args) {
        if (args.length < 4) {
            TextUtils.sendRawMessage(sender, formatKeyValue("Usage", "<aqua>" + commandPath("debug toggle <key> true|false") + "</aqua>"));
            TextUtils.sendRawMessage(sender, formatKeyValue("Keys", "<white>" + String.join(", ", DEBUG_TOGGLE_KEYS) + "</white>"));
            return;
        }

        String key = args[2].toLowerCase(Locale.ENGLISH);
        if (!DEBUG_TOGGLE_KEYS.contains(key)) {
            TextUtils.sendRawMessage(sender, formatKeyValue("Unknown Toggle Key", "<red>" + args[2] + "</red>"));
            TextUtils.sendRawMessage(sender, formatKeyValue("Keys", "<white>" + String.join(", ", DEBUG_TOGGLE_KEYS) + "</white>"));
            return;
        }
        if (!args[3].equalsIgnoreCase("true") && !args[3].equalsIgnoreCase("false")) {
            TextUtils.sendRawMessage(sender, formatKeyValue("Value", "<red>must be true or false</red>"));
            return;
        }

        boolean value = Boolean.parseBoolean(args[3]);
        plugin.getConfig().set(key, value);
        plugin.saveConfig();
        plugin.reloadPluginConfig();
        TextUtils.sendRawMessage(sender, formatKeyValue("Updated", "<aqua>" + key + "</aqua><dark_gray> -> </dark_gray>" + booleanValue(value)));
    }

    private LinkedHashMap<String, List<String>> buildDebugSections() {
        LinkedHashMap<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("status", getStatusLines());

        List<String> commandsPage = new ArrayList<>();
        commandsPage.add("");
        commandsPage.add(formatSectionTitle("Commands"));
        commandsPage.add(formatListEntry(commandPath(""), "status"));
        commandsPage.add(formatListEntry(commandPath("help"), "command list"));
        commandsPage.add(formatListEntry(commandPath("give <player>"), "give a Christmas Crystal"));
        commandsPage.add(formatListEntry(commandPath("gifts"), "spawn presents under all trees"));
        commandsPage.add(formatListEntry(commandPath("addhand"), "add held item to gifts"));
        commandsPage.add(formatListEntry(commandPath("reload"), "reload config and locale"));
        commandsPage.add(formatListEntry(commandPath("end"), "end the event"));
        commandsPage.add(formatListEntry(commandPath("debug"), "open the status debug section"));
        commandsPage.add(formatListEntry(commandPath("debug [section|page]"), "extended debug output by category"));
        commandsPage.add(formatListEntry(commandPath("debug toggle <key> true|false"), "toggle global booleans"));
        if (isLegacyAliasEnabled()) {
            commandsPage.add(formatKeyValue("Legacy Alias", "<aqua>/" + LEGACY_COMMAND + "</aqua>"));
        }
        sections.put("commands", commandsPage);

        List<String> permissionsPage = new ArrayList<>();
        permissionsPage.add("");
        permissionsPage.add(formatSectionTitle("Permissions"));
        for (Map.Entry<String, String> permission : PERMISSIONS.entrySet()) {
            permissionsPage.add(formatListEntry(permission.getKey(), permission.getValue()));
        }
        sections.put("permissions", permissionsPage);

        List<String> placeholdersPage = new ArrayList<>();
        placeholdersPage.add("");
        placeholdersPage.add(formatSectionTitle("Placeholders"));
        placeholdersPage.add(formatKeyValue("Notes", "<white>Requires PlaceholderAPI. Use '_' after prefix, then dotted keys.</white>"));
        for (String placeholder : XMasPlaceholders.EXAMPLES) {
            placeholdersPage.add(formatListEntry(placeholder, XMasPlaceholders.DESCRIPTIONS.getOrDefault(placeholder, "registered placeholder")));
        }
        sections.put("placeholders", placeholdersPage);

        List<String> togglesPage = new ArrayList<>();
        togglesPage.add("");
        togglesPage.add(formatSectionTitle("Toggleable Config Keys"));
        for (String key : DEBUG_TOGGLE_KEYS) {
            togglesPage.add(formatKeyValue(key, booleanValue(plugin.getConfig().getBoolean(key))));
        }
        sections.put("config", togglesPage);

        return sections;
    }

    private String normalizeDebugSection(String requested) {
        if (requested == null || requested.isBlank()) {
            return "status";
        }
        return switch (requested.toLowerCase(Locale.ENGLISH)) {
            case "status" -> "status";
            case "commands", "command" -> "commands";
            case "permissions", "permission", "perms", "perm" -> "permissions";
            case "placeholders", "placeholder", "papi" -> "placeholders";
            case "config", "configuration", "cfg", "toggles" -> "config";
            default -> null;
        };
    }

    private void sendDebugSection(CommandSender sender, String sectionKey) {
        LinkedHashMap<String, List<String>> sections = buildDebugSections();
        if (!sections.containsKey(sectionKey)) {
            sendInvalidDebugSelection(sender, sectionKey, sections.size());
            return;
        }
        renderDebugSection(sender, sectionKey, sections);
    }

    private void sendDebugPage(CommandSender sender, int requestedPage) {
        LinkedHashMap<String, List<String>> sections = buildDebugSections();
        List<String> sectionKeys = new ArrayList<>(sections.keySet());
        if (requestedPage < 1 || requestedPage > sectionKeys.size()) {
            sendInvalidDebugSelection(sender, Integer.toString(requestedPage), sectionKeys.size());
            return;
        }
        renderDebugSection(sender, sectionKeys.get(requestedPage - 1), sections);
    }

    private void renderDebugSection(CommandSender sender, String sectionKey, LinkedHashMap<String, List<String>> sections) {
        List<String> sectionKeys = new ArrayList<>(sections.keySet());
        int sectionIndex = sectionKeys.indexOf(sectionKey);
        int page = sectionIndex + 1;
        int pageCount = sectionKeys.size();

        TextUtils.sendRawMessage(sender, "<dark_green>" + TextUtils.DISPLAY_NAME + " Debug <white>" + DEBUG_SECTIONS.getOrDefault(sectionKey, sectionKey) + "</white><dark_gray> (" + page + "/" + pageCount + ")</dark_gray>");
        for (String line : sections.get(sectionKey)) {
            TextUtils.sendRawMessage(sender, line);
        }
        if (page < pageCount) {
            String nextSection = sectionKeys.get(sectionIndex + 1);
            TextUtils.sendRawMessage(sender, formatKeyValue("Next", "<aqua>" + commandPath("debug " + nextSection) + "</aqua>"));
        }
    }

    private void sendInvalidDebugSelection(CommandSender sender, String requested, int pageCount) {
        TextUtils.sendRawMessage(sender, formatKeyValue("Debug Sections", "<white>" + String.join(", ", DEBUG_SECTIONS.keySet()) + "</white>"));
        TextUtils.sendRawMessage(sender, formatKeyValue("Debug Pages", "<white>1-" + pageCount + "</white>"));
        TextUtils.sendRawMessage(sender, formatKeyValue("Requested", "<red>" + requested + "</red>"));
        TextUtils.sendRawMessage(sender, formatKeyValue("Try", "<aqua>" + commandPath("debug status") + "</aqua>"));
    }

    private List<String> getHelpLines() {
        List<String> lines = new ArrayList<>();
        for (String line : LocaleManager.COMMAND_HELP) {
            lines.add(line.replaceAll("/" + LEGACY_COMMAND + "(?![A-Za-z])", "/" + PRIMARY_COMMAND));
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

    public static boolean canOverrideTree(CommandSender sender) {
        return hasPermission(sender, PERMISSION_TREE_OVERRIDE);
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
        aliasCommand.setUsage("/" + LEGACY_COMMAND + " [help|give|gifts|addhand|reload|debug [section|page]|end]");
        aliasCommand.setPermission(null);
        aliasCommand.setExecutor(executor);
        aliasCommand.setTabCompleter(executor);
        commandMap.register(plugin.getPluginMeta().getName().toLowerCase(Locale.ENGLISH), aliasCommand);
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
                    List<Object> keysToRemove = new ArrayList<>();
                    for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                        if (entry.getValue() == legacyAliasCommand) {
                            keysToRemove.add(entry.getKey());
                        }
                    }
                    for (Object key : keysToRemove) {
                        removeKnownCommand(rawMap, key);
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        legacyAliasCommand = null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void removeKnownCommand(Map<?, ?> rawMap, Object key) {
        ((Map) rawMap).remove(key);
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

    private boolean canUseSubCommand(CommandSender sender, String subCommand) {
        return switch (subCommand.toLowerCase(Locale.ENGLISH)) {
            case "help" -> hasPermission(sender, PERMISSION_HELP);
            case "give" -> hasPermission(sender, PERMISSION_GIVE);
            case "end" -> hasPermission(sender, PERMISSION_END);
            case "gifts" -> hasPermission(sender, PERMISSION_GIFTS);
            case "reload" -> hasPermission(sender, PERMISSION_RELOAD);
            case "addhand" -> hasPermission(sender, PERMISSION_ADDHAND);
            case "debug" -> hasPermission(sender, PERMISSION_DEBUG) || hasPermission(sender, PERMISSION_DEBUG_TOGGLE);
            default -> false;
        };
    }

    private static boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(PERMISSION_ADMIN) || sender.hasPermission(permission);
    }

    private void sendNoPermission(CommandSender sender) {
        TextUtils.sendRawMessage(sender, "<red>You do not have permission to use this command.");
    }

    private String formatSectionTitle(String title) {
        return "<gold><bold>" + title + "</bold></gold>";
    }

    private String formatListEntry(String key, String value) {
        return "<aqua>" + key + "</aqua><dark_gray> : </dark_gray><white>" + value + "</white>";
    }

    private String formatKeyValue(String key, String value) {
        return "<yellow>" + key + "</yellow><dark_gray>: </dark_gray>" + value;
    }

    private String booleanValue(boolean value) {
        return value ? "<green>true</green>" : "<red>false</red>";
    }

    private static Map<String, String> createPermissionDescriptions() {
        Map<String, String> permissions = new LinkedHashMap<>();
        permissions.put(PERMISSION_ADMIN, "allows all " + TextUtils.DISPLAY_NAME + " commands and overrides");
        permissions.put(PERMISSION_STATUS, "shows /" + PRIMARY_COMMAND + " status output");
        permissions.put(PERMISSION_HELP, "shows /" + PRIMARY_COMMAND + " help output");
        permissions.put(PERMISSION_GIVE, "allows /" + PRIMARY_COMMAND + " give");
        permissions.put(PERMISSION_GIFTS, "allows /" + PRIMARY_COMMAND + " gifts");
        permissions.put(PERMISSION_ADDHAND, "allows /" + PRIMARY_COMMAND + " addhand");
        permissions.put(PERMISSION_RELOAD, "allows /" + PRIMARY_COMMAND + " reload");
        permissions.put(PERMISSION_DEBUG, "allows /" + PRIMARY_COMMAND + " debug");
        permissions.put(PERMISSION_DEBUG_TOGGLE, "allows /" + PRIMARY_COMMAND + " debug toggle");
        permissions.put(PERMISSION_END, "allows /" + PRIMARY_COMMAND + " end");
        permissions.put(PERMISSION_TREE_OVERRIDE, "allows managing other players' trees");
        return permissions;
    }

    private static Map<String, String> createDebugSections() {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("status", "Status");
        sections.put("commands", "Commands");
        sections.put("permissions", "Permissions");
        sections.put("placeholders", "Placeholders");
        sections.put("config", "Config");
        return sections;
    }

}
