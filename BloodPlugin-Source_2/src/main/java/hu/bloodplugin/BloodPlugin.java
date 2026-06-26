package hu.bloodplugin;

import hu.bloodplugin.commands.AltarSpawnCommand;
import hu.bloodplugin.commands.BloodMoonCommand;
import hu.bloodplugin.commands.LegendaryCommand;
import hu.bloodplugin.items.BloodItems;
import hu.bloodplugin.listeners.*;
import hu.bloodplugin.managers.BloodMoonManager;
import hu.bloodplugin.managers.BloodOreManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BloodPlugin extends JavaPlugin {

    private static BloodPlugin instance;
    private BloodMoonManager bloodMoonManager;
    public BloodOreManager bloodOreManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        BloodItems.init(this);

        bloodMoonManager = new BloodMoonManager(this);
        bloodOreManager  = new BloodOreManager(this);

        BloodAltarListener altarListener = new BloodAltarListener(this);

        // Clean up orphaned holograms from previous session
        getServer().getScheduler().runTaskLater(this, () ->
            altarListener.cleanupAllOrphanedHolograms(), 20L);

        getServer().getPluginManager().registerEvents(new BloodDropListener(this), this);
        getServer().getPluginManager().registerEvents(new FinisherPotionListener(this), this);
        getServer().getPluginManager().registerEvents(new BloodGemListener(this), this);
        getServer().getPluginManager().registerEvents(new BloodShieldListener(this), this);
        getServer().getPluginManager().registerEvents(altarListener, this);
        getServer().getPluginManager().registerEvents(new BloodMoonListener(this, bloodMoonManager), this);
        getServer().getPluginManager().registerEvents(new BloodOreListener(this), this);
        getServer().getPluginManager().registerEvents(new BloodMaceListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemLimitListener(this), this);

        getCommand("bloodmoon").setExecutor(new BloodMoonCommand(this, bloodMoonManager));

        LegendaryCommand legendaryCmd = new LegendaryCommand(this, altarListener);
        getCommand("legendary").setExecutor(legendaryCmd);
        getCommand("legendary").setTabCompleter(legendaryCmd);

        getCommand("altarspawn").setExecutor(new AltarSpawnCommand(this, altarListener));

        bloodMoonManager.startNightChecker();

        getLogger().info("BloodPlugin v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BloodPlugin disabled!");
    }

    public static BloodPlugin getInstance() { return instance; }
    public BloodMoonManager getBloodMoonManager() { return bloodMoonManager; }
}
