package local.ytk.skillsmod.network;

import io.netty.handler.codec.DecoderException;
import local.ytk.skillsmod.SkillsMod;
import local.ytk.skillsmod.skills.SkillList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;

public record SkillListPayload(SkillList skillList, Type type) implements CustomPayload {
    public static final Identifier ID = SkillsMod.id("skill_list");
    public static final Id<SkillListPayload> PAYLOAD_ID = new Id<>(ID);
    public static final PacketCodec<RegistryByteBuf, SkillListPayload> PACKET_CODEC = PacketCodec.ofStatic(
            SkillListPayload::write, SkillListPayload::new
    );
    
    public SkillListPayload(RegistryByteBuf buf) {
        this(SkillList.fromNbt(buf.readNbt()), Type.decode(buf.readByte()));
        System.out.println("Reading payload: " + skillList.toNbt());
    }
    public static void write(RegistryByteBuf buf, SkillListPayload payload) {
        buf.writeNbt(payload.skillList.toNbt());
        buf.writeByte(payload.type.encode());
        System.out.println("Writing payload: " + payload.skillList.toNbt());
    }
    
    public CustomPayloadS2CPacket toPacket() {
        return new CustomPayloadS2CPacket(this);
    }
    
    public static SkillListPayload request(SkillList skillList) {
        return new SkillListPayload(skillList, Type.REQUEST);
    }
    public static SkillListPayload modification(SkillList skillList) {
        return new SkillListPayload(skillList, Type.MODIFY);
    }
    public static SkillListPayload response(SkillList skillList) {
        return new SkillListPayload(skillList, Type.RESPOND);
    }
    public static SkillListPayload confirmation(SkillList skillList) {
        return new SkillListPayload(skillList, Type.CONFIRM);
    }
    public static SkillListPayload rejection(SkillList skillList) {
        return new SkillListPayload(skillList, Type.REJECT);
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
    
    
    public boolean isRequest() {
        return type == Type.REQUEST || type == Type.MODIFY;
    }
    public boolean isResponse() {
        return type == Type.RESPOND || type == Type.CONFIRM || type == Type.REJECT;
    }
    public boolean isSuccess() {
        return type == Type.RESPOND || type == Type.CONFIRM;
    }
    public boolean isSyncMode() {
        return type == Type.REQUEST || type == Type.RESPOND;
    }
    public boolean isModifyMode() {
        return type == Type.MODIFY || type == Type.CONFIRM || type == Type.REJECT;
    }
    
    public enum Type {
        REQUEST,
        RESPOND,
        MODIFY,
        CONFIRM,
        REJECT,;
        
        public byte encode() {
            return (byte) ordinal();
        }
        public static Type decode(byte b) {
            if (b < 0 || b >= values().length) {
                throw new DecoderException("Invalid type: " + b);
            }
            return values()[b];
        }
    }
}
