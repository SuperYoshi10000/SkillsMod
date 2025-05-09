package local.ytk.skillsmod.client.gui;

import local.ytk.skillsmod.client.SkillsModClient;
import local.ytk.skillsmod.skills.SkillData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkillsScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillsScreen.class);
    protected SkillListWidget skillList;
    private final SkillData skillData;
    
    protected SkillsScreen(SkillData skillData) {
        super(Text.translatable("gui.skills.title"));
        SkillsModClient.syncSkills();
        this.skillData = skillData;
    }
    
    public static SkillsScreen open(SkillData skillData) {
        return new SkillsScreen(skillData);
    }
    
    @Override
    protected void init() {
        super.init();
        skillList = new SkillListWidget(client, this, Math.clamp(this.width, 300, 500), this.height, 20, 33, skillData);
        skillList.setX(width / 2 - skillList.getWidth() / 2);
        addDrawableChild(skillList);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        if (client == null) {
            // This should never happen, but just in case
            LOGGER.warn("Client is null, setting to instance");
            client = MinecraftClient.getInstance();
        }
        context.drawTextWithShadow(client.textRenderer, title, width / 2 - client.textRenderer.getWidth(title) / 2, 5, 0xFFFFFF);
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.renderInGameBackground(context);
    }
}
