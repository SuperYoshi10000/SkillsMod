package local.ytk.skillsmod.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import local.ytk.skillsmod.SkillsMod;
import local.ytk.skillsmod.skills.*;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SkillCommand {
    public static final int SUCCESS = Command.SINGLE_SUCCESS;
    public static final int FAILURE = 0;
    public static final SkillSuggestionProvider SKILLS_SUGGESTIONS = new SkillSuggestionProvider();
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("skill")
                .requires(source -> source.hasPermissionLevel(2)) // Cheats enabled, creative mode, or operator
                .then(literal("list")
                        .executes(SkillCommand::listAllSkills)
                        .then(literal("all").executes(SkillCommand::listAllSkills))
                        .then(literal("for")
                                .then(argument("player", EntityArgumentType.player()).executes(SkillCommand::listSkillsForPlayer))))
                .then(literal("get")
                        .then(argument("player", EntityArgumentType.player())
                                .executes(SkillCommand::listSkillsForPlayer)
                                .then(argument("skill", IdentifierArgumentType.identifier())
                                        .suggests(SkillCommand.SKILLS_SUGGESTIONS)
                                        .executes(SkillCommand::getSkill))))
                                        //.then(literal("level").executes(SkillCommand::getSkillLevel))
                                        //.then(literal("xp").executes(SkillCommand::getSkillXp))
                .then(literal("set")
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("skill", IdentifierArgumentType.identifier())
                                        .suggests(SkillCommand.SKILLS_SUGGESTIONS)
                                        .then(argument("levels", IntegerArgumentType.integer())
                                                .executes(SkillCommand::setSkillLevel)
                                                .then(argument("xp", IntegerArgumentType.integer()).executes(SkillCommand::setSkill)))
                                        .then(literal("level")
                                                .then(argument("levels", IntegerArgumentType.integer()).executes(SkillCommand::setSkillLevel)))
                                        .then(literal("xp")
                                                .then(argument("xp", IntegerArgumentType.integer()).executes(SkillCommand::setSkillXp))))))
                .then(literal("add")
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("skill", IdentifierArgumentType.identifier())
                                        .suggests(SkillCommand.SKILLS_SUGGESTIONS)
                                        .then(argument("levels", IntegerArgumentType.integer()).executes(SkillCommand::addSkillLevel))
                                        .then(literal("level")
                                                .then(argument("levels", IntegerArgumentType.integer()).executes(SkillCommand::addSkillLevel)))
                                        .then(literal("xp")
                                                .then(argument("xp", IntegerArgumentType.integer()).executes(SkillCommand::addSkillXp))))))
//                .then(literal("test_list_skills").executes(context -> {
//                    PlayerEntity player = (PlayerEntity) context.getSource().getEntity();
//                    assert player != null;
//                    SkillList skillList = SkillManager.getSkills(player);
//                    String s = skillList.skillList().values().stream().map(v -> v.skill.id + ": " + v.level + ", " + v.xp).reduce("", String::concat);
//                    context.getSource().sendFeedback(() -> Text.literal(s), false);
//                    return 1;
//                }))
        );
    }
    
    
    static int listAllSkills(CommandContext<ServerCommandSource> context) {
        // List all skills
        Collection<Skill> skills = SkillManager.getSkills();
        
        Text message = skills.isEmpty() ? Text.translatable("commands.skill.none") : skills.stream()
                .map(skill -> Text.literal("\n").append(Text.translatable("commands.skill.item.all",
                                Text.translatable(skill.key),
                                Text.literal(String.valueOf(skill.maxLevel))
                        )))
                .reduce(Text.translatable("commands.skill.all"), MutableText::append);
        
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    static int listSkillsForPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // List all skills for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = SkillManager.getSkills(player);
        Text items = skillList.isEmpty() ? Text.translatable("commands.skill.player.none", player.getDisplayName()) : skillList.skills().values().stream()
                .map(s -> Text.literal("\n").append(Text.translatable("commands.skill.item.player",
                                Text.translatable(s.skill.key),
                                Text.literal(String.valueOf(s.level)),
                                Text.literal(String.valueOf(s.xp))
                        )))
                .reduce(Text.empty(), MutableText::append);
        // Get the name of the player
        Text message = Text.translatable("commands.skill.player", player.getDisplayName()).append(items);
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    static int getSkill(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get a skill for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = SkillManager.getSkills(player);
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            if (!SkillManager.hasSkill(id)) {
                context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
                return FAILURE;
            } else skill = SkillManager.createInstance(id);
        }
        // Get the name of the player
        Text message = Text.translatable("commands.skill.player.value",
                player.getDisplayName(),
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.level)),
                Text.literal(String.valueOf(skill.xp))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    static int getSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = SkillManager.getSkills(player);
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            if (!SkillManager.hasSkill(id)) {
                context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
                return FAILURE;
            } else skill = SkillManager.createInstance(id);
        }
        Text message = Text.translatable("commands.skill.item.player.level",
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.level))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    
    static int getSkillXp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = SkillManager.getSkills(player);
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            if (!SkillManager.hasSkill(id)) {
                context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
                return FAILURE;
            } else skill = SkillManager.createInstance(id);
        }
        Text message = Text.translatable("commands.skill.item.player.level_xp",
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.xp))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    static int setSkill(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Set a skill for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = SkillManager.getSkills(player);
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            if (!SkillManager.hasSkill(id)) {
                context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
                return FAILURE;
            } else skill = SkillManager.createInstance(id);
        }
        int level = IntegerArgumentType.getInteger(context, "levels");
        int xp = IntegerArgumentType.getInteger(context, "xp");
        skill.setLevel(level);
        skill.setXp(xp);
        SkillManager.updateSkills(player, skillList);
        // Get the name of the player
        Text message = Text.translatable("commands.skill.player.set",
                player.getDisplayName(),
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.level)),
                Text.literal(String.valueOf(skill.xp))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    static int setSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Set a skill levels for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = SkillManager.getSkills(player);
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            if (!SkillManager.hasSkill(id)) {
                context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
                return FAILURE;
            } else skill = SkillManager.createInstance(id);
        }
        int level = IntegerArgumentType.getInteger(context, "levels");
        skill.setLevel(level);
        SkillManager.updateSkills(player, skillList);
        // Get the name of the player
        Text message = Text.translatable("commands.skill.player.set",
                player.getDisplayName(),
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.level)),
                Text.literal(String.valueOf(skill.xp))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    
    static int setSkillXp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Set a skill xp for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = SkillManager.getSkills(player);
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            if (!SkillManager.hasSkill(id)) {
                context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
                return FAILURE;
            } else skill = SkillManager.createInstance(id);
        }
        int xp = IntegerArgumentType.getInteger(context, "xp");
        skill.setXp(xp);
        SkillManager.updateSkills(player, skillList);
        // Get the name of the player
        Text message = Text.translatable("commands.skill.player.set",
                player.getDisplayName(),
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.level)),
                Text.literal(String.valueOf(skill.xp))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    static int addSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Add to a skill levels for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = SkillManager.getSkills(player);
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            if (!SkillManager.hasSkill(id)) {
                context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
                return FAILURE;
            } else skill = SkillManager.createInstance(id);
        }
        int levels = IntegerArgumentType.getInteger(context, "levels");
        skill.addLevels(levels, player);
        SkillManager.updateSkills(player, skillList);
        // Get the name of the player
        Text message = Text.translatable("commands.skill.player.set",
                player.getDisplayName(),
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.level)),
                Text.literal(String.valueOf(skill.xp))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    
    static int addSkillXp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Add to a skill XP for a player
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = SkillManager.getSkills(player);
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            if (!SkillManager.hasSkill(id)) {
                context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
                return FAILURE;
            } else skill = SkillManager.createInstance(id);
        }
        int xp = IntegerArgumentType.getInteger(context, "xp");
        skill.addXp(xp, player);
        SkillsMod.syncSkills(context.getSource().getServer(), player, skillList);
        // Get the name of the player
        Text message = Text.translatable("commands.skill.player.set",
                player.getDisplayName(),
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.level)),
                Text.literal(String.valueOf(skill.xp))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    
    public static class SkillSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
            // Provide suggestions for skills
            SkillManager.getSkills().stream()
                    .filter(skill -> skill.id.toString().endsWith(builder.getRemaining()))
                    .forEach(skill -> builder.suggest(skill.id.toString()));
            return builder.buildFuture();
        }
    }
}
