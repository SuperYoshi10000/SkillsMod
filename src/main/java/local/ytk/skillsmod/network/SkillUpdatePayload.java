package local.ytk.skillsmod.network;

import io.netty.handler.codec.DecoderException;
import local.ytk.skillsmod.SkillsMod;
import local.ytk.skillsmod.skills.SkillInstance;
import local.ytk.skillsmod.skills.SkillList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;

public record SkillUpdatePayload(SkillInstance skillInstance, int addLevels, int addXp) implements CustomPayload {
    public static final Identifier ID = SkillsMod.id("skill_list");
    public static final Id<SkillUpdatePayload> PAYLOAD_ID = new Id<>(ID);
    public static final PacketCodec<RegistryByteBuf, SkillUpdatePayload> PACKET_CODEC = PacketCodec.ofStatic(
            SkillUpdatePayload::write, SkillUpdatePayload::new
    );
    
    public SkillUpdatePayload(RegistryByteBuf buf) {
        this(SkillInstance.PACKET_CODEC.decode(buf), buf.readInt(), buf.readInt());
        System.out.println("Reading payload: " + skillInstance.skill.id);
    }
    public static void write(RegistryByteBuf buf, SkillUpdatePayload payload) {
        buf.writeNbt(payload.skillInstance.toNbt());
        buf.writeByte(payload.addLevels);
        buf.writeByte(payload.addXp);
        System.out.println("Writing payload: " + payload.skillInstance.skill.id);
    }
    
    public CustomPayloadS2CPacket toPacket() {
        return new CustomPayloadS2CPacket(this);
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
