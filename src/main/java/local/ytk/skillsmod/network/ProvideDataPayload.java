package local.ytk.skillsmod.network;

import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import local.ytk.skillsmod.SkillsMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record ProvideDataPayload<T>(String id, NbtCompound data) implements CustomPayload {
    public static final Identifier ID = SkillsMod.id("update_data");
    public static final CustomPayload.Id<ProvideDataPayload<?>> PAYLOAD_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<RegistryByteBuf, ProvideDataPayload<?>> PACKET_CODEC = PacketCodec.ofStatic(
            ProvideDataPayload::write, ProvideDataPayload::new
    );
    
    public ProvideDataPayload(RegistryByteBuf buf) {
        this(buf.readString(), buf.readNbt());
    }
    public static void write(RegistryByteBuf buf, ProvideDataPayload<?> payload) {
        buf.writeString(payload.id);
        buf.writeNbt(payload.data);
    }
    
    public boolean has(String key) {
        return data.contains(key);
    }
    public <T> T get(String key, Codec<T> codec) {
        if (!data.contains(key)) return null;
        return codec.parse(NbtOps.INSTANCE, data.get(key)).getPartialOrThrow(DecoderException::new);
    }
    
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
    
    public static class Template<T> extends HashMap<String, Codec<T>> {
        public ProvideDataPayload<T> encode(String id, Map<String, T> data) {
            NbtCompound nbt = new NbtCompound();
            for (Map.Entry<String, T> entry : data.entrySet()) {
                String key = entry.getKey();
                Codec<T> codec = this.get(key);
                if (codec == null) throw new EncoderException("No codec found for key: " + key);
                T value = entry.getValue();
                nbt.put(key, codec.encodeStart(NbtOps.INSTANCE, value).getPartialOrThrow(EncoderException::new));
            }
            return new ProvideDataPayload<>(id, nbt);
        }
        
        public Map<String, T> decode(ProvideDataPayload<T> payload) {
            Map<String, T> result = new HashMap<>();
            for (Map.Entry<String, Codec<T>> entry : entrySet()) {
                String key = entry.getKey();
                Codec<T> codec = entry.getValue();
                if (!payload.data.contains(key)) continue;
                T value = payload.get(key, codec);
                result.put(key, value);
            }
            return result;
        }
    }
}
