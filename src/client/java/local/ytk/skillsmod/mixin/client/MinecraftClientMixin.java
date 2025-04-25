package local.ytk.skillsmod.mixin.client;

import local.ytk.skillsmod.client.SkillSpriteManager;
import local.ytk.skillsmod.client.screen.HasSkillSpriteManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements HasSkillSpriteManager {
    @Shadow @Final private ReloadableResourceManagerImpl resourceManager;
    @Shadow @Final private TextureManager textureManager;
    
    @Unique
    private SkillSpriteManager skillSpriteManager;
    
    @Inject(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resource/ResourceReloadLogger;reload(Lnet/minecraft/client/resource/ResourceReloadLogger$ReloadReason;Ljava/util/List;)V")
    )
    public void init(CallbackInfo info) {
        System.out.println("Initializing SkillSpriteManager"); // debug
        skillSpriteManager = new SkillSpriteManager(textureManager);
        resourceManager.registerReloader(skillSpriteManager);
        System.out.println("SkillSpriteManager initialized"); // debug
        
    }
    
    @Override
    @Unique
    public SkillSpriteManager getSkillSpriteManager() {
        return skillSpriteManager;
    }
}
