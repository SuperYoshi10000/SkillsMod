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
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SkillManager implements SimpleSynchronousResourceReloadListener {
    public static final Gson GSON = new Gson();
    public static final Identifier ID = Identifier.of(SkillsMod.MOD_ID, "skill");
    public static final SkillManager INSTANCE = new SkillManager();
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillManager.class);
    
    public final Map<Identifier, Skill> skills = new HashMap<>();
    
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
                .collect(Collectors.toMap(SkillInstance::getSkill, v -> v)));
    }
    
    public static SkillInstance createInstance(Identifier id) {
        return INSTANCE.skills.computeIfAbsent(id, s -> {
            LOGGER.error("Skill not found: {}", id);
            return new Skill(id, 0, 100, new Skill.Level(100, List.of()), IntList.of());
        }).createInstance();
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
    
    public void loadSkills(LifecycledResourceManager manager) {
//        resourceManager.findAllResources("skills")
    }
}
