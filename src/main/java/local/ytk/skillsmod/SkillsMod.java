package local.ytk.skillsmod;

import local.ytk.skillsmod.command.SkillCommand;
import local.ytk.skillsmod.network.SkillListSyncPayload;
import local.ytk.skillsmod.network.SkillUpdatePayload;
import local.ytk.skillsmod.skills.SkillData;
import local.ytk.skillsmod.skills.SkillInstance;
import local.ytk.skillsmod.skills.SkillList;
import local.ytk.skillsmod.skills.SkillManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SkillsMod implements ModInitializer {
    public static final String MOD_ID = "skills";
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
    
    final SkillManager skillManager = SkillManager.INSTANCE;
    
    //    DataRequestManager<ServerPlayerEntity> dataRequestManager = new DataRequestManager<>(ServerPlayNetworking::send);
    
    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(skillManager);
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            SkillCommand.register(dispatcher);
        });
        ServerPlayConnectionEvents.JOIN.register(SkillsMod::playerJoin);
        ServerLivingEntityEvents.AFTER_DEATH.register(SkillsMod::afterDeath);
        ServerPlayerEvents.AFTER_RESPAWN.register(SkillsMod::playerRespawn);
        
        PayloadTypeRegistry.playS2C().register(SkillListSyncPayload.PAYLOAD_ID, SkillListSyncPayload.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(SkillUpdatePayload.PAYLOAD_ID, SkillUpdatePayload.PACKET_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SkillUpdatePayload.PAYLOAD_ID, this::handleSkillUpdate);
    }
    
    private static void playerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        syncSkills(handler.player, handler.player, SkillManager.getSkills(handler.player));
    }
    private static void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (entity instanceof ServerPlayerEntity player)
            syncSkills(player.server, player, SkillManager.getSkills(player));
    }
    private static void playerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        syncSkills(oldPlayer.server, oldPlayer, SkillManager.getSkills(oldPlayer));
        syncSkills(newPlayer.server, newPlayer, SkillManager.getSkills(newPlayer));
    }
    
    private void handleSkillUpdate(SkillUpdatePayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        SkillData.PlayerSkillData playerState = SkillData.getPlayerState(player);
        SkillList playerSkillList = playerState.skillList();
        SkillInstance newSkillInstance = payload.skillInstance();
        SkillInstance playerSkillInstance = playerSkillList.get(newSkillInstance.skill);
        if (payload.addLevels() > 1) return; // Prevent cheating
        if (payload.addLevels() > 0) {
            playerSkillInstance.addLevels(payload.addLevels(), player);
        }
        if (payload.addXp() > 0) {
            playerSkillInstance.addXp(payload.addXp(), player);
        }
        if (payload.addLevels() > 0 || payload.addXp() > 0) {
            playerSkillList.replace(playerSkillInstance, newSkillInstance);
            syncSkills(player.server, player, playerSkillList);
        }
    }
    
    public static void syncSkills(MinecraftServer server, ServerPlayerEntity target, SkillList skills) {
        server.getPlayerManager().sendToAll(SkillListSyncPayload.createPacket(target, skills));
    }
    public static void syncSkills(ServerPlayerEntity player, ServerPlayerEntity target, SkillList skills) {
        player.networkHandler.sendPacket(SkillListSyncPayload.createPacket(target, skills));
    }
}
