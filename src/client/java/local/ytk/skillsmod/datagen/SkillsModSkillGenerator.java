package local.ytk.skillsmod.datagen;

import net.minecraft.GameVersion;
import net.minecraft.data.DataGenerator;

import java.nio.file.Path;

@Deprecated
public class SkillsModSkillGenerator extends DataGenerator {
    public SkillsModSkillGenerator(Path outputPath, GameVersion gameVersion, boolean ignoreCache) {
        super(outputPath, gameVersion, ignoreCache);
    }
}
