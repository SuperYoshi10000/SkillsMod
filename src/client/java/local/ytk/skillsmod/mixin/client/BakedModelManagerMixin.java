package local.ytk.skillsmod.mixin.client;

import local.ytk.skillsmod.client.SkillSpriteManager;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.atlas.Atlases;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.HashMap;
import java.util.Map;

@Mixin(BakedModelManager.class)
public abstract class BakedModelManagerMixin {
//    @ModifyConstant(method = "<clinit>", constant = @Constant)
//    private static Map<Identifier, Identifier> clinit(Map<Identifier, Identifier> LAYERS_TO_LOADERS) {
//        Map<Identifier, Identifier> map = new HashMap<>(Map.of(
//                TexturedRenderLayers.BANNER_PATTERNS_ATLAS_TEXTURE, Atlases.BANNER_PATTERNS,
//                TexturedRenderLayers.BEDS_ATLAS_TEXTURE, Atlases.BEDS,
//                TexturedRenderLayers.CHEST_ATLAS_TEXTURE, Atlases.CHESTS,
//                TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE, Atlases.SHIELD_PATTERNS,
//                TexturedRenderLayers.SIGNS_ATLAS_TEXTURE, Atlases.SIGNS,
//                TexturedRenderLayers.SHULKER_BOXES_ATLAS_TEXTURE, Atlases.SHULKER_BOXES,
//                TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE, Atlases.ARMOR_TRIMS,
//                TexturedRenderLayers.DECORATED_POT_ATLAS_TEXTURE, Atlases.DECORATED_POT,
//                SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Atlases.BLOCKS
//        ));
//        map.put(SkillSpriteManager.ID, SkillSpriteManager.ATLAS);
//        return map;
//    }
    //.
}
