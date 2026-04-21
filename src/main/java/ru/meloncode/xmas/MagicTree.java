package ru.meloncode.xmas;

import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.*;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import ru.meloncode.xmas.utils.TextUtils;
import org.bukkit.util.Vector;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class MagicTree {
    private static final ConcurrentHashMap<Block, UUID> blockAssociation = new ConcurrentHashMap<>();
    private final UUID owner;
    private final Location location;
    private final UUID treeuid;
    TreeLevel level;
    private Map<Material, Integer> levelupRequirements;
    private Set<Block> blocks;
    private long presentCounter;
    private int scheduledPresents;

    public MagicTree(UUID owner, TreeLevel level, Location location) {
        this.treeuid = UUID.randomUUID();
        this.owner = owner;
        this.level = level;
        this.location = location;
        this.levelupRequirements = new HashMap<>(level.getLevelupRequirements());
        if (Main.inProgress)
            build();
        presentCounter = 0;
        scheduledPresents = 0;
    }

    public MagicTree(UUID owner, UUID uid, TreeLevel level, Location location, Map<Material, Integer> levelupRequirements,
                     long presentCounter, int scheduledPresents) {
        this.owner = owner;
        this.treeuid = uid;
        this.level = level;
        this.location = location;
        this.levelupRequirements = new HashMap<>(levelupRequirements);
        this.presentCounter = 0;
        this.presentCounter = presentCounter;
        if (Main.inProgress)
            build();
        this.scheduledPresents = scheduledPresents;
    }

    public static MagicTree getTreeByBlock(Block block) {
        if (block == null) {
            return null;
        }
        return XMas.getTree(blockAssociation.get(block));
    }

    public static boolean isBlockBelongs(Block block) {
        if (block == null) {
            return false;
        }
        return blockAssociation.containsKey(block);
    }

    public UUID getOwner() {
        return owner;
    }

    public Player getPlayerOwner() {
        if (Bukkit.getPlayer(owner) != null) {
            return Bukkit.getPlayer(owner);
        }
        return null;
    }

    public TreeLevel getLevel() {
        return level;
    }

    public Location getLocation() {
        return location;
    }

    public Map<Material, Integer> getLevelupRequirements() {
        return levelupRequirements;
    }

    public boolean grow(Material material) {
        if (levelupRequirements.containsKey(material)) {
            int levelRequirement = level.getLevelupRequirements().getOrDefault(material, levelupRequirements.get(material));
            int remainingBefore = levelupRequirements.get(material);
            float volume = levelRequirement == remainingBefore ? Main.growFirstSoundVolume : Main.growRepeatSoundVolume;
            if (levelupRequirements.get(material) <= 1) {
                levelupRequirements.remove(material);
            } else {
                levelupRequirements.put(material, levelupRequirements.get(material) - 1);
            }
            for (Block block : blocks) {
                if (block.getType() == Material.SPRUCE_LEAVES || block.getType() == Material.SPRUCE_SAPLING) {
                    Effects.GROW.playEffect(block.getLocation());
                }
            }
            if (volume > 0) {
                location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, volume, Main.RANDOM.nextFloat() + 0.2f);
            }
            save();
            return true;
        }
        return false;
    }

    public void update() {
        if (Main.inProgress) {
            if (level.getGiftDelay() > 0) {
                if (presentCounter == 0) {
                    spawnPresent();
                    presentCounter = (long) ((level.getGiftDelay() * 1.25 - level.getGiftDelay() * 0.75) + level.getGiftDelay() * 0.75);
                }
                presentCounter--;
            }
        }
    }

    public void playParticles()
    {
        if (blocks != null && blocks.size() > 0) {
            for (Block block : blocks) {
                if (!block.getChunk().isLoaded())
                    continue;
                if (block.getType() == Material.SPRUCE_LEAVES) {
                    if (level.getSwagEffect() != null) {
                        level.getSwagEffect().playEffect(block.getLocation());
                    }
                }
                if (block.getType() == Material.SPRUCE_LOG) {
                    if (level.getBodyEffect() != null) {
                        level.getBodyEffect().playEffect(block.getLocation());
                    }
                }
                if (level.getAmbientEffect() != null) {
                    level.getAmbientEffect().playEffect(location.clone().add(0, level.getTreeHeight(), 0));
                }
            }
        }
    }

    public boolean tryLevelUp() {

        if (level.hasNext()) {
            if (level.nextLevel.getStructureTemplate().canGrow(location)) {
                levelUp();
                return true;
            }
        }

        return false;
    }

    private void levelUp() {
        unbuild();
        this.level = level.nextLevel;
        this.levelupRequirements = new HashMap<>(level.getLevelupRequirements());
        for (int i = 0; i <= 3; i++) {
            Firework fw = location.getWorld().spawn(location.clone().add(new Vector(-3 + Main.RANDOM.nextInt(6), 3, -3 + Main.RANDOM.nextInt(6))), Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().trail(true).withColor(Color.RED).withFade(Color.LIME).withFlicker().with(Type.BURST).build());
            fw.setFireworkMeta(meta);
            fw.setMetadata("nodamage", new FixedMetadataValue(Main.getInstance(), true));
        }
        build();
        save();
    }

    public void unbuild() {
        Block block;
        Location loc;
        for (Entry<Block, UUID> cBlock : blockAssociation.entrySet()) {
            if (cBlock.getValue().equals(treeuid)) {
                block = cBlock.getKey();
                loc = block.getLocation();
                loc.getWorld().playEffect(loc, Effect.STEP_SOUND, block.getType());
                block.setType(Material.AIR);
                blockAssociation.remove(block);
            }
        }
        location.clone().add(0, -1, 0).getBlock().setType(Material.GRASS_BLOCK);
        blocks = null;
    }

    public void build() {
        if (blocks != null && !blocks.isEmpty()) {
            return;
        }
        if (level.getStructureTemplate().canGrow(location)) {
            blocks = level.getStructureTemplate().build(location);
            for (Block block : blocks) {
                blockAssociation.put(block, getTreeUID());
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void spawnPresent() {
        if (!location.getChunk().isLoaded())
        {
            if(scheduledPresents + 1 <= 8)
                scheduledPresents++;
            return;
        }

        Location presentLoc = location.clone().add(-1 + Main.RANDOM.nextInt(3), 0, -1 + Main.RANDOM.nextInt(3));

        Block pBlock = presentLoc.getBlock();
        if (!pBlock.getType().isSolid() && pBlock.getType() != Material.SPRUCE_SAPLING)
        {
            pBlock.setType(Material.PLAYER_HEAD);
            BlockState state = pBlock.getState();
            if (state instanceof Skull) {
                Skull skull = (Skull) state;
                BlockFace face;
                do {
                    face = BlockFace.values()[Main.RANDOM.nextInt(BlockFace.values().length)];
                }
                while (face == BlockFace.DOWN || face == BlockFace.UP || face == BlockFace.SELF);
                //skull.setRotation(face);
                Rotatable skullRotatable = (Rotatable) skull.getBlockData();
                skullRotatable.setRotation(face);
                skull.setRotation(face);
                //skull.setSkullType(SkullType.PLAYER);
                skull.setType(Material.PLAYER_HEAD);
                //skull.setOwner();
                applyConfiguredHead(skull, Main.getHeads().get(Main.RANDOM.nextInt(Main.getHeads().size())));
                skull.update(true);
            }
        }
    }

    private void applyConfiguredHead(Skull skull, String configuredHead) {
        if (configuredHead == null || configuredHead.trim().isEmpty()) {
            return;
        }
        String trimmedHead = configuredHead.trim();
        if (!trimmedHead.contains("://")) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(trimmedHead));
            return;
        }
        try {
            URL skinUrl = new URL(trimmedHead);
            if (!"textures.minecraft.net".equalsIgnoreCase(skinUrl.getHost())) {
                Bukkit.getLogger().warning("[X-Mas] Ignoring non-Mojang present skin URL: " + trimmedHead);
                return;
            }
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(skinUrl);
            profile.setTextures(textures);
            skull.setOwnerProfile(profile);
        } catch (MalformedURLException e) {
            Bukkit.getLogger().warning("[X-Mas] Invalid present skin URL: " + trimmedHead);
        }
    }

    public boolean canLevelUp() {
        return getLevelupRequirements().size() == 0;
    }

    public UUID getTreeUID() {
        return treeuid;
    }

    public void save() {
        TreeSerializer.saveTree(this);
    }

    public void end() {
        end(getPlayerOwner());
    }

    public void end(Player refundTarget) {
        unbuild();
        clearNearbyPresents();
        if (Main.resourceBack) {
            refundResources(refundTarget);
        }
        XMas.removeTree(this, false);
    }

    private void clearNearbyPresents() {
        Block bl;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                bl = location.clone().add(x, 0, z).getBlock();
                if (bl.getType() == Material.PLAYER_HEAD) {
                    bl.setType(Material.AIR);
                }
            }
        }
    }

    private void refundResources(Player refundTarget) {
        List<ItemStack> refundItems = collectRefundItems();
        if (refundItems.isEmpty()) {
            return;
        }

        List<ItemStack> leftovers = putRefundsInContainer(Material.CHEST, refundItems);
        if (leftovers != null) {
            dropRefunds(leftovers);
            notifyRefund(refundTarget, "<green>Your tree resources were returned in a chest.</green>");
            return;
        }

        leftovers = putRefundsInContainer(Material.BARREL, refundItems);
        if (leftovers != null) {
            dropRefunds(leftovers);
            notifyRefund(refundTarget, "<green>Your tree resources were returned in a barrel.</green>");
            return;
        }

        leftovers = refundTarget != null ? addItems(refundTarget.getInventory(), refundItems) : refundItems;
        dropRefunds(leftovers);
        if (refundTarget != null) {
            if (leftovers.isEmpty()) {
                notifyRefund(refundTarget, "<green>Your tree resources were returned to your inventory.</green>");
            } else {
                notifyRefund(refundTarget, "<gold>Your tree resources were returned. Inventory overflow dropped at the tree.</gold>");
            }
        }
    }

    private List<ItemStack> collectRefundItems() {
        List<ItemStack> refundItems = new ArrayList<>();

        TreeLevel cLevel = TreeLevel.SAPLING;
        while (cLevel != null && cLevel != level) {
            addRequirements(refundItems, cLevel.getLevelupRequirements());
            cLevel = cLevel.nextLevel;
        }

        if (level.getLevelupRequirements() != null) {
            for (Entry<Material, Integer> currItem : level.getLevelupRequirements().entrySet()) {
                int remaining = getLevelupRequirements().getOrDefault(currItem.getKey(), 0);
                int spent = Math.max(0, currItem.getValue() - remaining);
                if (spent > 0) {
                    refundItems.add(new ItemStack(currItem.getKey(), spent));
                }
            }
        }
        return refundItems;
    }

    private void addRequirements(List<ItemStack> refundItems, Map<Material, Integer> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return;
        }
        for (Entry<Material, Integer> currItem : requirements.entrySet()) {
            if (currItem.getValue() > 0) {
                refundItems.add(new ItemStack(currItem.getKey(), currItem.getValue()));
            }
        }
    }

    private List<ItemStack> putRefundsInContainer(Material containerMaterial, List<ItemStack> refundItems) {
        Block refundBlock = location.getBlock();
        try {
            refundBlock.setType(containerMaterial, false);
            BlockState state = refundBlock.getState();
            if (state instanceof Container container) {
                return addItems(container.getInventory(), refundItems);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[X-Mas] Failed to place refund " + containerMaterial + ": " + e.getMessage());
        }
        refundBlock.setType(Material.AIR, false);
        return null;
    }

    private List<ItemStack> addItems(Inventory inventory, List<ItemStack> items) {
        List<ItemStack> clones = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                clones.add(item.clone());
            }
        }
        Map<Integer, ItemStack> leftovers = inventory.addItem(clones.toArray(new ItemStack[0]));
        return new ArrayList<>(leftovers.values());
    }

    private void dropRefunds(List<ItemStack> items) {
        if (items == null || items.isEmpty() || location.getWorld() == null) {
            return;
        }
        Location dropLocation = location.clone().add(0.5, 0.5, 0.5);
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                location.getWorld().dropItemNaturally(dropLocation, item.clone());
            }
        }
    }

    private void notifyRefund(Player refundTarget, String message) {
        if (refundTarget != null && refundTarget.isOnline()) {
            TextUtils.sendRawMessage(refundTarget, message);
        }
    }

    public long getPresentCounter() {
        return presentCounter;
    }

    public int getScheduledPresents() {
        return scheduledPresents;
    }

    public boolean hasScheduledPresents() {
        return scheduledPresents > 0;
    }

    public void spawnScheduledPresents() {
        for(int i = scheduledPresents; i > 0; i--)
            spawnPresent();
        scheduledPresents = 0;
    }
}
