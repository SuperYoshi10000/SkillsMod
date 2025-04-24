package local.ytk.skillsmod.skills;

import com.mojang.serialization.Codec;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;

import java.util.Map;

public record SkillList(Map<Skill, SkillInstance> skills) {
    public static final Codec<Map<Skill, SkillInstance>> MAP_CODEC = Codec.unboundedMap(Skill.ID_CODEC, SkillInstance.CODEC);
    public static final PacketCodec<RegistryByteBuf, SkillList> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.NBT_COMPOUND, SkillList::toNbt, SkillList::fromNbt
    );
    public static final TrackedDataHandler<SkillList> TRACKED_DATA_HANDLER = TrackedDataHandler.create(PACKET_CODEC);
    public static final TrackedData<SkillList> SKILL_TRACKER = DataTracker.registerData(PlayerEntity.class, TRACKED_DATA_HANDLER);
    static {
        TrackedDataHandlerRegistry.register(TRACKED_DATA_HANDLER);
    }
    
    public SkillInstance getSkillInstance(Identifier id) {
        Skill skill = SkillManager.getSkill(id);
        if (skill == null) return null;
        return skills.computeIfAbsent(skill, SkillInstance::new);
    }
    
    public static SkillList fromNbt(NbtCompound nbt) {
        SkillList skillList = SkillManager.createSkillList();
        for (String key : nbt.getKeys()) {
            Skill skill = SkillManager.getSkill(Identifier.of(key));
            if (skill == null) continue;
            SkillInstance skillInstance = skillList.skills.computeIfAbsent(skill, SkillInstance::new);
            NbtCompound skillNbt = nbt.getCompoundOrEmpty(key);
            skillInstance.setLevel(skillNbt.getInt("level", 0));
            skillInstance.setXp(skillNbt.getInt("xp", 0));
        }
        return skillList;
    }
    
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        for (Map.Entry<Skill, SkillInstance> entry : skills.entrySet()) {
            nbt.put(entry.getKey().id().toString(), entry.getValue().toNbt());
        }
        return nbt;
    }
}
