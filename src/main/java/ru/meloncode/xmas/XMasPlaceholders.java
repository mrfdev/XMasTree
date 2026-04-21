package ru.meloncode.xmas;

import org.bukkit.OfflinePlayer;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class XMasPlaceholders {
    public static final String IDENTIFIER = "onembxmastree";
    public static final List<String> EXAMPLES = Arrays.asList(
            "%onembxmastree_event.active%",
            "%onembxmastree_event.active_text%",
            "%onembxmastree_event.status%",
            "%onembxmastree_event.starts_at%",
            "%onembxmastree_event.ends_at%",
            "%onembxmastree_event.ends_in%",
            "%onembxmastree_event.ends_timestamp%",
            "%onembxmastree_event.auto_end%",
            "%onembxmastree_resource.back%",
            "%onembxmastree_resource.back_text%",
            "%onembxmastree_particles.enabled%",
            "%onembxmastree_luck.enabled%",
            "%onembxmastree_luck.chance%",
            "%onembxmastree_trees.total%",
            "%onembxmastree_trees.owners%",
            "%onembxmastree_player.trees%",
            "%onembxmastree_version%"
    );
    public static final Map<String, String> DESCRIPTIONS = createDescriptions();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");

    private XMasPlaceholders() {
    }

    public static String resolve(Main plugin, OfflinePlayer player, String params) {
        if (params == null || params.trim().isEmpty()) {
            return null;
        }
        String key = normalize(params);
        return switch (key) {
            case "event_active" -> Boolean.toString(Main.inProgress);
            case "event_active_text" -> Main.inProgress ? "Active" : "Inactive";
            case "event_status" -> Main.inProgress ? "In Progress" : "Holidays End";
            case "event_starts_at" -> "manual";
            case "event_ends_at" -> formatEndDate();
            case "event_ends_in" -> formatDurationUntilEnd();
            case "event_ends_timestamp" -> Long.toString(Main.endTime);
            case "event_auto_end" -> Boolean.toString(Main.autoEnd);
            case "resource_back" -> Boolean.toString(Main.resourceBack);
            case "resource_back_text" -> Main.resourceBack ? "Yes" : "No";
            case "particles_enabled" -> Boolean.toString(Main.particlesEnabled);
            case "luck_enabled" -> Boolean.toString(Main.LUCK_CHANCE_ENABLED);
            case "luck_chance" -> Integer.toString(Math.round(Main.LUCK_CHANCE * 100));
            case "trees_total" -> Integer.toString(XMas.getAllTrees().size());
            case "trees_owners" -> Integer.toString(countOwners(XMas.getAllTrees()));
            case "player_trees" -> Integer.toString(countPlayerTrees(player));
            case "version" -> plugin.getDescription().getVersion();
            default -> null;
        };
    }

    private static String normalize(String params) {
        return params.trim()
                .toLowerCase(Locale.ENGLISH)
                .replace('.', '_')
                .replace('-', '_');
    }

    private static String formatEndDate() {
        if (Main.endTime <= 0) {
            return "unknown";
        }
        return DATE_FORMAT.format(new Date(Main.endTime));
    }

    private static String formatDurationUntilEnd() {
        if (!Main.autoEnd) {
            return "disabled";
        }
        if (Main.endTime <= 0) {
            return "unknown";
        }
        long remainingMillis = Main.endTime - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return "ended";
        }

        long totalSeconds = remainingMillis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private static int countOwners(Collection<MagicTree> trees) {
        Set<UUID> owners = new LinkedHashSet<>();
        for (MagicTree tree : trees) {
            owners.add(tree.getOwner());
        }
        return owners.size();
    }

    private static int countPlayerTrees(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            return 0;
        }
        int count = 0;
        for (MagicTree tree : XMas.getAllTrees()) {
            if (player.getUniqueId().equals(tree.getOwner())) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, String> createDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("%onembxmastree_event.active%", "whether the event is currently active");
        descriptions.put("%onembxmastree_event.active_text%", "human-readable active state");
        descriptions.put("%onembxmastree_event.status%", "current event status text");
        descriptions.put("%onembxmastree_event.starts_at%", "event start mode");
        descriptions.put("%onembxmastree_event.ends_at%", "configured event end date");
        descriptions.put("%onembxmastree_event.ends_in%", "time remaining until the event ends");
        descriptions.put("%onembxmastree_event.ends_timestamp%", "event end timestamp in milliseconds");
        descriptions.put("%onembxmastree_event.auto_end%", "whether automatic ending is enabled");
        descriptions.put("%onembxmastree_resource.back%", "whether resource refunds are enabled");
        descriptions.put("%onembxmastree_resource.back_text%", "human-readable refund state");
        descriptions.put("%onembxmastree_particles.enabled%", "whether XMas Tree particles are enabled");
        descriptions.put("%onembxmastree_luck.enabled%", "whether gift luck chance is enabled");
        descriptions.put("%onembxmastree_luck.chance%", "gift luck chance as a percent");
        descriptions.put("%onembxmastree_trees.total%", "total loaded tree count");
        descriptions.put("%onembxmastree_trees.owners%", "number of unique tree owners");
        descriptions.put("%onembxmastree_player.trees%", "loaded trees owned by the placeholder player");
        descriptions.put("%onembxmastree_version%", "loaded plugin version");
        return descriptions;
    }
}
