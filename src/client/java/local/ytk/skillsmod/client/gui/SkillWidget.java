package local.ytk.skillsmod.client.gui;

import local.ytk.skillsmod.client.SkillsModClient;
import local.ytk.skillsmod.client.screen.HasSkillSpriteManager;
import local.ytk.skillsmod.skills.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class SkillWidget extends ButtonWidget {
    static final int LINE_HEIGHT = 10;
    static final int MARGIN_TOP = 5;
    static final int MARGIN_SIDE = 6;
    static final int ICON_SIZE = 18;
    static final int TEXT_OFFSET = 6;
    static final int TEXT_MAIN_COLOR = 0xffffff;
    static final int TEXT_ALT_COLOR = 0xbfbfbf;
    static final boolean SHOW_TOOLTIP = false;
    private final SkillsScreen screen;
    private final SkillInstance skillInstance;
    
    public SkillWidget(int width, int height, SkillInstance skillInstance, SkillsScreen screen) {
        // Text is empty because the skill text is rendered differently
        super(0, 0, width, height, Text.empty(), SkillWidget::press, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        this.width = width;
        this.height = height;
        this.screen = screen;
        this.skillInstance = skillInstance;
//        StatusEffectsDisplay
    }
    
    public Sprite getSkillIcon(Skill skill) {
        return ((HasSkillSpriteManager) MinecraftClient.getInstance())
                .getSkillSpriteManager()
                .getSprite(skill.iconId);
    }
    
    // TODO make button press work
    // Type must be SkillWidget to work - the argument is a ButtonWidget to allow it to be used as a button press action
    public static void press(ButtonWidget button) {
        if (!(button instanceof SkillWidget)) return;
        // Handle skill button press
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return; // Should never happen, but just in case
        SkillInstance skillInstance = ((SkillWidget) button).skillInstance;
        
        int level = skillInstance.level;
        int maxLevel = skillInstance.skill.maxLevel;
        int playerXp = player.totalExperience;
        int xpRequired = skillInstance.getXpToNextLevel();
        
        if (level >= maxLevel) return; // Already maxed out
        if (playerXp < xpRequired) return; // Not enough XP
        // Level up the skill
        skillInstance.spendXp(xpRequired, player);
        SkillsModClient.updateSkills(skillInstance);
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.renderWidget(context, mouseX, mouseY, deltaTicks);
        MinecraftClient client = MinecraftClient.getInstance();

        int currentLevel = skillInstance.level;
        int maxLevel = skillInstance.skill.maxLevel;
        int currentXp = skillInstance.xp;
        int xpRequired = skillInstance.skill.xpRequired(currentLevel + 1);
        
        Text nameText = Text.translatable(skillInstance.skill.key);
        Text levelText = Text.translatable("gui.skills.level", currentLevel, maxLevel, currentXp, xpRequired, Math.floorDiv(currentXp, xpRequired));

        //TODO: Fix text positioning
        int imageX = getX() + MARGIN_SIDE;
        int textX = imageX + ICON_SIZE + TEXT_OFFSET;
        int topLineY = getY() + MARGIN_TOP;
        int bottomLineY = topLineY + LINE_HEIGHT;

        context.drawTextWithShadow(screen.getTextRenderer(), nameText, textX, topLineY, TEXT_MAIN_COLOR);
        context.drawTextWithShadow(screen.getTextRenderer(), levelText, textX, bottomLineY, TEXT_ALT_COLOR);
        context.drawSpriteStretched(RenderLayer::getGuiTextured, getSkillIcon(skillInstance.skill), imageX, topLineY, ICON_SIZE, ICON_SIZE);

        if (SHOW_TOOLTIP) {
            List<LinkedEntityAttributeModifier> modifiers = skillInstance.skill.getModifiers(currentLevel);
            Text tooltipText = modifiers.stream()
                    .map(SkillWidget::formatModifier)
                    .reduce(Text.empty(), MutableText::append, MutableText::append);
            setTooltip(Tooltip.of(tooltipText));
        }
        
        assert client.player != null;
        active = client.player.totalExperience >= xpRequired; // Disable button if not enough XP
    }
    
    public static Text formatModifier(LinkedEntityAttributeModifier modifier) {
        return Text.translatable(modifier.attribute().getTranslationKey())
                .formatted(Formatting.ITALIC)
                .append("\n");
    }

}
