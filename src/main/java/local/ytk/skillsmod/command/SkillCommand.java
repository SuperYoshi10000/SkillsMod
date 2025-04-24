package local.ytk.skillsmod.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import local.ytk.skillsmod.skills.*;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SkillCommand {
    public static final int SUCCESS = Command.SINGLE_SUCCESS;
    public static final int FAILURE = 0;
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("skill")
                .then(literal("list")
                        .executes(SkillCommand::listAllSkills)
                        .then(literal("all").executes(SkillCommand::listAllSkills))
                        .then(literal("for")
                                .then(argument("player", EntityArgumentType.player()).executes(SkillCommand::listSkillsForPlayer))
                        )
                )
                .then(literal("get")
                        .then(argument("player", EntityArgumentType.player())
                                .executes(SkillCommand::listSkillsForPlayer)
                                .then(argument("skill", IdentifierArgumentType.identifier())
                                        .executes(SkillCommand::getSkill)
                                        .then(literal("level").executes(SkillCommand::getSkillLevel))
                                        .then(literal("xp").executes(SkillCommand::getSkillXp))
                                )
                        )
                )
                .then(literal("set")
                        .then(argument("players", EntityArgumentType.player())
                                .then(argument("skill", IdentifierArgumentType.identifier())
                                        .then(argument("levels", IntegerArgumentType.integer())
                                                .executes(SkillCommand::setSkillLevel)
                                                .then(argument("xp", IntegerArgumentType.integer()).executes(SkillCommand::setSkill))
                                        )
                                        .then(literal("level")
                                                .then(argument("levels", IntegerArgumentType.integer()).executes(SkillCommand::setSkillLevel))
                                        )
                                        .then(literal("xp")
                                                .then(argument("xp", IntegerArgumentType.integer()).executes(SkillCommand::setSkillXp))
                                        )
                                )
                        )
                )
                .then(literal("add")
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("skill", IdentifierArgumentType.identifier())
                                        .then(argument("levels", IntegerArgumentType.integer()).executes(SkillCommand::addSkillLevel))
                                        .then(literal("level")
                                                .then(argument("levels", IntegerArgumentType.integer()).executes(SkillCommand::addSkillLevel))
                                        )
                                        .then(literal("xp")
                                                .then(argument("xp", IntegerArgumentType.integer()).executes(SkillCommand::addSkillXp))
                                        )
                                )
                        )
                )
        );
    }
    
    public static int listAllSkills(CommandContext<ServerCommandSource> context) {
        // List all skillList
        Text message = SkillManager.getSkills().stream()
                .map(skill -> Text.translatable("commands.skill.item.all",
                        Text.translatable(skill.key),
                        Text.literal(String.valueOf(skill.maxLevel))
                ).append("\n"))
                .reduce(Text.empty(), MutableText::append);
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    public static int listSkillsForPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // List all skillList for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Text items = skillList.skills().values().stream()
                .map(s -> Text.translatable("commands.skill.item.player",
                        Text.translatable(s.skill.key),
                        Text.literal(String.valueOf(s.level)),
                        Text.literal(String.valueOf(s.xp))
                ).append("\n"))
                .reduce(Text.empty(), MutableText::append);
        Text message = Text.translatable("commands.skill.player", context.getSource().getDisplayName()).append("\n").append(items);
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    public static int getSkill(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get a skill for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
            return FAILURE;
        }
        Text message = Text.translatable("commands.skill.player.value",
                context.getSource().getDisplayName(),
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.level)),
                Text.literal(String.valueOf(skill.xp))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    public static int getSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
            return FAILURE;
        }
        Text message = Text.translatable("commands.skill.item.player.level",
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.level))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    public static int getSkillXp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
            return FAILURE;
        }
        Text message = Text.translatable("commands.skill.item.player.level_xp",
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.xp))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    
    public static int setSkill(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Set a skill for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
            return FAILURE;
        }
        int level = IntegerArgumentType.getInteger(context, "levels");
        int xp = IntegerArgumentType.getInteger(context, "xp");
        skill.setLevel(level);
        skill.setXp(xp);
        return SUCCESS;
    }
    public static int setSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Set a skill levels for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
            return FAILURE;
        }
        int level = IntegerArgumentType.getInteger(context, "levels");
        skill.setLevel(level);
        return SUCCESS;
    }
    public static int setSkillXp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Set a skill xp for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
            return FAILURE;
        }
        int xp = IntegerArgumentType.getInteger(context, "xp");
        skill.setXp(xp);
        return SUCCESS;
    }
    
    public static int addSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Add to a skill levels for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
            return FAILURE;
        }
        int levels = IntegerArgumentType.getInteger(context, "levels");
        skill.addLevels(levels, player);
        return SUCCESS;
    }
    public static int addSkillXp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Add to a skill XP for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
            return FAILURE;
        }
        int xp = IntegerArgumentType.getInteger(context, "xp");
        skill.addXp(xp, player);
        return SUCCESS;
    }
}
