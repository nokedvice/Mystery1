package hu.bloodplugin.managers;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class BloodOreManager {

    private final BloodPlugin plugin;
    private final Random random = new Random();
    public static final NamespacedKey BLOOD_ORE_KEY = new NamespacedKey("bloodplugin", "blood_ore");

    public BloodOreManager(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    public void startOreSpawner() {
        // Listen on chunk generation via event (see BloodOreListener)
        // This manager just holds logic used by the listener
    }

    /**
     * Called from BloodOreListener on chunk populate.
     * Spawns Blood Ore blocks in the chunk.
     */
    public void populateChunk(Chunk chunk) {
        int attempts = plugin.getConfig().getInt("blood-ore.attempts-per-chunk", 6);
        int minY     = plugin.getConfig().getInt("blood-ore.min-y", -64);
        int maxY     = plugin.getConfig().getInt("blood-ore.max-y", 16);

        for (int i = 0; i < attempts; i++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int y = minY + random.nextInt(maxY - minY + 1);

            Block block = chunk.getBlock(x, y, z);
            if (block.getType() == Material.DEEPSLATE) {
                block.setType(Material.DEEPSLATE_EMERALD_ORE);
                // Tag the block as blood ore via chunk PDC
                chunk.getPersistentDataContainer().set(
                        new NamespacedKey("bloodplugin", "ore_" + x + "_" + y + "_" + z),
                        PersistentDataType.BYTE, (byte) 1
                );
            }
        }
    }

    /**
     * Check if a block at (x,y,z) in its chunk is tagged as blood ore.
     */
    public boolean isBloodOre(Block block) {
        Chunk chunk = block.getChunk();
        int x = block.getX() & 15;
        int z = block.getZ() & 15;
        int y = block.getY();
        NamespacedKey k = new NamespacedKey("bloodplugin", "ore_" + x + "_" + y + "_" + z);
        return chunk.getPersistentDataContainer().has(k, PersistentDataType.BYTE);
    }

    public void removeBloodOreTag(Block block) {
        Chunk chunk = block.getChunk();
        int x = block.getX() & 15;
        int z = block.getZ() & 15;
        int y = block.getY();
        NamespacedKey k = new NamespacedKey("bloodplugin", "ore_" + x + "_" + y + "_" + z);
        chunk.getPersistentDataContainer().remove(k);
    }
}
