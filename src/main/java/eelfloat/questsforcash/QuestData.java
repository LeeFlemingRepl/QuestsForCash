package eelfloat.questsforcash;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * QuestData contains static, read-only configuration data for quests
 */
public class QuestData {
    public enum ObjectiveType {
        Fetch
    }

    /** The unique ID for the quest data, used for commands */
    public final String id;
    /** The type of quest */
    public final ObjectiveType type;

    /** The chance this quest can streak (0-1) */
    public final double streakChance;
    /** How much each streak increases the item requirements and rewards */
    public final double streakMultiplier;
    /** The maximum streak multiplier */
    public final double streakMultiplierCap;

    /** The name of the quest */
    public final String title;
    /** The chance for this test to be picked relative to all the others */
    public final double randomWeight;
    /** The amount this quest pays out */
    public final double reward;
    /** How long it takes the quest to expire, in seconds. */
    public final int expires;
    /** The item to fetch. Only for fetch quests. */
    public final Material material;
    /** The number of items required. Only for fetch quests. */
    public final int quantity;

    /** Deserialization constructor.
     * @param data `serialize()`d data */
    public QuestData(Map<?, ?> data) {
        QuestsForCash.plugin.logger.info("Loading quest...");
        this.id = (String) data.get("id");
        QuestsForCash.plugin.logger.info("Loading quest " + this.id + "...");
        this.type = ObjectiveType.valueOf((String) data.get("type"));

        this.streakChance = ((Number) data.get("streakChance")).doubleValue();
        this.streakMultiplier = ((Number) data.get("streakMultiplier")).doubleValue();
        this.streakMultiplierCap = ((Number) data.get("streakMultiplierCap")).doubleValue();

        this.title = (String) data.get("title");
        this.randomWeight = ((Number) data.get("randomWeight")).doubleValue();
        this.reward = ((Number) data.get("reward")).doubleValue();
        this.expires = ((Number) data.get("expires")).intValue();
        this.material = Material.valueOf((String) data.get("material"));
        this.quantity = ((Number) data.get("quantity")).intValue();
    }

    public Map<String, Object> serialize() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("type", type.name());
        map.put("title", title);
        map.put("randomWeight", randomWeight);
        map.put("reward", reward);
        map.put("material", material.toString());
        map.put("quantity", quantity);
        map.put("expires", expires);
        map.put("streakChance", streakChance);
        map.put("streakMultiplier", streakMultiplier);
        map.put("streakMultiplierCap", streakMultiplierCap);
        return map;
    }

    @Override
    public String toString() {
        return String.format(
            "QuestData{id='%s', type=%s, title='%s', randomWeight=%s, reward=%s, expires=%d, material=%s, quantity=%d}",
            id, type, title, randomWeight, reward, expires, material, quantity
        );
    }
}
