package local.ytk.skillsmod.client;

import local.ytk.skillsmod.SkillsMod;
import local.ytk.skillsmod.client.gui.SkillsScreen;
import local.ytk.skillsmod.network.SkillListPayload;
import local.ytk.skillsmod.skills.SkillInstance;
import local.ytk.skillsmod.skills.SkillList;
import local.ytk.skillsmod.skills.SkillManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.lwjgl.glfw.GLFW;

public class SkillsModClient implements ClientModInitializer {
    KeyBinding openSkillsScreenKeyBinding;
    static SkillSpriteManager skillSpriteManager;
    
//    DataRequestManager<?> dataRequestManager = new DataRequestManager<>((player, payload) -> ClientPlayNetworking.send(payload));
    
    @Override
    public void onInitializeClient() {
        openSkillsScreenKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skills.open_skills_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.skills.keybindings"
        ));
        
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
//            skillSpriteManager = new SkillSpriteManager(MinecraftClient.getInstance().getTextureManager());
//            ((ReloadableResourceManagerImpl) client.getResourceManager()).registerReloader(skillSpriteManager);
//        });
//.
        
        ClientPlayConnectionEvents.JOIN.register(SkillsModClient::playerJoin);
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSkillsScreenKeyBinding.wasPressed()) {
                // Open the skills screen
                client.setScreen(SkillsScreen.open());
            }
        });
        
        ClientPlayNetworking.registerGlobalReceiver(SkillListPayload.PAYLOAD_ID, this::handleSkillList);
    }
    
    private static void playerJoin(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        syncSkills();
    }
    
    private void handleSkillList(SkillListPayload payload, ClientPlayNetworking.Context context) {
        SkillList skillList = payload.skillList();
        ClientPlayerEntity player = context.player();
        switch (payload.type()) {
            case REQUEST -> ClientPlayNetworking.send(SkillListPayload.response(SkillManager.getSkills(player)));
            case MODIFY -> {
                SkillList playerSkillList = SkillManager.getSkills(player);
                playerSkillList.skills().putAll(skillList.skills());
                ClientPlayNetworking.send(SkillListPayload.confirmation(playerSkillList));
                SkillManager.updateSkills(player, playerSkillList);
            }
            case RESPOND -> SkillManager.setSkills(player, skillList);
            case CONFIRM -> {} // already correct, no need to do anything
            case REJECT -> SkillManager.getSkills(player).skills().putAll(skillList.skills());
        }
    }
    
    public static void syncSkills() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        SkillList skillList = SkillManager.getSkills(player);
        if (skillList == null) {
            skillList = SkillManager.createSkillList();
            SkillManager.setSkills(player, skillList);
        }
        ClientPlayNetworking.send(SkillListPayload.request(skillList));
    }
    public static void updateSkills(SkillInstance instance) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        SkillList skillList = SkillManager.createEmptySkillList();
        skillList.skills().put(instance.getSkill(), instance);
        ClientPlayNetworking.send(SkillListPayload.modification(skillList));
    }
}
