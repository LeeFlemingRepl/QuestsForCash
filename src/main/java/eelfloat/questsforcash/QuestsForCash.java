package eelfloat.questsforcash;

import eelfloat.questsforcash.commands.ListQuests;
import eelfloat.questsforcash.commands.ResetQuests;
import eelfloat.questsforcash.commands.SubmitQuest;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class QuestsForCash extends JavaPlugin implements Listener {
    public static QuestsForCash plugin;
    public Logger logger;
    /** Parsed quest pool */
    public ArrayList<QuestData> questData = new ArrayList<>();
    /** Per-user active quests */
    public HashMap<UUID, List<Quest>> quests = new HashMap<>();
    /** Active quests shared between all users */
    public List<Quest> sharedQuests = new ArrayList<>();
    public int max_random_quests;
    public int max_shared_quests;
    private File activeQuestsFile;
    private YamlConfiguration activeQuests;
    public Economy econ;


    @Override
    public void onEnable() {
        plugin = this;
        logger = this.getLogger();
        logger.info("Call 1-800-QUESTS-FOR-CASH now, that's 1-800-QUESTS-FOR-CASH.");

        FileConfiguration config = this.getConfig();
        max_random_quests = config.getInt("max_random_quests");
        max_shared_quests = config.getInt("max_shared_quests");

        this.getCommand("quests").setExecutor(new ListQuests());
        this.getCommand("submitquest").setExecutor(new SubmitQuest());
        this.getCommand("resetquests").setExecutor(new ResetQuests());
        this.getServer().getScheduler().runTaskTimer(this, this::tickQuests, 0, 60 * 20);
        this.getServer().getPluginManager().registerEvents(this, this);

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) throw new RuntimeException("VaultAPI not found");
        econ = rsp.getProvider();

        this.saveDefaultConfig();
        List<Map<?, ?>> questConfigs = config.getMapList("quests");
        for (Map<?, ?> quest: questConfigs) {
            questData.add(new QuestData(quest));
        }

        try {
            activeQuestsFile = new File(this.getDataFolder(), "activeQuests.yml");
            activeQuests = YamlConfiguration.loadConfiguration(activeQuestsFile);

            // Type erasure is lame, can't do any non-exceptional safety checks
            //noinspection unchecked
            List<Quest> sharedQuests = activeQuests.getObject("sharedQuests", List.class);
            if (sharedQuests != null) this.sharedQuests = sharedQuests;

            //noinspection unchecked
            ConfigurationSection playerQuests = activeQuests.getConfigurationSection("playerQuests");
            if (playerQuests != null) {
                for (String uuidStr : playerQuests.getKeys(false)) {
                    List<Quest> quests = playerQuests.getObject(uuidStr, List.class);
                    UUID uuid = UUID.fromString(uuidStr);
                    if (!this.quests.containsKey(uuid))
                        this.quests.put(uuid, new ArrayList<>());
                    this.quests.get(uuid).addAll(quests);
                }
            }
        } catch(Exception ex) {
            logger.warning("Failed to load activeQuests.yml: " + ex);
        }
    }

    @Override
    public void onDisable() {
        try {
            Map<String, List<Quest>> collected = this.quests
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                    entry -> entry.getKey().toString(),
                    Map.Entry::getValue
                ));
            activeQuests.set("playerQuests", collected);
            activeQuests.set("sharedQuests", sharedQuests);
            activeQuests.save(activeQuestsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent event) {
        this.tickQuests();
    }

    /** Checks all quests, removing expired ones and assigning new ones. */
    public void tickQuests() {
        sharedQuests.removeIf(quest -> quest == null || quest.expired());
        while (this.sharedQuests.size() < max_shared_quests) {
            Quest quest = new Quest(getRandomQuest(), Quest.QuestKind.SHARED);
            quest.sharedId = ThreadLocalRandom.current().nextLong();
            sharedQuests.add(quest);
        }
        while (this.sharedQuests.size() > max_shared_quests)
            sharedQuests.remove(sharedQuests.size() - 1);

        for (Player player : this.getServer().getOnlinePlayers()) {
            if (!this.quests.containsKey(player.getUniqueId()))
                this.quests.put(player.getUniqueId(), new ArrayList<>());

            List<Quest> quests = this.quests.get(player.getUniqueId());

            quests.removeIf(quest -> {
                // Never remove event quests, they need to be queried by staff
                if (quest.kind == Quest.QuestKind.EVENT)
                    return false;
                // Remove shared quests no longer in the global pool
                if (quest.kind == Quest.QuestKind.SHARED) {
                    if (sharedQuests.stream().noneMatch(v -> v.sharedId == quest.sharedId)) {
                        player.sendMessage("[QuestsForCash] Quest \"" + quest.questData.title + "\" expired.");
                        return true;
                    }
                }
                if (quest.expired()) {
                    if (quest.complete && quest.kind == Quest.QuestKind.STREAK) {
                        if (quest.streakExpired()) {
                            player.sendMessage("[QuestsForCash] Streak quest \"" + quest.questData.title + "\" expired.");
                            return true;
                        } else {
                            // If a streak quest is complete, renew it
                            player.sendMessage("[QuestsForCash] Quest \"" + quest.questData.title + "\" renewed!");
                            quest.created = System.currentTimeMillis();
                            quest.complete = false;
                            quest.streak += 1;
                            return false;
                        }
                    } else {
                        // Otherwise remove regular expired quests
                        player.sendMessage("[QuestsForCash] Quest \"" + quest.questData.title + "\" expired.");
                        return true;
                    }
                }
                return false;
            });

            boolean newQuests = false;

            // Assign missing shared quests
            for (Quest quest: sharedQuests) {
                if (quests.stream().noneMatch(v -> v.sharedId == quest.sharedId)) {
                    quests.add(quest.createCopy());
                    newQuests = true;
                }
            }

            // Assign random quests until count
            long count = quests.stream().filter(v -> (
                v.kind == Quest.QuestKind.REGULAR ||
                v.kind == Quest.QuestKind.STREAK
            )).count();
            for (long i = count; i < max_random_quests; i++) {
                Quest quest = new Quest(getRandomQuest(), Quest.QuestKind.REGULAR);
                if (Math.random() < quest.questData.streakChance)
                    quest.kind = Quest.QuestKind.STREAK;
                quests.add(quest);
                newQuests = true;
            }

            if (newQuests) {
                player.sendMessage("[QuestsForCash] You've got new quests!");
            }
        }
    }

    /** Picks a random weighted quest from the quest pool */
    public QuestData getRandomQuest() {
        // https://stackoverflow.com/a/6737362
        double totalWeight = 0.0;
        for (QuestData i : this.questData) {
            totalWeight += i.randomWeight;
        }

        int idx = 0;
        for (double r = Math.random() * totalWeight; idx < questData.size() - 1; ++idx) {
            r -= questData.get(idx).randomWeight;
            if (r <= 0.0) break;
        }
        return questData.get(idx);
    }
}
