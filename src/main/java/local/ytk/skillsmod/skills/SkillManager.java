package local.ytk.skillsmod.skills;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.IntList;
import local.ytk.skillsmod.SkillsMod;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

public class SkillManager implements SimpleSynchronousResourceReloadListener {
    public static final Gson GSON = new Gson();
    public static final Identifier ID = SkillsMod.id("skills");
    public static final SkillManager INSTANCE = new SkillManager();
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillManager.class);
    
    public final Map<Identifier, Skill> skills = new HashMap<>();
    
    public static boolean hasSkill(Identifier id) {
        return INSTANCE.skills.containsKey(id);
    }
    public static boolean hasSkill(Skill skill) {
        return INSTANCE.skills.containsValue(skill);
    }
    public static Skill getSkill(Identifier id) {
        return INSTANCE.skills.get(id);
    }
    public static Collection<Skill> getSkills() {
        return INSTANCE.skills.values();
    }
    public static SkillList createSkillList() {
        return new SkillList(SkillManager.getSkills()
                .stream()
                .map(SkillInstance::new)
                .collect(TreeMap::new, (m, i) -> m.put(i.skill, i), Map::putAll));
    }
    public static SkillList createEmptySkillList() {
        return new SkillList(new TreeMap<>(Comparator.nullsLast(Skill::compareTo))); // Use TreeMap to keep the order of skills consistent
    }
    
    public static SkillInstance createInstance(Identifier id) {
        return INSTANCE.skills.computeIfAbsent(id, s -> {
            LOGGER.error("Skill not found: {}", id);
            return new Skill(id, 0, 100, new Skill.Level(100, List.of(), List.of()), IntList.of(), id);
        }).createInstance();
    }
    public static SkillInstance createInstance(Identifier id, int level, int xp) {
        return INSTANCE.skills.computeIfAbsent(id, s -> {
            LOGGER.error("Skill not found: {} (level {}, {}xp)", id, level, xp);
            return new Skill(id, 0, 100, new Skill.Level(100, List.of(), List.of()), IntList.of(), id);
        }).createInstance(level, xp);
    }
    
    public static SkillList getSkills(PlayerEntity player) {
        return SkillData.getPlayerState(player).skillList();
    }
    public static void setSkills(PlayerEntity player, SkillList skillList) {
        getSkills(player).putAll(skillList);
        //updateSkills(player, skillList);
    }
    public static void updateSkills(PlayerEntity player, SkillList skillList) {
        // Add attributes for skillList
        setSkills(player, skillList);
        for (SkillInstance skill : skillList.skills().values()) {
            if (skill.level == 0) continue; // No need to add attributes for level 0
            for (LinkedEntityAttributeModifier modifier : skill.skill.getModifiers(Math.min(skill.level, skill.skill.maxLevel))) {
                EntityAttributeInstance attributeInstance = player.getAttributeInstance(modifier.attributeEntry());
                if (attributeInstance == null) attributeInstance = new EntityAttributeInstance(modifier.attributeEntry(), a -> {});
                attributeInstance.overwritePersistentModifier(modifier.toEntityAttributeModifier());
            }
        }
    }
    
    public static SkillList getSkills(PlayerEntity player, SkillData data) {
        return data.getLocalPlayerState(player).skillList();
    }
    public static void setSkills(PlayerEntity player, SkillList skillList, SkillData data) {
        getSkills(player, data).putAll(skillList);
        //updateSkills(player, skillList);
    }
    public static void updateSkills(PlayerEntity player, SkillList skillList, SkillData data) {
        // Add attributes for skillList
        setSkills(player, skillList, data);
        for (SkillInstance skill : skillList.skills().values()) {
            if (skill.level == 0) continue; // No need to add attributes for level 0
            for (LinkedEntityAttributeModifier modifier : skill.skill.getModifiers(Math.min(skill.level, skill.skill.maxLevel))) {
                EntityAttributeInstance attributeInstance = player.getAttributeInstance(modifier.attributeEntry());
                if (attributeInstance == null) attributeInstance = new EntityAttributeInstance(modifier.attributeEntry(), a -> {});
                attributeInstance.overwritePersistentModifier(modifier.toEntityAttributeModifier());
            }
        }
    }
    
    @Override
    public Identifier getFabricId() {
        return ID;
    }
    
    @Override
    public void reload(ResourceManager manager) {
        Map<Identifier, Resource> resources = manager.findResources("skill", path -> path.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier id = entry.getKey();
            Resource resource = entry.getValue();
            try (InputStream stream = resource.getInputStream()) {
                String content = new String(stream.readAllBytes());
                // Parse the JSON content and create a Skill object
                JsonObject json = GSON.fromJson(content, JsonObject.class);
                if (json.has("enabled") && !json.get("enabled").getAsBoolean()) continue;
                DataResult<Pair<Skill, JsonElement>> result = Skill.CODEC.decode(JsonOps.INSTANCE, json);
                Pair<Skill, JsonElement> pair = result.getPartialOrThrow(IllegalArgumentException::new);
                Skill skill = pair.getFirst();
                // Register the skill
                skills.put(skill.id, skill);
            } catch (Exception e) {
                // Ignore the error for now
                //throw new RuntimeException("Failed to load skill " + id, e);
                // TODO: Handle the error properly, e.g. log it or throw a custom exception
                LOGGER.error("e: ", e);
            }
        }
    }
}
