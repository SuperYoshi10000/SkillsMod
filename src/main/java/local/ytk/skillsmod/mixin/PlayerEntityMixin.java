package local.ytk.skillsmod.mixin;

import local.ytk.skillsmod.skills.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements HasSkills {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Override
    public SkillList getSkills() {
        return SkillData.getPlayerState((PlayerEntity) (Object) this).skillList();
    }
    @Override
    public void setSkills(SkillList skillList) {
        // Add attributes for skills
        if (skillList == null) skillList = getSkills();
        for (SkillInstance skill : skillList.skills().values()) {
            if (skill.level == 0) continue; // No need to add attributes for level 0
            for (LinkedEntityAttributeModifier modifier : skill.skill.getModifiers(Math.min(skill.level, skill.skill.maxLevel))) {
                EntityAttributeInstance attributeInstance = getAttributeInstance(modifier.attributeEntry());
                if (attributeInstance == null) attributeInstance = new EntityAttributeInstance(modifier.attributeEntry(), a -> {});
                attributeInstance.overwritePersistentModifier(modifier.toEntityAttributeModifier());
            }
        }
    }
    
//    @Inject(method = "initDataTracker", at = @At("TAIL"))
//    public void initDataTracker(DataTracker.Builder builder, CallbackInfo info) {
//        PlayerEntity self = (PlayerEntity) (Object) this;
//        SkillList skillList = SkillData.getPlayerState(self).skillList();
//        builder.add(SKILL_TRACKER, skillList);
//        SkillManager.setSkills(self, skillList);
//    }
    
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo info) {
        if (getWorld().isClient) return;
        nbt.put("skills", getSkills().toNbt());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo info) {
        if (getWorld().isClient) return;
        NbtCompound skills = nbt.getCompoundOrEmpty("skills");
        SkillList skillList = SkillList.fromNbt(skills);
        SkillManager.setSkills((PlayerEntity) (Object) this, skillList);
    }
    
}
