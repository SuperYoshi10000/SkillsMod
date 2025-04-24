package local.ytk.skillsmod.client;

import local.ytk.skillsmod.client.gui.SkillsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SkillsModClient implements ClientModInitializer {
    KeyBinding openSkillsScreenKeyBinding;
    
//    DataRequestManager<?> dataRequestManager = new DataRequestManager<>((player, payload) -> ClientPlayNetworking.send(payload));
    
    @Override
    public void onInitializeClient() {
        openSkillsScreenKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skills.open_skills_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.skills.keybindings"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSkillsScreenKeyBinding.wasPressed()) {
                // Open the skills screen
                client.setScreen(SkillsScreen.open());
            }
        });
    }
}
