package local.ytk.skillsmod.network;

import local.ytk.skillsmod.SkillsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record RequestDataPayload(String id, List<String> keys) implements CustomPayload {
    public static final Identifier ID = SkillsMod.id("update_data");
    public static final CustomPayload.Id<RequestDataPayload> PAYLOAD_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<RegistryByteBuf, RequestDataPayload> PACKET_CODEC = PacketCodec.ofStatic(
            RequestDataPayload::write,
            RequestDataPayload::new
    );
    
    public RequestDataPayload(RegistryByteBuf buf) {
        this(buf.readString(), buf.readList(PacketCodecs.STRING));
    }
    public static void write(RegistryByteBuf buf, RequestDataPayload payload) {
        buf.writeCollection(payload.keys, PacketCodecs.STRING);
    }
    
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return null;
    }
}
