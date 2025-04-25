package local.ytk.skillsmod.client.screen;

import local.ytk.skillsmod.client.SkillSpriteManager;

public interface HasSkillSpriteManager {
    default SkillSpriteManager getSkillSpriteManager() {
        return null;
    }
}
