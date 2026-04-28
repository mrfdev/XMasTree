package ru.meloncode.xmas;

import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import ru.meloncode.xmas.utils.ConfigUtils;
import ru.meloncode.xmas.utils.TextUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.*;

public class Main extends JavaPlugin implements Listener {
    private static final String CONFIG_RESOURCE_PATH = "config.yml";

    // Yeah. That's as it should be.
    static final Random RANDOM = new Random(Calendar.getInstance().get(Calendar.YEAR));
    static List<ItemStack> gifts;
    static float LUCK_CHANCE;
    static boolean LUCK_CHANCE_ENABLED;
    static boolean resourceBack;
    static int MAX_TREE_COUNT;
    static boolean autoEnd;
    static boolean particlesEnabled;
    static float growFirstSoundVolume;
    static float growRepeatSoundVolume;
    static long endTime;
    static boolean inProgress;
    private static int UPDATE_SPEED;
    private static int PARTICLES_DELAY;
    private static NamespacedKey crystalKey;
    private static NamespacedKey noDamageFireworkKey;
    private static List<String> heads;
    private static Plugin plugin;
    private static final int MAX_SERIALIZED_GIFT_LENGTH = 65536;
    private FileConfiguration config;
    private String locale;
    private XMasPlaceholderExpansion placeholderExpansion;

    public static Plugin getInstance() {
        return plugin;
    }

    public static List<String> getHeads() {
        return heads;
    }

    public static NamespacedKey getCrystalKey() {
        return crystalKey;
    }

    public static NamespacedKey getNoDamageFireworkKey() {
        return noDamageFireworkKey;
    }

    private File getPluginConfigFile() {
        return new File(getDataFolder(), CONFIG_RESOURCE_PATH);
    }

    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            config = ConfigUtils.loadManagedConfig(this, CONFIG_RESOURCE_PATH, getPluginConfigFile());
        }
        return config;
    }

    @Override
    public void reloadConfig() {
        config = ConfigUtils.loadManagedConfig(this, CONFIG_RESOURCE_PATH, getPluginConfigFile());
    }

    @Override
    public void saveConfig() {
        if (config == null) {
            return;
        }
        ConfigUtils.synchronizeWithResource(this, CONFIG_RESOURCE_PATH, config);
        ConfigUtils.saveConfig(getPluginConfigFile(), config);
    }

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        this.saveDefaults();
        crystalKey = new NamespacedKey(this, "xmas_crystal");
        noDamageFireworkKey = new NamespacedKey(this, "no_damage_firework");
        config = getConfig();
        locale = config.getString("core.locale");

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy kk-mm-ss");
        inProgress = config.getBoolean("core.plugin-enabled", true);
        UPDATE_SPEED = config.getInt("core.update-speed");
        if (UPDATE_SPEED <= 0) {
            TextUtils.sendConsoleMessage("Update speed must be > 0");
            TextUtils.sendConsoleMessage("Setting value to default");
            config.set("core.update-speed", 7);
            UPDATE_SPEED = 7;
        }
        PARTICLES_DELAY = config.getInt("core.particles-delay");
        if (PARTICLES_DELAY <= 0) {
            config.set("core.particles-delay", 35);
            PARTICLES_DELAY = 35;
        }
        particlesEnabled = config.getBoolean("core.particles-enabled", true);
        loadSoundConfig();
        
        autoEnd = config.getBoolean("core.holiday-ends.enabled");
        resourceBack = config.getBoolean("core.holiday-ends.resource-back");
        MAX_TREE_COUNT = config.getInt("core.tree-limit");
        Date date;
        try {
            date = sdf.parse(config.getString("core.holiday-ends.date"));
            endTime = date.getTime();
        } catch (ParseException e1) {
            TextUtils.sendConsoleMessage("<red>Unable to load date");
        }
        defineTreeLevels();
        for (World world : getServer().getWorlds()) {
            TreeSerializer.loadTrees(this, world);
        }

        LocaleManager.loadLocale(locale);
        heads = config.getStringList("xmas.presents");
        if (heads.size() == 0) {
            getLogger().warning("[X-Mas] Warning! No heads loaded! Presents can't spawn without box!");
            return;
        }
        gifts = new ArrayList<>();
        for (String serializedItem : config.getStringList("xmas.gifts")) {
            ItemStack item = deserializeItem(serializedItem);
            if (item != null) {
                gifts.add(item);
            } else {
                getLogger().warning("[X-Mas] Failed to load gift item: " + serializedItem);
            }
        }
        if (gifts.size() == 0) {
            getLogger().warning("[X-Mas] Warning! No gifts loaded! No X-Mas without gifts!");
            return;
        }

        LUCK_CHANCE_ENABLED = config.getBoolean("xmas.luck.enabled");
        LUCK_CHANCE = (float) config.getInt("xmas.luck.chance") / 100;
        new Events().registerListener();
        new MagicTask(this).runTaskTimer(this, 5, UPDATE_SPEED);
        new PlayParticlesTask(this).runTaskTimer(this, 5, PARTICLES_DELAY);
        XMas.XMAS_CRYSTAL = new ItemMaker(Material.EMERALD, LocaleManager.CRYSTAL_NAME, LocaleManager.CRYSTAL_LORE).make();
        ItemMeta crystalMeta = XMas.XMAS_CRYSTAL.getItemMeta();
        if (crystalMeta != null) {
            crystalMeta.getPersistentDataContainer().set(crystalKey, PersistentDataType.BYTE, (byte) 1);
            XMas.XMAS_CRYSTAL.setItemMeta(crystalMeta);
        }

        ShapedRecipe grinderRecipe;
        grinderRecipe = new ShapedRecipe(new NamespacedKey(this, "xmas"), XMas.XMAS_CRYSTAL).shape(" d ", "ded", " d ").setIngredient('d', Material.DIAMOND).setIngredient('e', Material.EMERALD);
        Iterator<Recipe> recipes = getServer().recipeIterator();
        boolean registered = false;
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (recipe.equals(grinderRecipe)) {
                registered = true;
                break;
            }

        }
        try {
            if (!registered)
                getServer().addRecipe(grinderRecipe);
        } catch (Exception ignored) {
        }
        XMasCommand.register(this);
        registerPlaceholderApi();
        TextUtils.sendConsoleMessage(LocaleManager.PLUGIN_ENABLED);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        TreeSerializer.loadTrees(this, event.getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        for (MagicTree magicTree : XMas.getAllTrees()) {
            if (magicTree.getLocation().getWorld() == event.getWorld()) magicTree.unbuild();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        config = getConfig();
        locale = config.getString("core.locale");

        inProgress = config.getBoolean("core.plugin-enabled", true);
        UPDATE_SPEED = config.getInt("core.update-speed");
        if (UPDATE_SPEED <= 0) {
            UPDATE_SPEED = 7;
        }
        PARTICLES_DELAY = config.getInt("core.particles-delay");
        if (PARTICLES_DELAY <= 0) {
            PARTICLES_DELAY = 35;
        }
        particlesEnabled = config.getBoolean("core.particles-enabled", true);
        loadSoundConfig();

        autoEnd = config.getBoolean("core.holiday-ends.enabled");
        resourceBack = config.getBoolean("core.holiday-ends.resource-back");
        MAX_TREE_COUNT = config.getInt("core.tree-limit");
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy kk-mm-ss");
            endTime = sdf.parse(config.getString("core.holiday-ends.date")).getTime();
        } catch (ParseException e) {
            TextUtils.sendConsoleMessage("<red>Invalid holiday end date in config.yml");
        }

        defineTreeLevels();
        LocaleManager.loadLocale(locale);
        heads = config.getStringList("xmas.presents");

        gifts = new ArrayList<>();
        for (String serializedItem : config.getStringList("xmas.gifts")) {
            ItemStack item = deserializeItem(serializedItem);
            if (item != null) {
                gifts.add(item);
            } else {
                getLogger().warning("[X-Mas] Failed to deserialize gift item: " + serializedItem);
            }
        }

        LUCK_CHANCE_ENABLED = config.getBoolean("xmas.luck.enabled");
        LUCK_CHANCE = (float) config.getInt("xmas.luck.chance") / 100;
        XMasCommand.refreshCommandConfiguration(this);
        TextUtils.sendConsoleMessage("<green>Configuration reloaded!");
    }

    private void loadSoundConfig() {
        growFirstSoundVolume = clampVolume(config.getDouble("core.sounds.grow.first-volume", 0.5));
        growRepeatSoundVolume = clampVolume(config.getDouble("core.sounds.grow.repeat-volume", 0.2));
    }

    private float clampVolume(double volume) {
        if (Double.isNaN(volume)) {
            return 0;
        }
        return (float) Math.max(0, Math.min(1, volume));
    }

    public void addGiftItem(ItemStack item) {
        ItemStack gift = item.clone();
        String serializedItem = serializeItem(gift);
        if (serializedItem == null) {
            getLogger().warning("Failed to serialize item for saving to the gift list. Item: " + gift);
            return;
        }

        List<String> giftList = config.getStringList("xmas.gifts");
        giftList.add(serializedItem);
        config.set("xmas.gifts", giftList);
        saveConfig();
        gifts.add(gift);
    }

    private String serializeItem(ItemStack item) {
        try {
            byte[] serializedBytes = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(serializedBytes);
        } catch (Exception e) {
            getLogger().severe("Failed to serialize item: " + e.getMessage());
            return null;
        }
    }

    public static ItemStack deserializeItem(String serializedItem) {
        if (serializedItem == null) {
            return null;
        }
        String trimmed = serializedItem.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        ItemStack materialItem = deserializeMaterialItem(trimmed);
        if (materialItem != null) {
            return materialItem;
        }

        if (trimmed.length() > MAX_SERIALIZED_GIFT_LENGTH) {
            Bukkit.getLogger().severe("[X-Mas] Gift item is too large to deserialize safely: " + trimmed.length() + " characters");
            return null;
        }

        try {
            byte[] serializedBytes = Base64.getDecoder().decode(trimmed);
            return ItemStack.deserializeBytes(serializedBytes);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().severe("[X-Mas] Invalid material name or Base64 gift item: " + trimmed);
            return null;
        } catch (Exception e) {
            Bukkit.getLogger().severe("[X-Mas] Failed to deserialize gift item: " + e.getMessage());
            return null;
        }
    }

    private static ItemStack deserializeMaterialItem(String serializedItem) {
        String materialName = serializedItem;
        int amount = 1;
        if (serializedItem.contains(":")) {
            String[] split = serializedItem.split(":", 2);
            materialName = split[0].trim();
            try {
                amount = Integer.parseInt(split[1].trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        Material material = Material.matchMaterial(materialName);
        if (material == null || material.isLegacy() || !material.isItem()) {
            return null;
        }
        int maxStackSize = Math.max(1, material.getMaxStackSize());
        amount = Math.max(1, Math.min(amount, maxStackSize));
        return new ItemStack(material, amount);
    }


    @Override
    public void onDisable() {
        if (placeholderExpansion != null && placeholderExpansion.isRegistered()) {
            placeholderExpansion.unregister();
        }
        if (XMas.getAllTrees().size() > 0)
            for (MagicTree tree : XMas.getAllTrees()) {
                tree.unbuild();
            }
    }

    private void registerPlaceholderApi() {
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        placeholderExpansion = new XMasPlaceholderExpansion(this);
        if (placeholderExpansion.register()) {
            getLogger().info("Registered PlaceholderAPI expansion: " + XMasPlaceholders.IDENTIFIER);
        } else {
            getLogger().warning("PlaceholderAPI is present, but placeholder registration failed.");
        }
    }

    public void end() {
        Bukkit.broadcast(TextUtils.parse("<green>" + LocaleManager.HAPPY_NEW_YEAR));
        inProgress = false;
        config.set("core.plugin-enabled", false);
        saveConfig();
    }

    private void saveDefaults() {
        reloadConfig();
        plugin.saveResource("locales/default.yml", true);
        List<String> defaults = Arrays.asList("locales/en.yml", "locales/ru.yml", "locales/ru_santa.yml", "trees.yml");
        for (String path : defaults)
            if (!new File(getDataFolder(), '/' + path).exists()) plugin.saveResource(path, false);
    }

    private void defineTreeLevels() {

        long sapling_delay = config.getInt("xmas.tree-lvl.sapling.gift-cooldown") * 20 / UPDATE_SPEED;
        long small_delay = config.getInt("xmas.tree-lvl.small_tree.gift-cooldown") * 20 / UPDATE_SPEED;
        long tree_delay = config.getInt("xmas.tree-lvl.tree.gift-cooldown") * 20 / UPDATE_SPEED;
        long magic_delay = config.getInt("xmas.tree-lvl.magic_tree.gift-cooldown") * 20 / UPDATE_SPEED;

        ConfigurationSection lvlups = config.getConfigurationSection("xmas.tree-lvl");
        Map<Material, Integer> saplingLevelUp = TreeSerializer.convertRequirementsMap(lvlups.getConfigurationSection("sapling.lvlup").getValues(false));
        Map<Material, Integer> smallLevelUp = TreeSerializer.convertRequirementsMap(lvlups.getConfigurationSection("small_tree.lvlup").getValues(false));
        Map<Material, Integer> treeLevelUp = TreeSerializer.convertRequirementsMap(lvlups.getConfigurationSection("tree.lvlup").getValues(false));

        TreeLevel.MAGIC_TREE = new TreeLevel("magic_tree",
                getParticleEffect("magic_tree", "ambient", Effects.TREE_WHITE_AMBIENT),
                getParticleEffect("magic_tree", "swag", Effects.TREE_SWAG),
                getParticleEffect("magic_tree", "body", null),
                null, magic_delay, Collections.emptyMap(), new StructureTemplate(new HashMap<Vector, Material>() {
            private static final long serialVersionUID = 1L;

            {
                put(new Vector(0, -1, 0), Material.GRASS_BLOCK);
                for (int i = 0; i <= 5; i++) {
                    put(new Vector(0, i, 0), Material.SPRUCE_LOG);
                    if (i >= 2) {
                        put(new Vector(1, i, 0), Material.SPRUCE_LEAVES);
                        put(new Vector(-1, i, 0), Material.SPRUCE_LEAVES);
                        put(new Vector(0, i, 1), Material.SPRUCE_LEAVES);
                        put(new Vector(0, i, -1), Material.SPRUCE_LEAVES);
                    }
                }
                put(new Vector(0, 6, 0), Material.SPRUCE_LEAVES);

                put(new Vector(0, 7, 0), Material.GLOWSTONE);// Star

                put(new Vector(1, 4, 0), Material.SPRUCE_LEAVES);
                put(new Vector(1, 4, 1), Material.SPRUCE_LEAVES);
                put(new Vector(1, 4, -1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 4, -1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 4, 1), Material.SPRUCE_LEAVES);

                put(new Vector(1, 2, 1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 2, -1), Material.SPRUCE_LEAVES);
                put(new Vector(1, 2, -1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 2, 1), Material.SPRUCE_LEAVES);

                put(new Vector(2, 2, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 2, 2), Material.SPRUCE_LEAVES);
                put(new Vector(-2, 2, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 2, -2), Material.SPRUCE_LEAVES);
            }
        }));

        TreeLevel.TREE = new TreeLevel("tree",
                getParticleEffect("tree", "ambient", Effects.AMBIENT_SNOW),
                getParticleEffect("tree", "swag", Effects.TREE_GOLD_SWAG),
                getParticleEffect("tree", "body", null),
                TreeLevel.MAGIC_TREE, tree_delay, treeLevelUp, new StructureTemplate(new HashMap<Vector, Material>() {
            private static final long serialVersionUID = 1L;

            {
                put(new Vector(0, -1, 0), Material.GRASS_BLOCK);
                put(new Vector(0, 0, 0), Material.SPRUCE_LOG);
                put(new Vector(0, 1, 0), Material.SPRUCE_LOG);
                put(new Vector(0, 2, 0), Material.SPRUCE_LOG);
                put(new Vector(0, 3, 0), Material.SPRUCE_LOG);
                put(new Vector(0, 4, 0), Material.SPRUCE_LOG);
                put(new Vector(0, 5, 0), Material.SPRUCE_LEAVES);
                put(new Vector(1, 4, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 4, 1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 4, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 4, -1), Material.SPRUCE_LEAVES);

                put(new Vector(1, 1, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 1, 1), Material.SPRUCE_LEAVES);
                put(new Vector(1, 1, 1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 1, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 1, -1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 1, -1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 1, 1), Material.SPRUCE_LEAVES);
                put(new Vector(1, 1, -1), Material.SPRUCE_LEAVES);

                put(new Vector(1, 2, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 2, 1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 2, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 2, -1), Material.SPRUCE_LEAVES);

            }
        }));

        TreeLevel.SMALL_TREE = new TreeLevel("small_tree",
                getParticleEffect("small_tree", "ambient", Effects.AMBIENT_PORTAL),
                getParticleEffect("small_tree", "swag", Effects.TREE_RED_SWAG),
                getParticleEffect("small_tree", "body", null),
                TreeLevel.TREE, small_delay, smallLevelUp, new StructureTemplate(new HashMap<Vector, Material>() {
            private static final long serialVersionUID = 1L;

            {
                put(new Vector(0, -1, 0), Material.GRASS_BLOCK);
                put(new Vector(0, 0, 0), Material.SPRUCE_LOG);
                put(new Vector(0, 1, 0), Material.SPRUCE_LOG);
                put(new Vector(0, 2, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 3, 0), Material.SPRUCE_LEAVES);

                put(new Vector(1, 1, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 1, 1), Material.SPRUCE_LEAVES);
                put(new Vector(1, 1, 1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 1, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 1, -1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 1, -1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 1, 1), Material.SPRUCE_LEAVES);
                put(new Vector(1, 1, -1), Material.SPRUCE_LEAVES);

                put(new Vector(1, 2, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 2, 1), Material.SPRUCE_LEAVES);
                put(new Vector(-1, 2, 0), Material.SPRUCE_LEAVES);
                put(new Vector(0, 2, -1), Material.SPRUCE_LEAVES);

            }
        }));

        TreeLevel.SAPLING = new TreeLevel("sapling",
                getParticleEffect("sapling", "ambient", Effects.AMBIENT_SAPLING),
                getParticleEffect("sapling", "swag", null),
                getParticleEffect("sapling", "body", null),
                TreeLevel.SMALL_TREE, sapling_delay, saplingLevelUp, new StructureTemplate(new HashMap<Vector, Material>() {
            private static final long serialVersionUID = 1L;

            {
                put(new Vector(0, -1, 0), Material.GRASS_BLOCK);
                put(new Vector(0, 0, 0), Material.SPRUCE_SAPLING);
            }
        }));
    }

    private ParticleContainer getParticleEffect(String level, String effect, ParticleContainer fallback) {
        String path = "xmas.tree-lvl." + level + ".particles." + effect;
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return fallback;
        }
        if (!section.getBoolean("enabled", fallback != null)) {
            return null;
        }

        String configuredParticle = section.getString("particle", fallback != null ? fallback.getType().name() : null);
        if (configuredParticle == null || configuredParticle.trim().isEmpty()) {
            return fallback;
        }

        Particle particle;
        try {
            particle = Particle.valueOf(configuredParticle.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            getLogger().warning("[X-Mas] Unknown particle '" + configuredParticle + "' at " + path + ". Using fallback.");
            return fallback;
        }

        if (particle.getDataType() != Void.class && particle != Particle.DUST) {
            getLogger().warning("[X-Mas] Particle '" + particle.name() + "' needs extra data and is not supported in config yet. Using fallback.");
            return fallback;
        }

        return new ParticleContainer(
                particle,
                (float) section.getDouble("offset-x", fallback != null ? fallback.getOffsetX() : 0),
                (float) section.getDouble("offset-y", fallback != null ? fallback.getOffsetY() : 0),
                (float) section.getDouble("offset-z", fallback != null ? fallback.getOffsetZ() : 0),
                (float) section.getDouble("speed", fallback != null ? fallback.getSpeed() : 0),
                Math.max(0, section.getInt("count", fallback != null ? fallback.getCount() : 0))
        );
    }
}
