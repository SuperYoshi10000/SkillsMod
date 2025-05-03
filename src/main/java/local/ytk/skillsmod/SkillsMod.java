package local.ytk.skillsmod;

import local.ytk.skillsmod.command.SkillCommand;
import local.ytk.skillsmod.network.SkillListPayload;
import local.ytk.skillsmod.network.SkillListSyncPayload;
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
        
        PayloadTypeRegistry.playS2C().register(SkillListPayload.PAYLOAD_ID, SkillListPayload.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(SkillListPayload.PAYLOAD_ID, SkillListPayload.PACKET_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SkillListPayload.PAYLOAD_ID, this::handleSkillList);
    }
    
    private static void playerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        syncSkills(handler.player);
    }
    private static void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (entity instanceof ServerPlayerEntity player)
            syncSkills(player.server, player, SkillManager.getSkills(player));
    }
    private static void playerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        syncSkills(oldPlayer.server, oldPlayer, SkillManager.getSkills(oldPlayer));
        syncSkills(newPlayer.server, newPlayer, SkillManager.getSkills(newPlayer));
    }
    
    private void handleSkillList(SkillListPayload payload, ServerPlayNetworking.Context context) {
        SkillList skillList = payload.skillList();
        ServerPlayerEntity player = context.player();
        switch (payload.type()) {
            case REQUEST -> ServerPlayNetworking.send(player, SkillListPayload.response(SkillManager.getSkills(player)));
            case MODIFY -> {
                // Only allowed to upgrade one skill at a time - this is to prevent cheating
                if (skillList.skills().size() != 1) {
                    ServerPlayNetworking.send(player, SkillListPayload.rejection(payload.skillList()));
                    return;
                }
                SkillInstance skillInstance = skillList.skills().values().iterator().next();
                SkillList playerSkillList = SkillManager.getSkills(player);
                SkillInstance playerSkillInstance = playerSkillList.skills().get(skillInstance.getSkill());
                if (skillInstance.level - playerSkillInstance.level != 1) {
                    ServerPlayNetworking.send(player, SkillListPayload.rejection(payload.skillList()));
                    return;
                } // upgrading by one level
                int neededXp = skillInstance.getXpToNextLevel();
                if (player.totalExperience < neededXp) {
                    ServerPlayNetworking.send(player, SkillListPayload.rejection(payload.skillList()));
                    return;
                } // enough xp
                playerSkillInstance.spendXp(neededXp, player);
                ServerPlayNetworking.send(player, SkillListPayload.confirmation(playerSkillList));
                SkillManager.updateSkills(player, playerSkillList);
                
                if (skillInstance.skill.id.equals(id("max_health"))) {
                    // heal player
                    // this is a special case, because upgrading the max health skill gives you more health
                    player.heal(1.0f);
                }
            }
            // client should not try to do anything else
        }
    }
    
    public static void syncSkills(MinecraftServer server, ServerPlayerEntity target, SkillList skills) {
        server.getPlayerManager().sendToAll(SkillListSyncPayload.createPacket(target, skills));
    }
    public static void syncSkills(ServerPlayerEntity player, ServerPlayerEntity target, SkillList skills) {
        player.networkHandler.sendPacket(SkillListSyncPayload.createPacket(target, skills));
    }
}
