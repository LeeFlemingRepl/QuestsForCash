package eelfloat.questsforcash;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

/**
 * An instance of a Quest assigned to a player or quest pool
 */
public class Quest implements ConfigurationSerializable {
    /** The underlying data containing the quest definition */
    public QuestData questData;

    /** What kind of quest this is */
    public QuestKind kind;

    /** If this quest has been completed */
    public boolean complete;
    /** When this quest was created, in milliseconds from the epoch */
    public long created;

    /** The shared ID for this quest. A new shared quest won't be assigned as long as this matches the last one.
     * Only for shared quests. */
    public long sharedId;

    /** How many times this quest has been streak completed.
     * Only for streak quests. */
    public int streak;

    public enum QuestKind {
        // Regular quests are randomly assigned from a pool of quests and are different per player.
        REGULAR,
        // Streak quests will renew themselves on expiration if completed, allowing them to be
        // completed multiple times with increasing rewards.
        STREAK,
        // Shared quests are shared between all players.
        SHARED,
        // Event quests are quests shared between all players and created using a command.
        EVENT
    }

    public Quest(QuestData questData, QuestKind kind) {
        this.questData = questData;
        this.complete = false;
        this.created = System.currentTimeMillis();
        this.kind = kind;
        this.sharedId = 0;
        this.streak = 0;
    }

    public boolean expired() {
        if (this.questData.expires == 0) return false;
        return System.currentTimeMillis() > created + questData.expires * 1000L;
    }

    /** @return the cost and rewards multiplier for this quest */
    public double multiplier() {
        if (this.kind == QuestKind.STREAK) {
            return Math.min(
                Math.pow(this.questData.streakMultiplier, this.streak),
                this.questData.streakMultiplierCap
            );
        }
        return 1;
    }

    /** @return the cash value reward for this quest */
    public double reward() {
        return this.questData.reward * this.multiplier();
    }

    /** Creates an uncompleted copy of this test */
    public Quest createCopy() {
        Quest quest = new Quest(this.serialize());
        quest.complete = false;
        return quest;
    }

    /** Serialization constructor */
    public Quest(Map<String, Object> data) {
        this.questData = new QuestData((Map<?, ?>) data.get("questData"));
        this.kind = QuestKind.valueOf((String) data.get("kind"));
        this.sharedId = ((Number) data.get("sharedId")).longValue();
        this.created = ((Number) data.get("created")).longValue();
        this.complete = (Boolean) data.get("complete");
        this.streak = ((Number) data.get("streak")).intValue();
    }

    @Override
    public Map<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("questData", this.questData.serialize());
        data.put("kind", this.kind.toString());
        data.put("sharedId", this.sharedId);
        data.put("created", this.created);
        data.put("complete", this.complete);
        data.put("streak", this.streak);
        return data;
    }

    @Override
    public String toString() {
        return String.format(
            "Quest{questData=%s, complete=%s, created=%d, kind=%s, sharedId=%d, streak=%d}",
            questData, complete, created, kind, sharedId, streak
        );
    }
}
