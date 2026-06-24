package hu.bloodplugin.listeners;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import hu.bloodplugin.managers.BloodOreManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class BloodOreListener implements Listener {

    private final BloodPlugin plugin;
    private final BloodOreManager oreManager;
    private final Random random = new Random();

    public BloodOreListener(BloodPlugin plugin) {
        this.plugin     = plugin;
        this.oreManager = new BloodOreManager(plugin);
    }

    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) {
        oreManager.populateChunk(event.getChunk());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DEEPSLATE_EMERALD_ORE) return;
        if (!oreManager.isBloodOre(block)) return;

        event.setDropItems(false);
        oreManager.removeBloodOreTag(block);

        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        int fortuneLevel = held.getEnchantmentLevel(Enchantment.FORTUNE);
        int drops = 1;
        if (fortuneLevel > 0 && random.nextDouble() < 0.20) {
            drops = 2;
        }
        for (int i = 0; i < drops; i++) {
            block.getWorld().dropItemNaturally(block.getLocation(), BloodItems.createBloodShard());
        }
    }
}
