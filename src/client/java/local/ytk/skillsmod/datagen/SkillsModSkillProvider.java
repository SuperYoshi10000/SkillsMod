package local.ytk.skillsmod.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import local.ytk.skillsmod.SkillsMod;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SkillsModSkillProvider implements DataProvider {
    static final String PLAYER_SKILLS_PATH = "player_skills";
    final DataOutput output;
    final Path basePath;
    
    public SkillsModSkillProvider(DataOutput output) {
        this.output = output;
        this.basePath = output.resolvePath(DataOutput.OutputType.DATA_PACK).resolve(SkillsMod.MOD_ID).resolve(PLAYER_SKILLS_PATH);
    }
    
    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (RegistryEntry<EntityAttribute> entry : Registries.ATTRIBUTE.getIndexedEntries()) {
            Identifier id = entry.getKey()
                    .map(RegistryKey::getValue)
                    .orElse(Identifier.of("skills", "unknown"));
            JsonObject json = new JsonObject();
            json.addProperty("id", entry.getKey()
                    .map(RegistryKey::getValue)
                    .map(i -> Objects.equals(i.getNamespace(), "minecraft") ? "skills" : i.getNamespace())
                    .orElse("skills")
            );
            json.addProperty("base", entry.value().getDefaultValue());
            json.addProperty("max_level", 100);
            JsonObject levels = generateJson(id);
            json.add("levels", levels);
            CompletableFuture<?> future = DataProvider.writeToPath(writer, json, basePath.resolve(id.getPath() + ".json"));
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }
    
    private JsonObject generateJson(Identifier id) {
        JsonObject modifier = new JsonObject();
        modifier.addProperty("attribute", id.toString());
        modifier.addProperty("id", "skills:upgrade/" + id.toShortTranslationKey());
        modifier.addProperty("operation", "add_multiplied_base");
        modifier.addProperty("value", 0.02);
        JsonObject levels = new JsonObject();
        levels.addProperty("xp", 100);
        JsonArray modifiers = new JsonArray();
        modifiers.add(modifier);
        levels.add("modifiers", modifiers);
        return levels;
    }
    
    @Override
    public String getName() {
        return "SkillsModSkillProvider";
    }
    
    
}
