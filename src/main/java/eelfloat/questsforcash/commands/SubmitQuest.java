package eelfloat.questsforcash.commands;

import eelfloat.questsforcash.Quest;
import eelfloat.questsforcash.QuestsForCash;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class SubmitQuest implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player");
            return true;
        }

        List<Quest> quests = QuestsForCash.plugin.quests.get(((Player) sender).getUniqueId());
        Optional<Quest> questOpt = quests.stream().filter(qst -> {
            return !qst.complete && qst.questData.id.equals(args[0]);
        }).findFirst();
        if (!questOpt.isPresent()) {
            sender.sendMessage("Quest \"" + args[0] + "\" not found");
            return true;
        }
        Quest quest = questOpt.get();

        switch (quest.questData.type) {
            case Fetch:
                PlayerInventory inventory = ((Player) sender).getInventory();
                int quantity = (int) Math.ceil(((double) quest.questData.quantity) * quest.multiplier());
                if (!inventory.contains(quest.questData.material, quantity)) {
                    sender.sendMessage("Insufficient items to complete quest.");
                    return true;
                }
                ItemStack target = new ItemStack(quest.questData.material, quantity);
                HashMap<Integer, ItemStack> map = inventory.removeItem(target);
                assert map.isEmpty();
                break;
            default: throw new RuntimeException("Unreachable");
        }

        quest.complete = true;
        QuestsForCash.plugin.econ.depositPlayer((OfflinePlayer) sender, quest.reward());
        sender.sendMessage(String.format("Quest complete! Received $%s.", quest.reward()));
        return true;
    }
}
