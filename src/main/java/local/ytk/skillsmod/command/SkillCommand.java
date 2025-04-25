package local.ytk.skillsmod.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import local.ytk.skillsmod.skills.*;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.ParsedSelector;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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
                                        .executes(SkillCommand::getSkill)
                                        .then(literal("level").executes(SkillCommand::getSkillLevel))
                                        .then(literal("xp").executes(SkillCommand::getSkillXp)))))
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
//                    SkillList skillList = ((HasSkills) player).getSkills();
//                    String s = skillList.skills().values().stream().map(v -> v.skill.id + ": " + v.level + ", " + v.xp).reduce("", String::concat);
//                    context.getSource().sendFeedback(() -> Text.literal(s), false);
//                    return 1;
//                }))
        );
    }
    
    
    private static Text getName(PlayerEntity player) {
        // Get the name of the player
        return ParsedSelector.parse(player.getUuidAsString())
                .map(p -> Text.selector(p, Optional.empty()))
                .resultOrPartial()
                .orElse(Text.empty());
    }
    
    static int listAllSkills(CommandContext<ServerCommandSource> context) {
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
    static int listSkillsForPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
        Text message = Text.translatable("commands.skill.player", getName(player)).append("\n").append(items);
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    static int getSkill(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get a skill for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            skill = SkillManager.createInstance(id);
//            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
        }
        Text message = Text.translatable("commands.skill.player.value",
                getName(player),
                Text.translatable(skill.skill.key),
                Text.literal(String.valueOf(skill.level)),
                Text.literal(String.valueOf(skill.xp))
        );
        context.getSource().sendMessage(message);
        return SUCCESS;
    }
    static int getSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            skill = SkillManager.createInstance(id);
//            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
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
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            skill = SkillManager.createInstance(id);
//            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
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
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            skill = SkillManager.createInstance(id);
//            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
        }
        int level = IntegerArgumentType.getInteger(context, "levels");
        int xp = IntegerArgumentType.getInteger(context, "xp");
        skill.setLevel(level);
        skill.setXp(xp);
        ((HasSkills) player).setSkills(skillList);
        return SUCCESS;
    }
    static int setSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Set a skill levels for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            skill = SkillManager.createInstance(id);
//            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
        }
        int level = IntegerArgumentType.getInteger(context, "levels");
        skill.setLevel(level);
        ((HasSkills) player).setSkills(skillList);
        return SUCCESS;
    }
    
    static int setSkillXp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Set a skill xp for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            skill = SkillManager.createInstance(id);
//            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
        }
        int xp = IntegerArgumentType.getInteger(context, "xp");
        skill.setXp(xp);
        ((HasSkills) player).setSkills(skillList);
        return SUCCESS;
    }
    static int addSkillLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Add to a skill levels for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            skill = SkillManager.createInstance(id);
//            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", player.getDisplayName(), id.toString()));
        }
        int levels = IntegerArgumentType.getInteger(context, "levels");
        skill.addLevels(levels, player);
        ((HasSkills) player).setSkills(skillList);
        return SUCCESS;
    }
    
    static int addSkillXp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Add to a skill XP for a player
        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        SkillList skillList = ((HasSkills) player).getSkills();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "skill");
        SkillInstance skill = skillList.getSkillInstance(id);
        if (skill == null) {
            skill = SkillManager.createInstance(id);
//            context.getSource().sendError(Text.translatable("commands.skill.player.notfound", getName(player), id.toString()));
        }
        int xp = IntegerArgumentType.getInteger(context, "xp");
        skill.addXp(xp, player);
        ((HasSkills) player).setSkills(skillList);
        return SUCCESS;
    }
    
    public static class SkillSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
            System.out.println("Getting suggestions for skill command"); // debug
            // Provide suggestions for skills
            SkillManager.getSkills().stream()
                    .filter(skill -> skill.key.equals(builder.getRemaining()))
                    .peek(System.out::println) // debug
                    .forEach(skill -> builder.suggest(skill.key));
            System.out.println("Suggestions completed"); // debug
            return builder.buildFuture();
        }
        
    }
}
