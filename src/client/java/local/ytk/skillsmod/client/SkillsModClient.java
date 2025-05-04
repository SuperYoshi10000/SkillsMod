package local.ytk.skillsmod.client;

import local.ytk.skillsmod.client.gui.SkillsScreen;
import local.ytk.skillsmod.network.SkillListSyncPayload;
import local.ytk.skillsmod.network.SkillUpdatePayload;
import local.ytk.skillsmod.skills.SkillData;
import local.ytk.skillsmod.skills.SkillInstance;
import local.ytk.skillsmod.skills.SkillList;
import local.ytk.skillsmod.skills.SkillManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

public class SkillsModClient implements ClientModInitializer {
    KeyBinding openSkillsScreenKeyBinding;
    SkillData skillData;
    
//    DataRequestManager<?> dataRequestManager = new DataRequestManager<>((player, payload) -> ClientPlayNetworking.send(payload));
    
    @Override
    public void onInitializeClient() {
        openSkillsScreenKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skills.open_skills_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.skills.keybindings"
        ));
        skillData = new SkillData();
        
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
//            skillSpriteManager = new SkillSpriteManager(MinecraftClient.getInstance().getTextureManager());
//            ((ReloadableResourceManagerImpl) client.getResourceManager()).registerReloader(skillSpriteManager);
//        });
//.
        
        ClientPlayConnectionEvents.JOIN.register(SkillsModClient::playerJoin);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSkillsScreenKeyBinding.wasPressed()) {
                // Open the skills screen
                client.setScreen(SkillsScreen.open(skillData));
            }
        });
        
        ClientPlayNetworking.registerGlobalReceiver(SkillListSyncPayload.PAYLOAD_ID, this::handleSkillList);
    }
    
    private static void playerJoin(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        // Do nothing - this is handled in the server
    }
    
    private void handleSkillList(SkillListSyncPayload payload, ClientPlayNetworking.Context context) {
        SkillList skillList = payload.skillList();
        MinecraftClient client = context.client();
        ClientPlayerEntity player = context.player();
        
        ClientWorld world = client.world;
        if (world == null) return;
        PlayerEntity targetPlayer = world.getEntity(payload.playerUuid()) instanceof PlayerEntity pe ? pe : null;
        
        SkillManager.updateSkills(targetPlayer, skillList, skillData);
    }
    
    public static void syncSkills() {
        ClientPlayNetworking.send(SkillUpdatePayload.EMPTY);
    }
    public static void updateSkills(SkillInstance instance) {
        SkillUpdatePayload payload = new SkillUpdatePayload(instance, 1, 0);
        ClientPlayNetworking.send(payload);
    }
}
