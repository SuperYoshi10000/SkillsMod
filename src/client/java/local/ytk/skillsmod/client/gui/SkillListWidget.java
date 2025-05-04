package local.ytk.skillsmod.client.gui;

import local.ytk.skillsmod.skills.HasSkills;
import local.ytk.skillsmod.skills.SkillInstance;
import local.ytk.skillsmod.skills.SkillList;
import local.ytk.skillsmod.skills.SkillManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public class SkillListWidget extends ElementListWidget<SkillListWidget.Entry> {
    public static final int HEADER_HEIGHT = 24;
    private final PlayerEntity player;
    private final SkillList playerSkillList;
    
    public SkillListWidget(MinecraftClient minecraftClient, SkillsScreen screen, int width, int height, int y, int itemHeight) {
        super(minecraftClient, width, height, y, itemHeight, HEADER_HEIGHT);
        player = minecraftClient.player;
        if (player == null) throw new IllegalStateException("Player is null");
        playerSkillList = SkillManager.getSkills(player);
        
        SkillManager.INSTANCE.skills.forEach((id, skill) -> {
            Text skillName = Text.translatable(id.toTranslationKey("skill"));
            SkillInstance skillInstance = playerSkillList.skills().computeIfAbsent(skill, SkillInstance::new);
            SkillWidget skillWidget = new SkillWidget(width, itemHeight, skillInstance, screen);
            addEntry(new Entry(skillWidget));
        });
    }
    
    
    
    public static class Entry extends ElementListWidget.Entry<Entry> {
        final SkillWidget skillWidget;
        final List<SkillWidget> skillWidgetList;
        public Entry(SkillWidget skillWidget) {
            this.skillWidget = skillWidget;
            this.skillWidgetList = List.of(skillWidget);
        }
        
        @Override
        public List<? extends Selectable> selectableChildren() {
            return skillWidgetList;
        }
        
        @Override
        public List<? extends Element> children() {
            return skillWidgetList;
        }
        
        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickProgress) {
            skillWidget.setX(x);
            skillWidget.setY(y);
            skillWidget.setWidth(entryWidth);
            skillWidget.setHeight(entryHeight);
            skillWidget.render(context, mouseX, mouseY, tickProgress);
        }
    }
}
