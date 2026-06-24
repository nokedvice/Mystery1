package hu.bloodplugin.commands;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.managers.BloodMoonManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BloodMoonCommand implements CommandExecutor {

    private final BloodPlugin plugin;
    private final BloodMoonManager manager;

    public BloodMoonCommand(BloodPlugin plugin, BloodMoonManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bloodplugin.bloodmoon")) {
            sender.sendMessage(Component.text("Nincs jogosultságod!", NamedTextColor.RED));
            return true;
        }
        manager.scheduleForceNextNight();
        sender.sendMessage(Component.text(
            "A következő éjszaka garantált Blood Moon lesz!", NamedTextColor.DARK_RED));
        return true;
    }
}
