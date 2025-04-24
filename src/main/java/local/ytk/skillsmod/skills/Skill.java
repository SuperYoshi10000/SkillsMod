package local.ytk.skillsmod.skills;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Skill {
    public static final IntStream DEFAULT_XP_REQUIRED = IntStream.range(0, 100).map(i -> 100 * i);
    
    public static final Codec<Skill> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(Skill::id),
//            Identifier.CODEC.fieldOf("attribute").forGetter(Skill::attribute),
            Codec.INT.fieldOf("base").forGetter(Skill::base),
            Codec.INT.fieldOf("max_level").forGetter(Skill::maxLevel),
            Codec.either(Level.CODEC, Level.CODEC.listOf()).fieldOf("levels").forGetter(s -> Either.right(s.levels())),
            Codec.BOOL.optionalFieldOf("stack_lower_levels", false).forGetter(Skill::stackLowerLevels),
            Codec.INT_STREAM.optionalFieldOf("xp_required").xmap(s -> s.map(IntStream::toArray).map(IntList::of).orElse(IntList.of()), l -> Optional.ofNullable(l).map(IntCollection::intStream)).forGetter(Skill::xpRequired)
    ).apply(instance, Skill::new));
    public static final Codec<Skill> ID_CODEC = Identifier.CODEC.xmap(SkillManager::getSkill, Skill::id);
    
    
    public final Identifier id;
    public final String key;
    
    //    public final Identifier attribute;
    public final int base; // Not currently used, but could be used for base value or something else
    public final int maxLevel;
    public final List<Level> levels;
    @Nullable
    // A single level that stacks, when all levels are the same - only works if 'stackLowerLevels' is true
    public final Level allLevels;
    public final boolean stackLowerLevels;
    // XP required for each level, if not provided, defaults to 0, 100, 200, ...
    // Only used when using 'allLevels'
    public final IntList xpRequired;
    
    public Skill(Identifier id, int base, int maxLevel, Either<Level, List<Level>> levels, boolean stackLowerLevels, @Nullable IntList xpRequired) {
        this.id = id;
        this.key = id.toTranslationKey("player_skills");
//        this.attribute = attribute;
        this.base = base;
        this.maxLevel = maxLevel;
        if (levels.left().isPresent()) {
            Level level = levels.left().get();
            this.levels = Collections.nCopies(maxLevel, level);
            this.allLevels = level;
            if (xpRequired != null && !xpRequired.isEmpty()) this.xpRequired = xpRequired;
            else this.xpRequired = IntList.of(IntStream.range(0, maxLevel).map(i -> level.xpRequired * (i + 1)).toArray());
            this.stackLowerLevels = true;
        } else if (levels.right().isPresent()) {
            this.levels = levels.right().get();
            this.allLevels = null;
            this.xpRequired = null;
            this.stackLowerLevels = stackLowerLevels;
        } else {
            throw new IllegalArgumentException("Either a single level or a list of levels must be provided");
        }
    }
    public Skill(Identifier id, int base, int maxLevel, List<Level> levels, boolean stackLowerLevels) {
        this.id = id;
        this.key = id.toTranslationKey("player_skills");
//        this.attribute = attribute;
        this.base = base;
        this.maxLevel = maxLevel;
        this.levels = levels;
        this.allLevels = null;
        this.stackLowerLevels = stackLowerLevels;
        this.xpRequired = null;
    }
    public Skill(Identifier id, int base, int maxLevel, Level allLevels, IntList xpRequired) {
        this.id = id;
        this.key = id.toTranslationKey("player_skills");
        this.base = base;
        this.maxLevel = maxLevel;
        this.levels = Collections.nCopies(maxLevel, allLevels);
        this.allLevels = allLevels;
        this.stackLowerLevels = true;
        this.xpRequired = xpRequired;
    }
    
    public static Stream<LinkedEntityAttributeModifier> mergeSimilar(Map.Entry<EntityAttribute, Collection<LinkedEntityAttributeModifier>> e) {
        EntityAttribute attribute = e.getKey();
        if (e.getValue().isEmpty()) return Stream.empty();
        if (e.getValue().size() == 1) return e.getValue().stream();
        double addValue = 0, multiplyBaseValue = 0, multiplyTotalValue = 1;
        for (LinkedEntityAttributeModifier modifier : e.getValue()) switch (modifier.operation()) {
            case ADD_VALUE -> addValue += modifier.value();
            case ADD_MULTIPLIED_BASE -> multiplyBaseValue += modifier.value();
            case ADD_MULTIPLIED_TOTAL -> multiplyTotalValue *= 1 + modifier.value();
            case null, default -> {}
        }
        multiplyTotalValue -= 1; // This will be added back automatically
        Identifier id_add = Identifier.of("skills", "stacked.add/" + e.getKey().getTranslationKey()),
                id_multiplyBase = Identifier.of("skills", "stacked.multiply_base/" + e.getKey().getTranslationKey()),
                id_multiplyTotal = Identifier.of("skills", "stacked.multiply_total/" + e.getKey().getTranslationKey());
        if (addValue == 0 && multiplyBaseValue == 0 && multiplyTotalValue == 0)
            return Stream.of(); // No modifiers
        if (multiplyBaseValue == 0 && multiplyTotalValue == 0)
            return Stream.of(new LinkedEntityAttributeModifier(attribute, id_add, addValue, EntityAttributeModifier.Operation.ADD_VALUE));
        if (addValue == 0 && multiplyTotalValue == 0)
            return Stream.of(new LinkedEntityAttributeModifier(attribute, id_multiplyBase, multiplyBaseValue, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (addValue == 0 && multiplyBaseValue == 0)
            return Stream.of(new LinkedEntityAttributeModifier(attribute, id_multiplyTotal, multiplyTotalValue, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (multiplyTotalValue == 0)
            return Stream.of(new LinkedEntityAttributeModifier(attribute, id_add, addValue, EntityAttributeModifier.Operation.ADD_VALUE),
                    new LinkedEntityAttributeModifier(attribute, id_multiplyBase, multiplyBaseValue, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (multiplyBaseValue == 0)
            return Stream.of(new LinkedEntityAttributeModifier(attribute, id_add, addValue, EntityAttributeModifier.Operation.ADD_VALUE),
                    new LinkedEntityAttributeModifier(attribute, id_multiplyTotal, multiplyTotalValue, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (addValue == 0)
            return Stream.of(new LinkedEntityAttributeModifier(attribute, id_multiplyBase, multiplyBaseValue, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    new LinkedEntityAttributeModifier(attribute, id_multiplyTotal, multiplyTotalValue, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return Stream.of(
                new LinkedEntityAttributeModifier(attribute, id_add, addValue, EntityAttributeModifier.Operation.ADD_VALUE),
                new LinkedEntityAttributeModifier(attribute, id_multiplyBase, multiplyBaseValue, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                new LinkedEntityAttributeModifier(attribute, id_multiplyTotal, multiplyTotalValue, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        );
    }
    
    public SkillInstance createInstance() {
        return new SkillInstance(this);
    }
    
    public Identifier id() {
        return id;
    }
    
    public int base() {
        return base;
    }
    public int maxLevel() {
        return maxLevel;
    }
    public List<Level> levels() {
        return levels;
    }
    public boolean stackLowerLevels() {
        return stackLowerLevels;
    }
    public IntList xpRequired() {
        return xpRequired;
    }
    
    public int xpRequired(int level) {
        if (xpRequired == null) {
            if (allLevels != null) return allLevels.xpRequired * level;
            return levels.get(level).xpRequired;
        } else {
            return xpRequired.getInt(level);
        }
    }
    public List<LinkedEntityAttributeModifier> getModifiers(int level) {
        if (allLevels != null) {
            return Collections.nCopies(level, allLevels).stream().flatMap(l -> l.modifiers.stream()).toList();
        } else if (stackLowerLevels) {
            return levels.subList(0, level).stream().flatMap(l -> l.modifiers.stream()).toList();
        } else {
            return levels.get(level - 1).modifiers;
        }
    }
    public List<LinkedEntityAttributeModifier> getOptimizedModifiers(int level) {
        if (allLevels != null) {
            return allLevels.modifiers.stream().map(m -> new LinkedEntityAttributeModifier(m.attribute(), m.id(), m.value() * level, m.operation())).toList();
        } else if (stackLowerLevels) {
            Multimap<EntityAttribute, LinkedEntityAttributeModifier> map = HashMultimap.create();
            levels.subList(0, level).forEach(l -> l.modifiers.forEach(m -> map.put(m.attribute(), m)));
            return map.asMap().entrySet().stream().flatMap(Skill::mergeSimilar).toList();
        } else {
            return levels.get(level - 1).modifiers; // No optimization needed
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        Skill other = (Skill) obj;
        return Objects.equals(this.id, other.id) &&
//                Objects.equals(this.attribute, other.attribute) &&
                this.base == other.base &&
                this.maxLevel == other.maxLevel &&
                Objects.equals(this.levels, other.levels) &&
                this.stackLowerLevels == other.stackLowerLevels;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, base, maxLevel, levels, stackLowerLevels);
    }
    
    @Override
    public String toString() {
        return "Skill[" +
                "id=" + id + ", " +
//                "attribute=" + attribute + ", " +
                "base=" + base + ", " +
                "maxLevel=" + maxLevel + ", " +
                "levels=" + levels + ", " +
                "stackLowerLevels=" + stackLowerLevels +']';
    }
    
    public record Level(int xpRequired, List<LinkedEntityAttributeModifier> modifiers) {
        public static final Codec<Level> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("xp").forGetter(Level::xpRequired),
                LinkedEntityAttributeModifier.CODEC.listOf().fieldOf("modifiers").forGetter(Level::modifiers)
        ).apply(instance, Level::new));
        
        @Override
        public String toString() {
            return xpRequired + ": " + modifiers;
        }
        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            Level other = (Level) obj;
            return this.xpRequired == other.xpRequired &&
                    Objects.equals(this.modifiers, other.modifiers);
        }
        @Override
        public int hashCode() {
            return Objects.hash(xpRequired, modifiers);
        }
    }
}
