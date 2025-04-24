package local.ytk.skillsmod.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/// Like EntityAttributeModifier, but it remembers the attribute it modifies
public record LinkedEntityAttributeModifier(
        EntityAttribute attribute,
        Identifier id,
        double value,
        EntityAttributeModifier.Operation operation
) {
    public LinkedEntityAttributeModifier(RegistryEntry<EntityAttribute> attribute, Identifier id, double value, EntityAttributeModifier.Operation operation) {
        this(attribute.value(), id, value, operation);
    }
    public static final Codec<LinkedEntityAttributeModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityAttribute.CODEC.fieldOf("attribute").forGetter(LinkedEntityAttributeModifier::attributeEntry),
            Identifier.CODEC.fieldOf("id").forGetter(LinkedEntityAttributeModifier::id),
            Codec.DOUBLE.fieldOf("value").forGetter(LinkedEntityAttributeModifier::value),
            EntityAttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(LinkedEntityAttributeModifier::operation)
    ).apply(instance, LinkedEntityAttributeModifier::new));
    
    public RegistryEntry<EntityAttribute> attributeEntry() {
        return Registries.ATTRIBUTE.getEntry(attribute);
    }
    public EntityAttributeModifier toEntityAttributeModifier() {
        return new EntityAttributeModifier(id, value, operation);
    }
}
