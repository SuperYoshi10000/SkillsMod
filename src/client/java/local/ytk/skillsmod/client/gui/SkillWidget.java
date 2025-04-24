package local.ytk.skillsmod.client.gui;

import local.ytk.skillsmod.skills.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SkillWidget extends PressableWidget {
    private final int width;
    private final int height;
    private final Text text;
    private final SkillInstance skillInstance;
    
    private final Tooltip tooltip;
    
    public SkillWidget(int x, int y, int width, int height, Text text, SkillInstance skillInstance) {
        super(x, y, width, height, text);
        this.width = width;
        this.height = height;
        this.text = text;
        this.skillInstance = skillInstance;
        
        List<LinkedEntityAttributeModifier> modifiers = skillInstance.skill.levels.get(skillInstance.level).modifiers();
        Text tooltipText = modifiers.stream()
                .map(SkillWidget::formatModifier)
                .reduce(Text.empty(), MutableText::append, MutableText::append);
        tooltip = Tooltip.of(tooltipText);
    }

    public void onPress() {
        // Handle skill button press
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return; // Should never happen, but just in case
        SkillList skills = ((HasSkills) player).getSkills();
        if (skills == null) skills = SkillManager.createSkillList();
        skills.skills().put(skillInstance.skill, skillInstance);
        ((HasSkills) player).setSkills(skills); // Update the player's skills on both client and server
    }
    
    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
    
    @Override
    public @Nullable Tooltip getTooltip() {
        return tooltip;
    }
    
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.renderWidget(context, mouseX, mouseY, deltaTicks);
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        //TODO: Add skill icon and level display
        //TODO: Fix text positioning
        int textX = getX() + (width - 100);
        int textY = getY() + 5;
        int currentLevel = skillInstance.level;
        int maxLevel = skillInstance.skill.maxLevel;
        int currentXp = skillInstance.xp;
        int xpRequired = skillInstance.skill.xpRequired(currentLevel + 1);
        Text levelText = Text.translatable("gui.skills.level", currentLevel, maxLevel, currentXp, xpRequired, Math.floorDiv(currentXp, xpRequired));
        context.drawTextWithShadow(minecraftClient.textRenderer, levelText, textX, textY, 0xFFFFFF);
        
        active = currentXp >= xpRequired; // Disable button if not enough XP
    }
    
    public static Text formatModifier(LinkedEntityAttributeModifier modifier) {
        return Text.translatable(modifier.attribute().getTranslationKey())
                .formatted(Formatting.ITALIC)
                .append("\n");
    }
}
