package hu.bloodplugin.commands;

import hu.bloodplugin.BloodPlugin;
import hu.bloodplugin.items.BloodItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LegendaryCommand implements CommandExecutor, TabCompleter {

    private final BloodPlugin plugin;

    private static final List<String> ITEMS = List.of(
        "blood", "blood_shard", "blood_gem", "blood_mace",
        "finisher_potion", "blood_shield"
    );

    public LegendaryCommand(BloodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bloodplugin.legendary")) {
            sender.sendMessage(Component.text("Nincs jogosultságod!", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Csak játékos használhatja!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showMenu(player);
            return true;
        }

        ItemStack item = resolveItem(args[0].toLowerCase());
        if (item == null) {
            player.sendMessage(Component.text(
                "Ismeretlen item: " + args[0] + ". Elérhető: " + String.join(", ", ITEMS),
                NamedTextColor.RED));
            return true;
        }

        player.getInventory().addItem(item);
        player.sendMessage(Component.text("Megkaptad: ", NamedTextColor.GOLD)
            .append(item.displayName()));
        return true;
    }

    private void showMenu(Player player) {
        player.sendMessage(Component.text("═══ /legendary <item> ═══", NamedTextColor.DARK_RED));
        for (String name : ITEMS) {
            player.sendMessage(Component.text("  • " + name, NamedTextColor.RED));
        }
    }

    private ItemStack resolveItem(String name) {
        return switch (name) {
            case "blood"           -> BloodItems.createBloodDrop();
            case "blood_shard"     -> BloodItems.createBloodShard();
            case "blood_gem"       -> BloodItems.createBloodGem();
            case "blood_mace"      -> BloodItems.createBloodMace();
            case "finisher_potion" -> BloodItems.createFinisherPotion();
            case "blood_shield"    -> BloodItems.createBloodShield();
            default                -> null;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return ITEMS.stream().filter(s -> s.startsWith(partial)).toList();
        }
        return List.of();
    }
}
