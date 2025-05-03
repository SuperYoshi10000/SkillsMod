package local.ytk.skillsmod.network;

import local.ytk.skillsmod.SkillsMod;
import local.ytk.skillsmod.skills.SkillList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record SkillListSyncPayload(UUID playerUuid, SkillList skillList) implements CustomPayload {
    public static final Identifier ID = SkillsMod.id("skill_list_sync");
    public static final Id<SkillListSyncPayload> PAYLOAD_ID = new Id<>(ID);
    public static final PacketCodec<RegistryByteBuf, SkillListSyncPayload> PACKET_CODEC = PacketCodec.ofStatic(
            SkillListSyncPayload::write, SkillListSyncPayload::new
    );
    
    public SkillListSyncPayload(RegistryByteBuf buf) {
        this(buf.readUuid(), SkillList.fromNbt(buf.readNbt()));
        System.out.println("Reading payload for " + playerUuid);
    }
    public static void write(RegistryByteBuf buf, SkillListSyncPayload payload) {
        buf.writeUuid(payload.playerUuid);
        buf.writeNbt(payload.skillList.toNbt());
        System.out.println("Writing payload for " + payload.playerUuid);
    }
    
    public static CustomPayloadS2CPacket createPacket(PlayerEntity target, SkillList skillList) {
        return new CustomPayloadS2CPacket(new SkillListSyncPayload(target.getUuid(), skillList));
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
    
}
