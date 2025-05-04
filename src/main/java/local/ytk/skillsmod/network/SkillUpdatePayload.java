package local.ytk.skillsmod.network;

import local.ytk.skillsmod.SkillsMod;
import local.ytk.skillsmod.skills.SkillInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public record SkillUpdatePayload(SkillInstance skillInstance, int addLevels, int addXp) implements CustomPayload {
    public static final SkillUpdatePayload EMPTY = new SkillUpdatePayload(null, 0, 0);
    
    public static final Identifier ID = SkillsMod.id("skill_update");
    public static final Id<SkillUpdatePayload> PAYLOAD_ID = new Id<>(ID);
    public static final PacketCodec<RegistryByteBuf, SkillUpdatePayload> PACKET_CODEC = PacketCodec.ofStatic(
            SkillUpdatePayload::write, SkillUpdatePayload::new
    );
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillUpdatePayload.class);
    
    public SkillUpdatePayload(RegistryByteBuf buf) {
        this(
                SkillInstance.fromNbt(Objects.requireNonNullElseGet(buf.readNbt(), NbtCompound::new)), // skillInstance
                buf.readInt(), // addLevels
                buf.readInt() // addXp
        );
        LOGGER.info("Reading payload{}", skillInstance.skill != null ? ": " + skillInstance.skill.id : "");
    }
    public static void write(RegistryByteBuf buf, SkillUpdatePayload payload) {
        NbtCompound nbt = new NbtCompound();
        SkillInstance skillInstance = payload.skillInstance;
        if (skillInstance != null) buf.writeNbt(skillInstance.toNbt());
        else buf.writeNbt(NbtEnd.INSTANCE);
        buf.writeInt(payload.addLevels);
        buf.writeInt(payload.addXp);
        LOGGER.info("Writing payload{}", skillInstance != null ? ": " + skillInstance.skill.id : "");
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
    
    public boolean isEmpty() {
        return skillInstance == null || skillInstance.skill == null;
    }
}
