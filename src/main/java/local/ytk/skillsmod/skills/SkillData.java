package local.ytk.skillsmod.skills;

import com.mojang.serialization.Codec;
import local.ytk.skillsmod.SkillsMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

public class SkillData extends PersistentState {
    public static final Codec<SkillData> CODEC = NbtCompound.CODEC.xmap(SkillData::fromNbt, SkillData::toNbt);
    private static final PersistentStateType<SkillData> TYPE = new PersistentStateType<>(
            SkillsMod.MOD_ID, SkillData::new, CODEC, null
    );
    
    public final HashMap<UUID, PlayerSkillData> players = new HashMap<>();
    
    public SkillList getPlayerSkills(UUID uuid) {
        return players.computeIfAbsent(uuid, PlayerSkillData::new).skillList;
    }
    
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound skillTag = new NbtCompound();
        for (UUID uuid : players.keySet()) {
            NbtCompound playerTag = new NbtCompound();
            playerTag.put("skills", getPlayerSkills(uuid).toNbt());
            System.out.println("Writing skills: " + playerTag);
            skillTag.put(uuid == null ? "PLAYER" : uuid.toString(), playerTag);
        }
        nbt.put("players", skillTag);
        return nbt;
    }
    public NbtCompound toNbt() {
        return writeNbt(new NbtCompound());
    }
    
    public static SkillData fromNbt(NbtCompound nbt) {
        SkillData state = new SkillData();
        NbtCompound skillTag = nbt.getCompound("players").orElseGet(NbtCompound::new);
        for (String key : skillTag.getKeys()) {
            UUID uuid = key.equals("PLAYER") ? null : UUID.fromString(key);
            NbtCompound playerTag = nbt.getCompound(key).orElseGet(NbtCompound::new);
            PlayerSkillData playerState = new PlayerSkillData(SkillList.fromNbt(playerTag.getCompound("skills").orElseGet(NbtCompound::new)));
            state.players.put(uuid, playerState);
        }
        return state;
    }
    
    public static SkillData getServerState(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        assert world != null;
        SkillData state = world.getPersistentStateManager().getOrCreate(TYPE);
        state.markDirty();
        return state;
    }
    public static PlayerSkillData getPlayerState(PlayerEntity player) {
        assert player != null;
        MinecraftServer server = player.getServer();
        if (server == null) {
            // Client side
            return new PlayerSkillData(player.getUuid());
        }
        SkillData state = getServerState(server);
        boolean isSingleplayer = server.isSingleplayer();
        return state.players.computeIfAbsent(!isSingleplayer ? player.getUuid() : null, uuid -> new PlayerSkillData(player));
    }
    
    public static void savePlayerState(PlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (player.getWorld().isClient || server == null) return; // Client side
        SkillData state = getServerState(server);
        PlayerSkillData playerState = getPlayerState(player);
        ServerWorld world = server.getWorld(World.OVERWORLD);
        assert world != null;
        state.players.put(player.getUuid(), playerState);
        world.getPersistentStateManager().set(TYPE, state);
        state.markDirty();
    }
    
    public record PlayerSkillData(SkillList skillList) {
        public PlayerSkillData(UUID uuid) {
            this(SkillManager.createEmptySkillList());
        }
        public PlayerSkillData(PlayerEntity player) {
            this(SkillManager.createEmptySkillList());
            skillList.putAll(SkillManager.getSkills(player));
        }
    }
}
