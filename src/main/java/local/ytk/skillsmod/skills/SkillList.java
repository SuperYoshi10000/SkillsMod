package local.ytk.skillsmod.skills;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Map;

public record SkillList(Map<Skill, SkillInstance> skills) {
    public SkillInstance getSkillInstance(Identifier id) {
        Skill skill = SkillManager.getSkill(id);
        if (skill == null) return null;
        return get(skill);
    }
    
    public static SkillList fromNbt(NbtCompound nbt) {
        SkillList skillList = SkillManager.createEmptySkillList();
        if (nbt == null) return skillList;
        for (String key : nbt.getKeys()) {
            Skill skill = SkillManager.getSkill(Identifier.of(key));
            if (skill == null) continue;
            SkillInstance skillInstance = skillList.get(skill);
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
    
    public SkillInstance replace(SkillInstance oldInstance, SkillInstance newInstance) {
        if (oldInstance.equals(newInstance)) return newInstance;
        if (!oldInstance.skill.id.equals(newInstance.skill.id)) return newInstance;
        return put(newInstance);
    }
    public SkillInstance put(SkillInstance instance) {
        return skills.put(instance.skill, instance);
    }
    public void putAll(SkillList skillList) {
        skills.putAll(skillList.skills);
    }
    public SkillInstance get(Skill skill) {
        if (skill == null) return null;
        return skills.computeIfAbsent(skill, SkillInstance::new);
    }
}
