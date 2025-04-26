package local.ytk.skillsmod.mixin;

import com.mojang.authlib.GameProfile;
import local.ytk.skillsmod.skills.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static local.ytk.skillsmod.skills.SkillList.SKILL_TRACKER;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements HasSkills {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Override
    public SkillList getSkills() {
        return dataTracker.get(SKILL_TRACKER);
    }
    @Override
    public void setSkills(SkillList skills) {
        dataTracker.set(SKILL_TRACKER, skills);
    }
    
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void clinit(CallbackInfo info) {
        // This is used to load the static field
        // Without this, it won't be initialized until the DataTracker has already been created, which will be too late
        @SuppressWarnings("unused")
        TrackedData<SkillList> _list = SKILL_TRACKER;
    }
    
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    public void initDataTracker(DataTracker.Builder builder, CallbackInfo info) {
        builder.add(SKILL_TRACKER, SkillData.getPlayerState((PlayerEntity) (Object) this).skills());
    }
    
    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo info) {
        // Add attributes for skills
        SkillList skillList = dataTracker.get(SKILL_TRACKER);
        for (SkillInstance skill : skillList.skills().values()) {
            if (skill.level == 0) continue; // No need to add attributes for level 0
            for (LinkedEntityAttributeModifier modifier : skill.skill.getOptimizedModifiers(Math.min(skill.level, skill.skill.maxLevel))) {
                EntityAttributeInstance attributeInstance = getAttributeInstance(modifier.attributeEntry());
                if (attributeInstance == null) attributeInstance = new EntityAttributeInstance(modifier.attributeEntry(), a -> {});
                attributeInstance.overwritePersistentModifier(modifier.toEntityAttributeModifier());
            }
        }
    }
    
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo info) {
        nbt.put("skills", dataTracker.get(SKILL_TRACKER).toNbt());
    }
    
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo info) {
        dataTracker.set(SKILL_TRACKER, SkillList.fromNbt(nbt.getCompound("skills").orElse(new NbtCompound())));
    }
    
    // TODO make skill data persistent
    @Inject(method = "onDeath", at = @At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;onDeath(Lnet/minecraft/entity/damage/DamageSource;)V"))
    public void onDeath(CallbackInfo info) {
        SkillData.savePlayerState((PlayerEntity) (Object) this);
    }
}
