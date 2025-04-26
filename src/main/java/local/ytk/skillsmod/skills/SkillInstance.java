package local.ytk.skillsmod.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class SkillInstance {
    // Get skill and player
    public static final Codec<SkillInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Skill.ID_CODEC.fieldOf("skill").forGetter(SkillInstance::getSkill),
            Codec.INT.fieldOf("level").forGetter(SkillInstance::getLevel),
            Codec.INT.fieldOf("xp").forGetter(SkillInstance::getXp)
    ).apply(instance, SkillInstance::new));
    public static final PacketCodec<RegistryByteBuf, SkillInstance> PACKET_CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, SkillInstance::getId, SkillManager::createInstance
    );
    
    public Skill skill;
    public int level;
    public int xp;
    
    public SkillInstance(Skill skill, int level, int xp) {
        this.skill = skill;
        this.level = level;
        this.xp = xp;
    }
    public SkillInstance(Skill skill) {
        this(skill, 0, 0);
    }
    
    public Skill getSkill() {
        return skill;
    }
    
    Identifier getId() {
        return skill.id;
    }
    
    public int getLevel() {
        return level;
    }
    public int getXp() {
        return xp;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    public void setXp(int xp) {
        this.xp = xp;
    }
    
    public void moveXp(int xp, PlayerEntity player) {
        addXp(xp, player);
        // Copied from PlayerEntity, with addScore removed
        player.experienceProgress = player.experienceProgress + (float)xp / player.getNextLevelExperience();
        player.totalExperience = MathHelper.clamp(player.totalExperience + xp, 0, Integer.MAX_VALUE);
        
        while (player.experienceProgress < 0.0F) {
            float f = player.experienceProgress * player.getNextLevelExperience();
            if (player.experienceLevel > 0) {
                player.addExperienceLevels(-1);
                player.experienceProgress = 1.0F + f / player.getNextLevelExperience();
            } else {
                player.addExperienceLevels(-1);
                player.experienceProgress = 0.0F;
            }
        }
    }
    public void addXp(int xp, @Nullable PlayerEntity player) {
        this.xp += xp;
        // `level - 1` is used because first level is 1 but levels list is 0 indexed
        while (this.xp >= skill.levels.get(level).xpRequired()) {
            this.xp -= skill.levels.get(level).xpRequired();
            level++;
            MinecraftServer server = player != null ? player.getServer() : null;
            if (level == 1) {
                if (server != null) server.sendMessage(Text.translatable("chat.type.skill.unlock", player.getName(), skill.id.toString()));
            } else if (level >= skill.maxLevel()) {
                level = skill.maxLevel();
                if (server != null) server.sendMessage(Text.translatable("chat.type.skill.max", player.getName(), skill.id.toString(), level));
                break;
            } else {
                if (server != null) server.sendMessage(Text.translatable("chat.type.skill.levelup", player.getName(), skill.id.toString(), level));
            }
        }
    }
    public void addLevels(int levels, @Nullable PlayerEntity player) {
        if (level >= skill.maxLevel()) return; // Already at max level - cannot level up
        level += levels;
        @Nullable MinecraftServer server = player != null ? player.getServer() : null;
        if (level == 1) {
            if (server != null) server.sendMessage(Text.translatable("chat.type.skill.unlock", player.getName(), skill.id.toString()));
        } else if (level >= skill.maxLevel()) {
            level = skill.maxLevel();
            if (server != null) server.sendMessage(Text.translatable("chat.type.skill.max", player.getName(), skill.id.toString(), level));
        } else {
            if (server != null) server.sendMessage(Text.translatable("chat.type.skill.levelup", player.getName(), skill.id.toString(), level));
        }
    }
    
    public int getXpToNextLevel() {
        if (level >= skill.maxLevel()) return 0; // Already at max level
        if (level == 0) return 0; // No XP required for level 0 - this is the starting level
        return skill.levels.get(level).xpRequired() - xp;
    }
    
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("skill", skill.id.toString());
        nbt.putInt("level", level);
        nbt.putInt("xp", xp);
        return nbt;
    }
}
