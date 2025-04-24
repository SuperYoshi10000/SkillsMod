package local.ytk.skillsmod;

import local.ytk.skillsmod.command.SkillCommand;
import local.ytk.skillsmod.skills.SkillManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.command.DataCommand;

public class SkillsMod implements ModInitializer {
    public static final String MOD_ID = "skills";
    
    final SkillManager skillManager = SkillManager.INSTANCE;
//    DataRequestManager<ServerPlayerEntity> dataRequestManager = new DataRequestManager<>(ServerPlayNetworking::send);

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(skillManager);
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> SkillCommand.register(dispatcher));
    }
}
