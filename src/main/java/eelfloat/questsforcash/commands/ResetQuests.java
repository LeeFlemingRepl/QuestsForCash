package eelfloat.questsforcash.commands;

import eelfloat.questsforcash.QuestsForCash;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ResetQuests implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equals("confirm")) {
            QuestsForCash.plugin.quests.clear();
            QuestsForCash.plugin.tickQuests();
            sender.sendMessage("Quests cleared for all players");
        } else if (args.length > 0 && args[0].equals("shared")) {
            QuestsForCash.plugin.sharedQuests.clear();
            QuestsForCash.plugin.tickQuests();
            sender.sendMessage("Shared quests cleared");
        } else if (args.length > 0) {
            Player player = null;

            // Try by UUID...
            try {
                UUID uuid = UUID.fromString(args[0]);
                player = QuestsForCash.plugin.getServer().getPlayer(uuid);
            } catch(IllegalArgumentException ignored) {
            }

            // Try by username
            if (player == null) {
                player = QuestsForCash.plugin.getServer().getPlayer(args[0]);
            }

            // Give up
            if (player == null) {
                sender.sendMessage("Player \"" + args[0] + "\" not found");
                return true;
            }

            QuestsForCash.plugin.quests.remove(player.getUniqueId());
            QuestsForCash.plugin.tickQuests();
            sender.sendMessage("Quests cleared for player " + player.getName());
        } else {
            sender.sendMessage("Are you sure you want to clear quests for all players? Run /resetquests confirm to confirm.");
        }
        return true;
    }
}
