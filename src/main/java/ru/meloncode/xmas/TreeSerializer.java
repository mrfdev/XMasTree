package ru.meloncode.xmas;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.meloncode.xmas.utils.ConfigUtils;
import ru.meloncode.xmas.utils.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

class TreeSerializer {
    private static final File treesFile = new File(Main.getInstance().getDataFolder() + "/trees.yml");
    private static final FileConfiguration trees = ConfigUtils.loadConfig(treesFile);
    private static final Set<String> loggedWorldAliasMappings = ConcurrentHashMap.newKeySet();

    public static void loadTrees(JavaPlugin plugin, World world) {
        try {
            UUID owner;
            UUID treeUID;
            TreeLevel level;
            int x, y, z;
            Location loc;
            long presentCounter;
            int scheduledPresents;
            if (trees.getConfigurationSection("trees") != null && trees.getConfigurationSection("trees").getKeys(false).size() > 0) {

                for (String cKey : trees.getConfigurationSection("trees").getKeys(false)) {
                    String savedWorldName = trees.getString("trees." + cKey + ".loc.world");
                    if (matchesSavedWorld(world, savedWorldName)) {
                        try {
                            treeUID = UUID.fromString(cKey);
                            owner = UUID.fromString(trees.getString("trees." + cKey + ".owner"));
                            level = TreeLevel.fromString(trees.getString("trees." + cKey + ".level"));
                            x = trees.getInt("trees." + cKey + ".loc.x");
                            y = trees.getInt("trees." + cKey + ".loc.y");
                            z = trees.getInt("trees." + cKey + ".loc.z");
                            loc = new Location(world, x, y, z);
                            Map<Material, Integer> requirements;
                            if (trees.getConfigurationSection("trees." + cKey + ".levelup") != null) {
                                requirements = convertRequirementsMap(trees.getConfigurationSection("trees." + cKey + ".levelup").getValues(false));
                            } else {
                                requirements = new HashMap<>();
                            }
                            presentCounter = trees.getLong("trees." + cKey + ".present_counter", 0);
                            scheduledPresents = trees.getInt("trees." + cKey + ".scheduled_presents", 0);

                            XMas.addMagicTree(new MagicTree(owner, treeUID, level, loc, requirements, presentCounter, scheduledPresents));
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.SEVERE, String.format("Error while loading tree `%s`", cKey), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            TextUtils.sendConsoleMessage("<dark_red>ERROR WHILE LOADING TREES");
            plugin.getLogger().log(Level.SEVERE, "Unable to load X-Mas trees", e);
        }

    }

    public static void saveTree(MagicTree tree) {
        String cKey = tree.getTreeUID().toString();
        String owner = tree.getOwner().toString();
        trees.set("trees." + cKey + ".owner", owner);
        trees.set("trees." + cKey + ".level", tree.getLevel().getLevelName());
        trees.set("trees." + cKey + ".loc.world", tree.getLocation().getWorld().getName());
        trees.set("trees." + cKey + ".loc.x", tree.getLocation().getX());
        trees.set("trees." + cKey + ".loc.y", tree.getLocation().getY());
        trees.set("trees." + cKey + ".loc.z", tree.getLocation().getZ());
        if (tree.getLevelupRequirements() != null && tree.getLevelupRequirements().size() > 0)
            trees.createSection("trees." + cKey + ".levelup", tree.getLevelupRequirements());
        trees.set("trees." + cKey + ".present_counter", tree.getPresentCounter());
        trees.set("trees." + cKey + ".scheduled_presents", tree.getScheduledPresents());
        try {
            trees.save(treesFile);
        } catch (IOException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "Unable to save X-Mas tree data", e);
        }
    }

    public static void removeTree(MagicTree tree) {
        trees.set("trees." + tree.getTreeUID().toString(), null);
        try {
            trees.save(treesFile);
        } catch (IOException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "Unable to remove X-Mas tree data", e);
        }
    }

    public static Map<Material, Integer> convertRequirementsMap(Map<String, Object> map) {
        Map<Material, Integer> levelupRequirements = new HashMap<>();
        Material cMaterial;
        int value;
        if (map != null)
            for (String sMaterial : map.keySet()) {
                try {
                    cMaterial = Material.matchMaterial(sMaterial);
                    if (cMaterial == null || cMaterial.isLegacy()) {
                        TextUtils.sendConsoleMessage("<red>Can't find modern material '" + sMaterial + "' for tree level.");
                        continue;
                    }
                    Object rawValue = map.get(sMaterial);
                    if (!(rawValue instanceof Number)) {
                        TextUtils.sendConsoleMessage("<red>Tree level material '" + sMaterial + "' must use a numeric amount.");
                        continue;
                    }
                    value = ((Number) rawValue).intValue();
                    levelupRequirements.put(cMaterial, value);
                } catch (IllegalArgumentException e) {
                    TextUtils.sendConsoleMessage("<red>Can't load material '" + sMaterial + "' for tree level.");
                }
            }
        return levelupRequirements;
    }

    private static boolean matchesSavedWorld(World world, String savedWorldName) {
        if (world == null || savedWorldName == null || savedWorldName.isBlank()) {
            return false;
        }
        if (world.getName().equalsIgnoreCase(savedWorldName)) {
            return true;
        }

        String configuredWorldName = getWorldAlias(savedWorldName);
        if (configuredWorldName != null && world.getName().equalsIgnoreCase(configuredWorldName)) {
            logWorldAliasMapping(savedWorldName, world.getName());
            return true;
        }
        return false;
    }

    private static String getWorldAlias(String savedWorldName) {
        if (!(Main.getInstance() instanceof Main plugin)) {
            return null;
        }
        return plugin.getConfig().getString("migration.world-aliases." + savedWorldName);
    }

    private static void logWorldAliasMapping(String savedWorldName, String worldName) {
        String mapping = savedWorldName + "->" + worldName;
        if (loggedWorldAliasMappings.add(mapping)) {
            Main.getInstance().getLogger().info("Loading legacy X-Mas trees from saved world '" + savedWorldName + "' into '" + worldName + "' via migration.world-aliases.");
        }
    }
}
