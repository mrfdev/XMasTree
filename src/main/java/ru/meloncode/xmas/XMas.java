package ru.meloncode.xmas;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.meloncode.xmas.utils.LocationUtils;
import ru.meloncode.xmas.utils.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static ru.meloncode.xmas.Main.RANDOM;

class XMas {

    private static final ConcurrentHashMap<UUID, MagicTree> trees = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, List<MagicTree>> trees_byChunk = new ConcurrentHashMap<>();
    public static ItemStack XMAS_CRYSTAL;

    public static void createMagicTree(Player player, Location loc) {
        MagicTree tree = new MagicTree(player.getUniqueId(), TreeLevel.SAPLING, loc);
        trees.put(tree.getTreeUID(), tree);
        trees_byChunk.computeIfAbsent(LocationUtils.getChunkKey(tree.getLocation()), aLong -> new ArrayList<>()).add(tree);
        tree.save();
    }

    public static void addMagicTree(MagicTree tree) {
        trees.put(tree.getTreeUID(), tree);
        trees_byChunk.computeIfAbsent(LocationUtils.getChunkKey(tree.getLocation()), aLong -> new ArrayList<>());
        List<MagicTree> chunkTrees = trees_byChunk.get(LocationUtils.getChunkKey(tree.getLocation()));
        chunkTrees.removeIf(existingTree -> existingTree.getTreeUID().equals(tree.getTreeUID()));
        chunkTrees.add(tree);
        tree.build();
    }

    public static Collection<MagicTree> getAllTrees() {
        return trees.values();
    }

    public static Collection<MagicTree> getAllTreesInChunk(Chunk chunk) {
        return trees_byChunk.get(LocationUtils.getChunkKey(chunk));
    }

    public static void removeTree(MagicTree tree) {
        removeTree(tree, true);
    }

    public static void removeTree(MagicTree tree, boolean unbuild) {
        if (unbuild) {
            tree.unbuild();
        }
        TreeSerializer.removeTree(tree);
        trees.remove(tree.getTreeUID());
        List<MagicTree> chunkTrees = trees_byChunk.get(LocationUtils.getChunkKey(tree.getLocation()));
        if (chunkTrees != null) {
            chunkTrees.remove(tree);
            if (chunkTrees.isEmpty()) {
                trees_byChunk.remove(LocationUtils.getChunkKey(tree.getLocation()));
            }
        }
    }

    public static void processPresent(Block block, Player player) {
        if (block.getType() == Material.PLAYER_HEAD) {
            Skull skull = (Skull) block.getState();
            String headId = getHeadIdentifier(skull);

            if (headId != null && Main.getHeads().contains(headId)) {
                Location loc = block.getLocation();
                World world = loc.getWorld();
                if (world != null) {
                    if (RANDOM.nextFloat() < Main.LUCK_CHANCE || !Main.LUCK_CHANCE_ENABLED) {
                        world.dropItemNaturally(loc, new ItemStack(Main.gifts.get(RANDOM.nextInt(Main.gifts.size()))));
                        Effects.TREE_SWAG.playEffect(loc);
                        TextUtils.sendMessage(player, LocaleManager.GIFT_LUCK);
                    } else {
                        Effects.SMOKE.playEffect(loc);
                        world.dropItemNaturally(loc, new ItemStack(Material.COAL));
                        TextUtils.sendMessage(player, LocaleManager.GIFT_FAIL);
                    }
                }
                block.setType(Material.AIR);
            }
        }
    }

    static String getHeadIdentifier(Skull skull) {
        if (skull.getPlayerProfile() != null
                && skull.getPlayerProfile().getTextures() != null
                && skull.getPlayerProfile().getTextures().getSkin() != null) {
            return skull.getPlayerProfile().getTextures().getSkin().toString();
        }
        return skull.getOwningPlayer() != null ? skull.getOwningPlayer().getName() : null;
    }

    public static List<MagicTree> getTreesPlayerOwn(Player player) {
        List<MagicTree> own = new ArrayList<>();
        for (MagicTree cTree : getAllTrees())
            if (cTree.getOwner().equals(player.getUniqueId()))
                own.add(cTree);
        return own;
    }

    public static MagicTree getTree(UUID treeUID) {
        return trees.get(treeUID);
    }
}
