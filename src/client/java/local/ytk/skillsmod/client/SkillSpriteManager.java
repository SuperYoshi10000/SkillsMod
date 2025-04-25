package local.ytk.skillsmod.client;

import local.ytk.skillsmod.SkillsMod;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasHolder;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public class SkillSpriteManager extends SpriteAtlasHolder {
    public static final Identifier ATLAS = SkillsMod.id("skills");
    public static final Identifier ID = SkillsMod.id("textures/atlas/skills.png");
    
    public SkillSpriteManager(TextureManager textureManager) {
        super(textureManager, ID, ATLAS);
    }
    
    @Override
    public Sprite getSprite(Identifier iconId) {
        return super.getSprite(iconId);
    }
}
