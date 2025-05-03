package local.ytk.skillsmod.client.gui;

import local.ytk.skillsmod.client.SkillsModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class SkillsScreen extends Screen {
    public SkillListWidget skillList;
    
    protected SkillsScreen() {
        super(Text.translatable("gui.skills.title"));
        SkillsModClient.syncSkills();
    }
    
    public static SkillsScreen open() {
        return new SkillsScreen();
    }
    
    @Override
    protected void init() {
        super.init();
        skillList = new SkillListWidget(client, this, Math.clamp(this.width, 300, 500), this.height, 0, 33);
        skillList.setX(width / 2 - skillList.getWidth() / 2);
        addDrawableChild(skillList);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        if (client == null) {
            // This should never happen, but just in case
            client = MinecraftClient.getInstance();
        }
        context.drawTextWithShadow(client.textRenderer, title, width / 2 - client.textRenderer.getWidth(title) / 2, 10, 0xFFFFFF);
    }
}
