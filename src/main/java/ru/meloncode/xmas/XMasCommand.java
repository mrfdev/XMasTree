package ru.meloncode.xmas;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import ru.meloncode.xmas.utils.TextUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

public class XMasCommand implements CommandExecutor, TabCompleter {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
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
    public static final String PERMISSION_INSPECT = "onembxmastree.command.inspect";
    public static final String PERMISSION_TEST = "onembxmastree.command.test";
    public static final String PERMISSION_DATA = "onembxmastree.command.data";
    public static final String PERMISSION_END = "onembxmastree.command.end";
    public static final String PERMISSION_TREE_OVERRIDE = "onembxmastree.tree.override";
    private static final List<String> COMMANDS = Arrays.asList("help", "give", "end", "gifts", "reload", "addhand", "debug", "inspect", "test", "data");
    private static final Set<String> DEBUG_TOGGLE_KEYS = new LinkedHashSet<>(Arrays.asList(
            "core.commands.legacy-command-enabled",
            "core.plugin-enabled",
            "core.holiday-ends.enabled",
            "core.holiday-ends.resource-back",
            "core.particles-enabled",
            "xmas.luck.enabled"
    ));
    private static final Map<String, String> DEBUG_SECTIONS = createDebugSections();
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
                    handleGifts(sender, args);
                    break;
                }
                case "reload": {
                    if (!hasPermission(sender, PERMISSION_RELOAD)) {
                        sendNoPermission(sender);
                        break;
                    }
                    Main.ReloadSummary summary = plugin.reloadPluginConfig();
                    TextUtils.sendRawMessage(sender, LocaleManager.text("command.reload-success", TextUtils.success(TextUtils.displayName() + " configuration reloaded.")));
                    for (String line : getReloadSummaryLines(summary)) {
                        TextUtils.sendRawMessage(sender, line);
                    }
                    break;
                }
                case "inspect": {
                    if (!hasPermission(sender, PERMISSION_INSPECT)) {
                        sendNoPermission(sender);
                        break;
                    }
                    handleInspect(sender, args);
                    break;
                }
                case "test": {
                    if (!hasPermission(sender, PERMISSION_TEST)) {
                        sendNoPermission(sender);
                        break;
                    }
                    handleTest(sender, args);
                    break;
                }
                case "data": {
                    if (!hasPermission(sender, PERMISSION_DATA)) {
                        sendNoPermission(sender);
                        break;
                    }
                    handleData(sender, args);
                    break;
                }
                case "addhand": {
                    if (!(sender instanceof Player player)) {
                        TextUtils.sendRawMessage(sender, LocaleManager.text("command.player-only", TextUtils.error("Only players can use this command.")));
                        break;
                    }
                    if (!hasPermission(sender, PERMISSION_ADDHAND)) {
                        sendNoPermission(sender);
                        break;
                    }
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType().isAir()) {
                        TextUtils.sendRawMessage(player, replaceToken(
                                LocaleManager.text("command.hold-item-first", TextUtils.error("Hold an item before running {command}.")),
                                "command",
                                commandPath("addhand")
                        ));
                        break;
                    }
                    plugin.addGiftItem(item.clone());
                    TextUtils.sendRawMessage(player, LocaleManager.text("command.gift-added", TextUtils.success("Added the held item to the gift list.")));
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
        } else if (args[0].equalsIgnoreCase("inspect")) {
            if (args.length == 2) {
                List<String> inspectSuggestions = new ArrayList<>();
                inspectSuggestions.add("nearest");
                for (MagicTree tree : XMas.getAllTrees()) {
                    inspectSuggestions.add(tree.getTreeUID().toString());
                }
                suggestions.addAll(filterStartingWith(inspectSuggestions, args[1]));
            } else if (args.length == 3 && args[1].equalsIgnoreCase("nearest")) {
                String typed = args[2].toLowerCase(Locale.ENGLISH);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase(Locale.ENGLISH).startsWith(typed)) {
                        suggestions.add(player.getName());
                    }
                }
            }
        } else if (args[0].equalsIgnoreCase("test")) {
            if (args.length == 2) {
                suggestions.addAll(filterStartingWith(Arrays.asList("sound", "particle"), args[1]));
            } else if (args.length == 3 && args[1].equalsIgnoreCase("sound")) {
                suggestions.addAll(filterStartingWith(Arrays.asList("first", "repeat"), args[2]));
            } else if (args.length == 3 && args[1].equalsIgnoreCase("particle")) {
                suggestions.addAll(filterStartingWith(treeLevelNames(), args[2]));
            } else if (args.length == 4 && args[1].equalsIgnoreCase("particle")) {
                suggestions.addAll(filterStartingWith(Arrays.asList("all", "ambient", "swag", "body"), args[3]));
            } else if ((args.length == 4 && args[1].equalsIgnoreCase("sound")) || (args.length == 5 && args[1].equalsIgnoreCase("particle"))) {
                String typed = args[args.length - 1].toLowerCase(Locale.ENGLISH);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase(Locale.ENGLISH).startsWith(typed)) {
                        suggestions.add(player.getName());
                    }
                }
            }
        } else if (args[0].equalsIgnoreCase("data")) {
            if (args.length == 2) {
                suggestions.addAll(filterStartingWith(Arrays.asList("backup", "validate", "migrate-world"), args[1]));
            } else if (args.length == 3 && args[1].equalsIgnoreCase("migrate-world")) {
                suggestions.addAll(filterStartingWith(getWorldNames(), args[2]));
            } else if (args.length == 4 && args[1].equalsIgnoreCase("migrate-world")) {
                suggestions.addAll(filterStartingWith(getWorldNames(), args[3]));
            } else if (args.length == 5 && args[1].equalsIgnoreCase("migrate-world")) {
                suggestions.addAll(filterStartingWith(Arrays.asList("dry-run", "apply"), args[4]));
            }
        } else if (args[0].equalsIgnoreCase("gifts")) {
            if (args.length == 2) {
                suggestions.addAll(filterStartingWith(Arrays.asList("list", "roll", "remove", "spawn"), args[1]));
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

    private void handleGifts(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("spawn")) {
            spawnPresentsUnderTrees();
            return;
        }
        if (args[1].equalsIgnoreCase("list")) {
            int page = parsePositiveInt(args.length >= 3 ? args[2] : null, 1);
            sendGiftList(sender, page);
            return;
        }
        if (args[1].equalsIgnoreCase("roll")) {
            sendGiftRoll(sender);
            return;
        }
        if (args[1].equalsIgnoreCase("remove")) {
            handleGiftRemove(sender, args);
            return;
        }

        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.gifts.labels.unknown-action", "Unknown Gift Action"), TextUtils.error(args[1])));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.gifts.labels.usage", "Usage"), TextUtils.command(commandPath("gifts list"))));
    }

    private void spawnPresentsUnderTrees() {
        Random random = new Random();
        for (MagicTree magicTree : XMas.getAllTrees()) {
            for (int i = 0; i < 3 + random.nextInt(4); i++) {
                magicTree.spawnPresent();
            }
        }
        Bukkit.broadcast(TextUtils.parse(LocaleManager.COMMAND_GIVEAWAY));
    }

    private void sendGiftList(CommandSender sender, int page) {
        List<ItemStack> giftItems = plugin.getGiftItems();
        int pageSize = 8;
        int pageCount = Math.max(1, (int) Math.ceil(giftItems.size() / (double) pageSize));
        page = Math.max(1, Math.min(page, pageCount));
        int start = (page - 1) * pageSize;
        int end = Math.min(giftItems.size(), start + pageSize);

        TextUtils.sendRawMessage(sender, LocaleManager.text("ui.gifts.title", TextUtils.title("Gift Pool")) + " " + TextUtils.muted("(" + page + "/" + pageCount + ")"));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.gifts.labels.total", "Total"), TextUtils.text(Integer.toString(giftItems.size()))));
        if (giftItems.isEmpty()) {
            TextUtils.sendRawMessage(sender, LocaleManager.text("ui.gifts.values.empty", TextUtils.warning("No gifts are configured.")));
            return;
        }
        for (int index = start; index < end; index++) {
            TextUtils.sendRawMessage(sender, formatStyledListEntry(Integer.toString(index + 1), formatGiftItem(giftItems.get(index))));
        }
        if (page < pageCount) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.next", "Next"), TextUtils.command(commandPath("gifts list " + (page + 1)))));
        }
    }

    private void sendGiftRoll(CommandSender sender) {
        ItemStack gift = plugin.rollGiftItem();
        if (gift == null) {
            TextUtils.sendRawMessage(sender, LocaleManager.text("ui.gifts.values.empty", TextUtils.warning("No gifts are configured.")));
            return;
        }
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.gifts.labels.rolled", "Rolled"), formatGiftItem(gift)));
    }

    private void handleGiftRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.gifts.labels.usage", "Usage"), TextUtils.command(commandPath("gifts remove <index>"))));
            return;
        }
        int index = parsePositiveInt(args[2], -1);
        if (index < 1) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.gifts.labels.index", "Index"), TextUtils.error(args[2])));
            return;
        }
        ItemStack removed = plugin.removeGiftItem(index);
        if (removed == null) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.gifts.labels.index", "Index"), TextUtils.error(Integer.toString(index))));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.gifts.labels.try", "Try"), TextUtils.command(commandPath("gifts list"))));
            return;
        }
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.gifts.labels.removed", "Removed"), formatGiftItem(removed)));
    }

    private void handleTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.usage", "Usage"), TextUtils.command(commandPath("test sound first|repeat [player]"))));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.usage", "Usage"), TextUtils.command(commandPath("test particle <level> [all|ambient|swag|body] [player]"))));
            return;
        }

        if (args[1].equalsIgnoreCase("sound")) {
            handleTestSound(sender, args);
            return;
        }
        if (args[1].equalsIgnoreCase("particle")) {
            handleTestParticle(sender, args);
            return;
        }

        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.unknown-test", "Unknown Test"), TextUtils.error(args[1])));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.try", "Try"), TextUtils.command(commandPath("test sound first"))));
    }

    private void handleTestSound(CommandSender sender, String[] args) {
        if (args.length < 3) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.usage", "Usage"), TextUtils.command(commandPath("test sound first|repeat [player]"))));
            return;
        }

        boolean repeat;
        if (args[2].equalsIgnoreCase("first")) {
            repeat = false;
        } else if (args[2].equalsIgnoreCase("repeat")) {
            repeat = true;
        } else {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.sound", "Sound"), TextUtils.error(args[2])));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.try", "Try"), TextUtils.command(commandPath("test sound first"))));
            return;
        }

        Player target = resolveCommandTarget(sender, args, 3, commandPath("test sound first <player>"));
        if (target == null) {
            return;
        }

        float volume = repeat ? Main.growRepeatSoundVolume : Main.growFirstSoundVolume;
        if (volume > 0) {
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, volume, 0.8f);
        }
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.sound", "Sound"), TextUtils.text(repeat ? "repeat" : "first")));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.target", "Target"), TextUtils.text(target.getName())));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.volume", "Volume"), TextUtils.text(Float.toString(volume))));
    }

    private void handleTestParticle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.usage", "Usage"), TextUtils.command(commandPath("test particle <level> [all|ambient|swag|body] [player]"))));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.levels", "Levels"), TextUtils.text(String.join(", ", treeLevelNames()))));
            return;
        }

        TreeLevel level = treeLevelByName(args[2]);
        if (level == null) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.level", "Level"), TextUtils.error(args[2])));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.levels", "Levels"), TextUtils.text(String.join(", ", treeLevelNames()))));
            return;
        }

        String effectName = "all";
        int targetArgIndex = 3;
        if (args.length >= 4 && isParticleEffectName(args[3])) {
            effectName = args[3].toLowerCase(Locale.ENGLISH);
            targetArgIndex = 4;
        }

        Player target = resolveCommandTarget(sender, args, targetArgIndex, commandPath("test particle <level> [all|ambient|swag|body] <player>"));
        if (target == null) {
            return;
        }

        int played = playParticlePreview(level, effectName, target.getLocation());
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.particle", "Particle"), TextUtils.text(level.getLevelName() + " " + effectName)));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.target", "Target"), TextUtils.text(target.getName())));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.effects-played", "Effects Played"), TextUtils.text(Integer.toString(played))));
        if (played == 0) {
            TextUtils.sendRawMessage(sender, LocaleManager.text("command.test.no-particles", TextUtils.warning("No enabled particle effects matched that preview.")));
        }
    }

    private void handleData(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.usage", "Usage"), TextUtils.command(commandPath("data backup"))));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.usage", "Usage"), TextUtils.command(commandPath("data validate"))));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.usage", "Usage"), TextUtils.command(commandPath("data migrate-world <from> <to> [dry-run|apply]"))));
            return;
        }

        if (args[1].equalsIgnoreCase("backup")) {
            handleDataBackup(sender);
            return;
        }
        if (args[1].equalsIgnoreCase("validate")) {
            handleDataValidate(sender);
            return;
        }
        if (args[1].equalsIgnoreCase("migrate-world")) {
            handleDataMigrateWorld(sender, args);
            return;
        }

        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.unknown-action", "Unknown Data Action"), TextUtils.error(args[1])));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.try", "Try"), TextUtils.command(commandPath("data validate"))));
    }

    private void handleDataBackup(CommandSender sender) {
        try {
            File backupFile = TreeSerializer.backupTreesFile();
            TextUtils.sendRawMessage(sender, LocaleManager.text("command.data.backup-created", TextUtils.success("Tree data backup created.")));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.file", "File"), TextUtils.text(backupFile.getPath())));
        } catch (IOException exception) {
            TextUtils.sendRawMessage(sender, LocaleManager.text("command.data.backup-failed", TextUtils.error("Unable to back up trees.yml.")));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.error", "Error"), TextUtils.error(exception.getMessage())));
        }
    }

    private void handleDataValidate(CommandSender sender) {
        TreeSerializer.TreeDataValidationReport report = TreeSerializer.validateTreesFile();
        TextUtils.sendRawMessage(sender, formatSectionTitle(LocaleManager.text("ui.data.title", "Tree Data Validation")));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.file", "File"), TextUtils.text(TreeSerializer.getTreesFile().getPath())));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.stored-trees", "Stored Trees"), TextUtils.text(Integer.toString(report.storedTreeCount()))));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.loaded-trees", "Loaded Trees"), TextUtils.text(Integer.toString(report.loadedTreeCount()))));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.status", "Status"), report.hasWarnings()
                ? LocaleManager.text("ui.data.values.warnings", TextUtils.warning("warnings"))
                : LocaleManager.text("ui.data.values.clean", TextUtils.success("clean"))));
        sendValidationSet(sender, "invalid-tree-ids", report.invalidTreeIds());
        sendValidationSet(sender, "invalid-owners", report.invalidOwners());
        sendValidationSet(sender, "invalid-levels", report.invalidLevels());
        sendValidationSet(sender, "invalid-locations", report.invalidLocations());
        sendValidationSet(sender, "missing-worlds", report.missingWorlds());
        sendValidationSet(sender, "invalid-requirements", report.invalidRequirements());
        sendValidationSet(sender, "duplicate-locations", report.duplicateLocations());
    }

    private void handleDataMigrateWorld(CommandSender sender, String[] args) {
        if (args.length < 4) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.usage", "Usage"), TextUtils.command(commandPath("data migrate-world <from> <to> [dry-run|apply]"))));
            return;
        }

        String sourceWorld = args[2];
        String targetWorld = args[3];
        boolean apply = args.length >= 5 && args[4].equalsIgnoreCase("apply");
        if (args.length >= 5 && !args[4].equalsIgnoreCase("apply") && !args[4].equalsIgnoreCase("dry-run")) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.mode", "Mode"), TextUtils.error(args[4])));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.usage", "Usage"), TextUtils.command(commandPath("data migrate-world <from> <to> [dry-run|apply]"))));
            return;
        }

        try {
            TreeSerializer.WorldMigrationReport report = TreeSerializer.migrateWorldName(sourceWorld, targetWorld, apply);
            TextUtils.sendRawMessage(sender, formatSectionTitle(LocaleManager.text("ui.data.world-migration-title", "Tree World Migration")));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.source-world", "Source World"), TextUtils.text(report.sourceWorld())));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.target-world", "Target World"), TextUtils.text(report.targetWorld())));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.matched-trees", "Matched Trees"), TextUtils.text(Integer.toString(report.matchedTrees()))));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.mode", "Mode"), TextUtils.text(report.applied() ? "apply" : "dry-run")));
            if (report.backupFile() != null) {
                TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.backup", "Backup"), TextUtils.text(report.backupFile().getPath())));
            }
            if (!report.applied()) {
                TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.apply-command", "Apply Command"), TextUtils.command(commandPath("data migrate-world " + sourceWorld + " " + targetWorld + " apply"))));
            } else {
                TextUtils.sendRawMessage(sender, LocaleManager.text("command.data.migration-applied", TextUtils.warning("World migration saved. Restart the server before testing migrated trees.")));
            }
        } catch (IOException exception) {
            TextUtils.sendRawMessage(sender, LocaleManager.text("command.data.migration-failed", TextUtils.error("Unable to migrate tree world names.")));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.data.labels.error", "Error"), TextUtils.error(exception.getMessage())));
        }
    }

    private void sendValidationSet(CommandSender sender, String labelKey, Set<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        String label = LocaleManager.text("ui.data.labels." + labelKey, labelKey);
        TextUtils.sendRawMessage(sender, formatKeyValue(label, TextUtils.warning(joinLimited(values, 8))));
    }

    private String joinLimited(Set<String> values, int limit) {
        List<String> limited = new ArrayList<>();
        int index = 0;
        for (String value : values) {
            if (index >= limit) {
                break;
            }
            limited.add(value);
            index++;
        }
        if (values.size() > limit) {
            limited.add("+" + (values.size() - limit) + " more");
        }
        return String.join(", ", limited);
    }

    private int parsePositiveInt(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private Player resolveCommandTarget(CommandSender sender, String[] args, int targetArgIndex, String consoleUsage) {
        if (args.length > targetArgIndex) {
            Player target = Bukkit.getPlayer(args[targetArgIndex]);
            if (target == null) {
                TextUtils.sendMessage(sender, LocaleManager.COMMAND_PLAYER_OFFLINE);
            }
            return target;
        }
        if (sender instanceof Player player) {
            return player;
        }
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.test.labels.usage", "Usage"), TextUtils.command(consoleUsage)));
        return null;
    }

    private int playParticlePreview(TreeLevel level, String effectName, Location location) {
        int played = 0;
        if ((effectName.equals("all") || effectName.equals("ambient")) && level.getAmbientEffect() != null) {
            level.getAmbientEffect().playEffect(location);
            played++;
        }
        if ((effectName.equals("all") || effectName.equals("swag")) && level.getSwagEffect() != null) {
            level.getSwagEffect().playEffect(location);
            played++;
        }
        if ((effectName.equals("all") || effectName.equals("body")) && level.getBodyEffect() != null) {
            level.getBodyEffect().playEffect(location);
            played++;
        }
        return played;
    }

    private boolean isParticleEffectName(String value) {
        return value != null
                && (value.equalsIgnoreCase("all")
                || value.equalsIgnoreCase("ambient")
                || value.equalsIgnoreCase("swag")
                || value.equalsIgnoreCase("body"));
    }

    private TreeLevel treeLevelByName(String levelName) {
        try {
            return TreeLevel.fromString(levelName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private List<String> treeLevelNames() {
        return Arrays.asList("sapling", "small_tree", "tree", "magic_tree");
    }

    private void handleInspect(CommandSender sender, String[] args) {
        MagicTree tree = null;
        if (args.length >= 2 && !args[1].equalsIgnoreCase("nearest")) {
            tree = findTreeByUuid(args[1]);
            if (tree == null) {
                TextUtils.sendRawMessage(sender, formatKeyValue(
                        LocaleManager.text("ui.inspect.labels.requested", "Requested"),
                        TextUtils.error(args[1])
                ));
                TextUtils.sendRawMessage(sender, formatKeyValue(
                        LocaleManager.text("ui.inspect.labels.try", "Try"),
                        TextUtils.command(commandPath("inspect nearest"))
                ));
                return;
            }
        } else if (args.length >= 3 && args[1].equalsIgnoreCase("nearest")) {
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                TextUtils.sendMessage(sender, LocaleManager.COMMAND_PLAYER_OFFLINE);
                return;
            }
            tree = findNearestTree(target.getLocation(), 16);
        } else if (sender instanceof Player player) {
            tree = findTargetTree(player);
            if (tree == null) {
                tree = findNearestTree(player.getLocation(), 8);
            }
        } else {
            TextUtils.sendRawMessage(sender, formatKeyValue(
                    LocaleManager.text("ui.inspect.labels.usage", "Usage"),
                    TextUtils.command(commandPath("inspect <tree-uuid>"))
            ));
            return;
        }

        if (tree == null) {
            TextUtils.sendRawMessage(sender, LocaleManager.text("command.inspect.not-found", TextUtils.warning("No XMas Tree found nearby or in your line of sight.")));
            return;
        }
        for (String line : getInspectLines(tree)) {
            TextUtils.sendRawMessage(sender, line);
        }
    }

    private MagicTree findTreeByUuid(String rawUuid) {
        try {
            return XMas.getTree(UUID.fromString(rawUuid));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private MagicTree findTargetTree(Player player) {
        Block targetBlock = player.getTargetBlockExact(8);
        return MagicTree.getTreeByBlock(targetBlock);
    }

    private MagicTree findNearestTree(Location location, double range) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        double maxDistanceSquared = range * range;
        MagicTree nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        for (MagicTree tree : XMas.getAllTrees()) {
            Location treeLocation = tree.getLocation();
            if (treeLocation.getWorld() == null || !treeLocation.getWorld().equals(location.getWorld())) {
                continue;
            }
            double distanceSquared = treeLocation.distanceSquared(location);
            if (distanceSquared <= maxDistanceSquared && distanceSquared < nearestDistanceSquared) {
                nearest = tree;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return nearest;
    }

    private List<String> getInspectLines(MagicTree tree) {
        List<String> lines = new ArrayList<>();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(tree.getOwner());
        Location location = tree.getLocation();
        String ownerName = owner.getName() != null ? owner.getName() : LocaleManager.text("ui.inspect.values.unknown-owner", "unknown");

        lines.add(LocaleManager.text("ui.inspect.title", TextUtils.title(TextUtils.displayName() + " Tree Inspect")));
        lines.add(formatKeyValue(LocaleManager.text("ui.inspect.labels.owner", "Owner"), TextUtils.text(ownerName) + TextUtils.muted(" (" + tree.getOwner() + ")")));
        lines.add(formatKeyValue(LocaleManager.text("ui.inspect.labels.tree-id", "Tree ID"), TextUtils.text(tree.getTreeUID().toString())));
        lines.add(formatKeyValue(LocaleManager.text("ui.inspect.labels.level", "Level"), TextUtils.text(tree.getLevel().getLevelName())));
        lines.add(formatKeyValue(LocaleManager.text("ui.inspect.labels.location", "Location"), TextUtils.text(formatLocation(location))));
        lines.add(formatKeyValue(LocaleManager.text("ui.inspect.labels.can-level-up", "Can Level Up"), TextUtils.booleanValue(tree.canLevelUp())));
        lines.add(formatKeyValue(LocaleManager.text("ui.inspect.labels.present-timer", "Present Timer"), TextUtils.text(Long.toString(tree.getPresentCounter()))));
        lines.add(formatKeyValue(LocaleManager.text("ui.inspect.labels.scheduled-presents", "Scheduled Presents"), TextUtils.text(Integer.toString(tree.getScheduledPresents()))));
        lines.add(formatKeyValue(LocaleManager.text("ui.inspect.labels.remaining", "Remaining Requirements"), TextUtils.text(formatRequirements(tree.getLevelupRequirements()))));
        lines.add(formatKeyValue(LocaleManager.text("ui.inspect.labels.refund-preview", "Refund Preview"), TextUtils.text(formatItems(tree.getRefundPreviewItems()))));
        return lines;
    }

    private List<String> getReloadSummaryLines(Main.ReloadSummary summary) {
        List<String> lines = new ArrayList<>();
        lines.add(formatSectionTitle(LocaleManager.text("ui.reload.title", "Reload Summary")));
        lines.add(formatKeyValue(LocaleManager.text("ui.reload.labels.locale", "Locale"), TextUtils.text(summary.locale())));
        lines.add(formatKeyValue(LocaleManager.text("ui.reload.labels.gifts", "Gifts"), TextUtils.text(Integer.toString(summary.giftCount()))));
        lines.add(formatKeyValue(LocaleManager.text("ui.reload.labels.present-heads", "Present Heads"), TextUtils.text(Integer.toString(summary.presentHeadCount()))));
        lines.add(formatKeyValue(LocaleManager.text("ui.reload.labels.trees", "Trees"), TextUtils.text(Integer.toString(summary.treeCount()))));
        lines.add(formatKeyValue(LocaleManager.text("ui.reload.labels.owners", "Owners"), TextUtils.text(Integer.toString(summary.treeOwnerCount()))));
        lines.add(formatKeyValue(LocaleManager.text("ui.reload.labels.particles", "Particles"), TextUtils.booleanValue(summary.particlesEnabled())));
        lines.add(formatKeyValue(LocaleManager.text("ui.reload.labels.resource-back", "Resource Back"), TextUtils.booleanValue(summary.resourceBack())));
        lines.add(formatKeyValue(LocaleManager.text("ui.reload.labels.legacy-alias", "Legacy Alias"), TextUtils.booleanValue(summary.legacyAliasEnabled())));
        lines.add(formatKeyValue(LocaleManager.text("ui.reload.labels.sounds", "Sounds"), TextUtils.text("first=" + summary.growFirstSoundVolume() + ", repeat=" + summary.growRepeatSoundVolume())));
        return lines;
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

        lines.add(
                replaceToken(
                        LocaleManager.text("ui.status.title", TextUtils.title(TextUtils.displayName()) + " " + TextUtils.text("{version}") + " " + TextUtils.title("Plugin Status")),
                        "version",
                        plugin.getPluginMeta().getVersion()
                )
        );
        lines.add("");
        lines.add(formatKeyValue(
                LocaleManager.text("ui.status.labels.event-status", "Event Status"),
                Main.inProgress
                        ? LocaleManager.text("ui.status.values.in-progress", TextUtils.success("In Progress"))
                        : LocaleManager.text("ui.status.values.holidays-end", TextUtils.error("Holidays End"))
        ));
        if (Main.inProgress) {
            lines.add(formatKeyValue(LocaleManager.text("ui.status.labels.current-time", "Current Time"), TextUtils.text(sdf.format(System.currentTimeMillis()))));
            lines.add(formatKeyValue(LocaleManager.text("ui.status.labels.holidays-end", "Holidays End"), TextUtils.text(sdf.format(Main.endTime))));
        }
        lines.add(formatKeyValue(LocaleManager.text("ui.status.labels.auto-end", "Auto-End"), TextUtils.booleanValue(Main.autoEnd)));
        lines.add(formatKeyValue(LocaleManager.text("ui.status.labels.resource-back", "Resource Back"), TextUtils.booleanValue(Main.resourceBack)));
        lines.add(formatKeyValue(LocaleManager.text("ui.status.labels.particles", "Particles"), TextUtils.booleanValue(Main.particlesEnabled)));
        lines.add("");
        lines.add(formatKeyValue(LocaleManager.text("ui.status.labels.loaded-trees", "Loaded Trees"), TextUtils.text(Integer.toString(treeCount))));
        lines.add(formatKeyValue(LocaleManager.text("ui.status.labels.tree-owners", "Tree Owners"), TextUtils.text(Integer.toString(owners.size()))));
        lines.add(formatKeyValue(LocaleManager.text("ui.status.labels.help", "Help"), TextUtils.command(commandPath("help"))));
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
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.usage", "Usage"), TextUtils.command(commandPath("debug toggle <key> true|false"))));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.keys", "Keys"), TextUtils.text(String.join(", ", DEBUG_TOGGLE_KEYS))));
            return;
        }

        String key = args[2].toLowerCase(Locale.ENGLISH);
        if (!DEBUG_TOGGLE_KEYS.contains(key)) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.unknown-toggle-key", "Unknown Toggle Key"), TextUtils.error(args[2])));
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.keys", "Keys"), TextUtils.text(String.join(", ", DEBUG_TOGGLE_KEYS))));
            return;
        }
        if (!args[3].equalsIgnoreCase("true") && !args[3].equalsIgnoreCase("false")) {
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.value", "Value"), LocaleManager.text("ui.debug.invalid-boolean", TextUtils.error("must be true or false"))));
            return;
        }

        boolean value = Boolean.parseBoolean(args[3]);
        plugin.getConfig().set(key, value);
        plugin.saveConfig();
        plugin.reloadPluginConfig();
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.updated", "Updated"), TextUtils.command(key) + TextUtils.muted(" -> ") + TextUtils.booleanValue(value)));
    }

    private LinkedHashMap<String, List<String>> buildDebugSections() {
        LinkedHashMap<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("status", getStatusLines());

        List<String> commandsPage = new ArrayList<>();
        commandsPage.add("");
        commandsPage.add(formatSectionTitle(debugSectionDisplayName("commands")));
        commandsPage.add(formatListEntry(commandPath(""), LocaleManager.text("ui.command-descriptions.status", "status")));
        commandsPage.add(formatListEntry(commandPath("help"), LocaleManager.text("ui.command-descriptions.help", "command list")));
        commandsPage.add(formatListEntry(commandPath("give <player>"), LocaleManager.text("ui.command-descriptions.give", "give a Christmas Crystal")));
        commandsPage.add(formatListEntry(commandPath("gifts"), LocaleManager.text("ui.command-descriptions.gifts", "spawn presents under all trees")));
        commandsPage.add(formatListEntry(commandPath("gifts list [page]"), LocaleManager.text("ui.command-descriptions.gifts-list", "list configured gifts")));
        commandsPage.add(formatListEntry(commandPath("gifts roll"), LocaleManager.text("ui.command-descriptions.gifts-roll", "preview a random gift roll")));
        commandsPage.add(formatListEntry(commandPath("gifts remove <index>"), LocaleManager.text("ui.command-descriptions.gifts-remove", "remove a configured gift")));
        commandsPage.add(formatListEntry(commandPath("addhand"), LocaleManager.text("ui.command-descriptions.addhand", "add held item to gifts")));
        commandsPage.add(formatListEntry(commandPath("reload"), LocaleManager.text("ui.command-descriptions.reload", "reload config and locale")));
        commandsPage.add(formatListEntry(commandPath("inspect"), LocaleManager.text("ui.command-descriptions.inspect", "inspect the tree you are looking at")));
        commandsPage.add(formatListEntry(commandPath("test sound first|repeat [player]"), LocaleManager.text("ui.command-descriptions.test-sound", "preview grow sounds")));
        commandsPage.add(formatListEntry(commandPath("test particle <level> [effect] [player]"), LocaleManager.text("ui.command-descriptions.test-particle", "preview configured particles")));
        commandsPage.add(formatListEntry(commandPath("data backup"), LocaleManager.text("ui.command-descriptions.data-backup", "back up trees.yml")));
        commandsPage.add(formatListEntry(commandPath("data validate"), LocaleManager.text("ui.command-descriptions.data-validate", "validate trees.yml")));
        commandsPage.add(formatListEntry(commandPath("data migrate-world <from> <to> [dry-run|apply]"), LocaleManager.text("ui.command-descriptions.data-migrate-world", "migrate saved tree world names")));
        commandsPage.add(formatListEntry(commandPath("end"), LocaleManager.text("ui.command-descriptions.end", "end the event")));
        commandsPage.add(formatListEntry(commandPath("debug"), LocaleManager.text("ui.command-descriptions.debug", "open the status debug section")));
        commandsPage.add(formatListEntry(commandPath("debug [section|page]"), LocaleManager.text("ui.command-descriptions.debug-section", "extended debug output by category")));
        commandsPage.add(formatListEntry(commandPath("debug toggle <key> true|false"), LocaleManager.text("ui.command-descriptions.debug-toggle", "toggle global booleans")));
        if (isLegacyAliasEnabled()) {
            commandsPage.add(formatKeyValue(LocaleManager.text("ui.status.labels.legacy-alias", "Legacy Alias"), TextUtils.command("/" + LEGACY_COMMAND)));
        }
        sections.put("commands", commandsPage);

        List<String> permissionsPage = new ArrayList<>();
        permissionsPage.add("");
        permissionsPage.add(formatSectionTitle(debugSectionDisplayName("permissions")));
        for (Map.Entry<String, String> permission : createPermissionDescriptions().entrySet()) {
            permissionsPage.add(formatListEntry(permission.getKey(), permission.getValue()));
        }
        sections.put("permissions", permissionsPage);

        List<String> placeholdersPage = new ArrayList<>();
        placeholdersPage.add("");
        placeholdersPage.add(formatSectionTitle(debugSectionDisplayName("placeholders")));
        placeholdersPage.add(formatKeyValue(
                LocaleManager.text("ui.debug.labels.notes", "Notes"),
                LocaleManager.text("ui.debug.notes.placeholders", TextUtils.text("Requires PlaceholderAPI. Use '_' after prefix, then dotted keys."))
        ));
        Map<String, String> placeholderDescriptions = XMasPlaceholders.descriptions();
        for (String placeholder : XMasPlaceholders.EXAMPLES) {
            placeholdersPage.add(formatListEntry(placeholder, placeholderDescriptions.getOrDefault(placeholder, LocaleManager.text("ui.debug.placeholder-default-description", "registered placeholder"))));
        }
        sections.put("placeholders", placeholdersPage);

        List<String> togglesPage = new ArrayList<>();
        togglesPage.add("");
        togglesPage.add(formatSectionTitle(debugSectionDisplayName("config")));
        for (String key : DEBUG_TOGGLE_KEYS) {
            togglesPage.add(formatKeyValue(key, TextUtils.booleanValue(plugin.getConfig().getBoolean(key))));
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

        TextUtils.sendRawMessage(sender,
                LocaleManager.text("ui.debug.section-title", TextUtils.title(TextUtils.displayName() + " Debug"))
                        + " "
                        + TextUtils.text(debugSectionDisplayName(sectionKey))
                        + TextUtils.muted(" (" + page + "/" + pageCount + ")"));
        for (String line : sections.get(sectionKey)) {
            TextUtils.sendRawMessage(sender, line);
        }
        if (page < pageCount) {
            String nextSection = sectionKeys.get(sectionIndex + 1);
            TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.next", "Next"), TextUtils.command(commandPath("debug " + nextSection))));
        }
    }

    private void sendInvalidDebugSelection(CommandSender sender, String requested, int pageCount) {
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.sections", "Debug Sections"), TextUtils.text(String.join(", ", DEBUG_SECTIONS.keySet()))));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.pages", "Debug Pages"), TextUtils.text("1-" + pageCount)));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.requested", "Requested"), TextUtils.error(requested)));
        TextUtils.sendRawMessage(sender, formatKeyValue(LocaleManager.text("ui.debug.labels.try", "Try"), TextUtils.command(commandPath("debug status"))));
    }

    private List<String> getHelpLines() {
        List<String> lines = new ArrayList<>();
        for (String line : LocaleManager.COMMAND_HELP) {
            lines.add(line.replaceAll("/" + LEGACY_COMMAND + "(?![A-Za-z])", "/" + PRIMARY_COMMAND));
        }
        addMissingBundledHelpLines(lines);
        if (isLegacyAliasEnabled()) {
            lines.add(LocaleManager.text(
                    "command.legacy-alias-enabled",
                    TextUtils.muted("Legacy alias: ") + TextUtils.command("/" + LEGACY_COMMAND) + TextUtils.muted(" still works.")
            ));
        }
        return lines;
    }

    private String commandPath(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return "/" + PRIMARY_COMMAND;
        }
        return "/" + PRIMARY_COMMAND + " " + suffix;
    }

    private void addMissingBundledHelpLines(List<String> lines) {
        List<String> bundledLines = LocaleManager.bundledTextList("command.help");
        for (String bundledLine : bundledLines) {
            String normalizedLine = bundledLine.replaceAll("/" + LEGACY_COMMAND + "(?![A-Za-z])", "/" + PRIMARY_COMMAND);
            String commandNeedle = findCommandNeedle(normalizedLine);
            if (commandNeedle != null && lines.stream().noneMatch(line -> line.contains(commandNeedle))) {
                lines.add(normalizedLine);
            }
        }
    }

    private String findCommandNeedle(String line) {
        List<String> commandNeedles = List.of(
                commandPath("debug toggle"),
                commandPath("debug status"),
                commandPath("debug"),
                commandPath("test particle"),
                commandPath("test sound"),
                commandPath("data validate"),
                commandPath("data migrate-world"),
                commandPath("data backup"),
                commandPath("gifts remove"),
                commandPath("gifts roll"),
                commandPath("gifts list"),
                commandPath("inspect"),
                commandPath("reload"),
                commandPath("addhand"),
                commandPath("gifts"),
                commandPath("give"),
                commandPath("help"),
                commandPath("end"),
                commandPath("")
        );
        for (String commandNeedle : commandNeedles) {
            if (line.contains(commandNeedle)) {
                return commandNeedle;
            }
        }
        return null;
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
            plugin.getLogger().warning(LocaleManager.text("console.alias.command-map-unavailable", "Unable to access the Bukkit command map. Skipping legacy /xmas alias registration."));
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
            plugin.getLogger().warning(LocaleManager.text("console.alias.owned-by-plugin", "Legacy alias '/{alias}' is already owned by plugin '{plugin}'. Skipping alias registration.",
                    "{alias}", LEGACY_COMMAND,
                    "{plugin}", existingPluginCommand.getPlugin().getName()));
            return;
        }
        if (existing != null) {
            plugin.getLogger().warning(LocaleManager.text("console.alias.registered-by-other", "Legacy alias '/{alias}' is already registered by another command source. Skipping alias registration.",
                    "{alias}", LEGACY_COMMAND));
            return;
        }

        PluginCommand aliasCommand = createPluginCommand(plugin, LEGACY_COMMAND);
        if (aliasCommand == null) {
            plugin.getLogger().warning(LocaleManager.text("console.alias.create-failed", "Unable to create the legacy /xmas alias command."));
            return;
        }
        aliasCommand.setDescription("Legacy alias for /" + PRIMARY_COMMAND);
        aliasCommand.setUsage("/" + LEGACY_COMMAND + " [help|give|gifts|addhand|reload|inspect|test|data|debug [section|page]|end]");
        aliasCommand.setPermission(null);
        aliasCommand.setExecutor(executor);
        aliasCommand.setTabCompleter(executor);
        commandMap.register(plugin.getPluginMeta().getName().toLowerCase(Locale.ENGLISH), aliasCommand);
        legacyAliasCommand = aliasCommand;
        plugin.getLogger().info(LocaleManager.text("console.alias.registered", "Registered legacy alias '/{alias}' for '/{primary}'.",
                "{alias}", LEGACY_COMMAND,
                "{primary}", PRIMARY_COMMAND));
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
            plugin.getLogger().warning(LocaleManager.text("console.alias.construct-failed", "Unable to construct dynamic command '/{command}': {error}",
                    "{command}", name,
                    "{error}", e.getMessage()));
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
            case "inspect" -> hasPermission(sender, PERMISSION_INSPECT);
            case "test" -> hasPermission(sender, PERMISSION_TEST);
            case "data" -> hasPermission(sender, PERMISSION_DATA);
            case "debug" -> hasPermission(sender, PERMISSION_DEBUG) || hasPermission(sender, PERMISSION_DEBUG_TOGGLE);
            default -> false;
        };
    }

    private static boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(PERMISSION_ADMIN) || sender.hasPermission(permission);
    }

    private void sendNoPermission(CommandSender sender) {
        TextUtils.sendRawMessage(sender, LocaleManager.text("command.no-permission", TextUtils.error("You do not have permission to use this command.")));
    }

    private String formatSectionTitle(String title) {
        return TextUtils.title(title);
    }

    private String formatListEntry(String key, String value) {
        return TextUtils.command(key) + TextUtils.muted(" : ") + TextUtils.text(value);
    }

    private String formatStyledListEntry(String key, String value) {
        return TextUtils.command(key) + TextUtils.muted(" : ") + value;
    }

    private String formatKeyValue(String key, String value) {
        return TextUtils.label(key) + TextUtils.muted(": ") + value;
    }

    private static Map<String, String> createPermissionDescriptions() {
        Map<String, String> permissions = new LinkedHashMap<>();
        permissions.put(PERMISSION_ADMIN, LocaleManager.text("ui.permission-descriptions.admin", "allows all {plugin_name} commands and overrides"));
        permissions.put(PERMISSION_STATUS, LocaleManager.text("ui.permission-descriptions.status", "shows /xmastree status output"));
        permissions.put(PERMISSION_HELP, LocaleManager.text("ui.permission-descriptions.help", "shows /xmastree help output"));
        permissions.put(PERMISSION_GIVE, LocaleManager.text("ui.permission-descriptions.give", "allows /xmastree give"));
        permissions.put(PERMISSION_GIFTS, LocaleManager.text("ui.permission-descriptions.gifts", "allows /xmastree gifts"));
        permissions.put(PERMISSION_ADDHAND, LocaleManager.text("ui.permission-descriptions.addhand", "allows /xmastree addhand"));
        permissions.put(PERMISSION_RELOAD, LocaleManager.text("ui.permission-descriptions.reload", "allows /xmastree reload"));
        permissions.put(PERMISSION_INSPECT, LocaleManager.text("ui.permission-descriptions.inspect", "allows /xmastree inspect"));
        permissions.put(PERMISSION_TEST, LocaleManager.text("ui.permission-descriptions.test", "allows /xmastree test"));
        permissions.put(PERMISSION_DATA, LocaleManager.text("ui.permission-descriptions.data", "allows /xmastree data"));
        permissions.put(PERMISSION_DEBUG, LocaleManager.text("ui.permission-descriptions.debug", "allows /xmastree debug"));
        permissions.put(PERMISSION_DEBUG_TOGGLE, LocaleManager.text("ui.permission-descriptions.debug-toggle", "allows /xmastree debug toggle"));
        permissions.put(PERMISSION_END, LocaleManager.text("ui.permission-descriptions.end", "allows /xmastree end"));
        permissions.put(PERMISSION_TREE_OVERRIDE, LocaleManager.text("ui.permission-descriptions.tree-override", "allows managing other players' trees"));
        return permissions;
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return LocaleManager.text("ui.inspect.values.unknown-location", "unknown");
        }
        return location.getWorld().getName()
                + " "
                + location.getBlockX()
                + ", "
                + location.getBlockY()
                + ", "
                + location.getBlockZ();
    }

    private String formatRequirements(Map<Material, Integer> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return LocaleManager.text("ui.inspect.values.none", "none");
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : requirements.entrySet()) {
            if (entry.getValue() > 0) {
                parts.add(formatMaterial(entry.getKey()) + " x" + entry.getValue());
            }
        }
        return parts.isEmpty() ? LocaleManager.text("ui.inspect.values.none", "none") : String.join(", ", parts);
    }

    private String formatItems(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return LocaleManager.text("ui.inspect.values.none", "none");
        }
        Map<Material, Integer> totals = new LinkedHashMap<>();
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                totals.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        return formatRequirements(totals);
    }

    private String formatGiftItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return TextUtils.muted(LocaleManager.text("ui.inspect.values.none", "none"));
        }
        String name = formatMaterial(item.getType());
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            name = PLAIN_TEXT.serialize(meta.displayName());
        }
        String amount = " x" + item.getAmount();
        if (name.equals(formatMaterial(item.getType()))) {
            return TextUtils.text(name + amount);
        }
        return TextUtils.text(name + amount) + TextUtils.muted(" (" + item.getType().name() + ")");
    }

    private List<String> getWorldNames() {
        List<String> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            worlds.add(world.getName());
        }
        return worlds;
    }

    private String formatMaterial(Material material) {
        return Arrays.stream(material.name().toLowerCase(Locale.ENGLISH).split("_"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(material.name());
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

    private String debugSectionDisplayName(String sectionKey) {
        return LocaleManager.text("ui.debug.sections." + sectionKey, DEBUG_SECTIONS.getOrDefault(sectionKey, sectionKey));
    }

    private String replaceToken(String template, String key, String value) {
        if (template == null) {
            return null;
        }
        return template.replace("{" + key + "}", TextUtils.escape(value));
    }

}
