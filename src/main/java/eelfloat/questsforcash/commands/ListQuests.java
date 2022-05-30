package eelfloat.questsforcash.commands;

import eelfloat.questsforcash.Quest;
import eelfloat.questsforcash.QuestsForCash;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ListQuests implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player");
            return true;
        }

        List<Quest> quests = QuestsForCash.plugin.quests.get(((Player) sender).getUniqueId());
        for (Quest quest: quests) {
            TextComponent base = new TextComponent();
            TextComponent dashes = new TextComponent("===");
            dashes.setColor(ChatColor.DARK_GRAY);
            base.addExtra(dashes);
            TextComponent questText = new TextComponent(" Quest: ");
            questText.setColor(ChatColor.GRAY);
            base.addExtra(questText);
            base.addExtra(new TextComponent(quest.questData.title));
            switch (quest.kind) {
                case EVENT:
                    TextComponent event = new TextComponent(" [event]");
                    event.setColor(ChatColor.GREEN);
                    base.addExtra(event);
                    break;
                case SHARED:
                    TextComponent shared = new TextComponent(" [shared]");
                    shared.setColor(ChatColor.BLUE);
                    base.addExtra(shared);
                    break;
                case STREAK:
                    TextComponent streak = new TextComponent(String.format(" [streak: %d]", quest.streak));
                    streak.setColor(ChatColor.GREEN);
                    base.addExtra(streak);
                    break;
            }
            base.addExtra(new TextComponent(" "));
            base.addExtra(dashes);
            sender.spigot().sendMessage(base);

            if (quest.complete) {
                TextComponent base5 = new TextComponent("Complete!");
                base5.setColor(ChatColor.GREEN);
                sender.spigot().sendMessage(base5);
            } else {
                TextComponent base2 = new TextComponent();
                TextComponent taskText = new TextComponent("Task: ");
                taskText.setColor(ChatColor.GRAY);
                base2.addExtra(taskText);
                String task = null;
                switch (quest.questData.type) {
                    case Fetch:
                        task = String.format(
                            "Fetch %s %s",
                                (int) Math.ceil(quest.questData.quantity * quest.multiplier()),
                            quest.questData.material.getKey().getKey().toLowerCase().replace("_", " ")
                        );
                        break;
                }
                base2.addExtra(new TextComponent(task));
                sender.spigot().sendMessage(base2);
            }
            TextComponent base3 = new TextComponent();
            TextComponent expiresText = new TextComponent("Expires: ");
            expiresText.setColor(ChatColor.GRAY);
            base3.addExtra(expiresText);
            long expireMs = Math.max((quest.created + quest.questData.expires * 1000L) - System.currentTimeMillis(), 0);
            String expireString = expireMs < (60 * 1000)
                    ? "soon!"
                    : expireMs < (60 * 60 * 1000)
                    ? String.format("in %d minutes", expireMs / (60 * 1000))
                    : String.format("in %d hours", expireMs / (60 * 60 * 1000));
            TextComponent expiresString = new TextComponent(expireString);
            base3.addExtra(expiresString);
            sender.spigot().sendMessage(base3);

            if (!quest.complete) {
                TextComponent base4 = new TextComponent();

                TextComponent submitButton = new TextComponent("submit");
                submitButton.setBold(true);
                submitButton.setColor(ChatColor.GOLD);
                submitButton.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/submitquest " + quest.questData.id
                ));
                base4.addExtra("[");
                base4.addExtra(submitButton);
                base4.addExtra("]");

                TextComponent reward = new TextComponent(" Reward: ");
                reward.setColor(ChatColor.GRAY);
                base4.addExtra(reward);

                TextComponent rewardNumber = new TextComponent(String.format("$%s", quest.reward()));
                rewardNumber.setColor(ChatColor.GREEN);
                base4.addExtra(rewardNumber);

                sender.spigot().sendMessage(base4);
            }
        }

        return true;
    }
}
