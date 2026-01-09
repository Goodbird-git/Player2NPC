//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.goodbird.player2npc;

import com.goodbird.player2npc.companion.AutomatoneEntity;
import com.goodbird.player2npc.companion.CompanionManager;
import com.goodbird.player2npc.network.AutomatoneDespawnRequestPacket;
import com.goodbird.player2npc.network.AutomatoneSpawnRequestPacket;
import com.player2.playerengine.PlayerEngine;
import com.player2.playerengine.PlayerEngineController;
import com.player2.playerengine.player2api.utils.Player2HTTPUtils;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Player2NPC {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "player2npc";
    public static final ResourceLocation SPAWN_PACKET_ID = ResourceLocation.fromNamespaceAndPath("player2npc", "spawn_automatone");
    public static final ResourceLocation SPAWN_REQUEST_PACKET_ID = ResourceLocation.fromNamespaceAndPath("player2npc", "request_spawn_automatone");
    public static final ResourceLocation DESPAWN_REQUEST_PACKET_ID = ResourceLocation.fromNamespaceAndPath("player2npc", "request_despawn_automatone");

    public Player2NPC() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("player2npc", path);
    }

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(MOD_ID, Registries.ENTITY_TYPE);
    public static RegistrySupplier<EntityType<AutomatoneEntity>> AUTOMATONE = ENTITY_TYPES.register(id("aicompanion"), () -> {
        LOGGER.info("REGISTER");
        EntityType<AutomatoneEntity> AUTOMATONE = EntityType.Builder.<AutomatoneEntity>of(AutomatoneEntity::new, MobCategory.CREATURE).sized(EntityType.PLAYER.getWidth(), EntityType.PLAYER.getHeight()).clientTrackingRange(64).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, id("aicompanion")));
        LOGGER.info("CREATE");
        return AUTOMATONE;
    });

    public static void onInitialize() {
        LOGGER.info("INIT");
        ENTITY_TYPES.register();
        EntityAttributeRegistry.register(AUTOMATONE, Zombie::createAttributes);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SPAWN_REQUEST_PACKET_ID, AutomatoneSpawnRequestPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DESPAWN_REQUEST_PACKET_ID, AutomatoneDespawnRequestPacket::handle);
        PlayerEvent.PLAYER_JOIN.register((player) -> {
            // Async health check on join - will trigger reauth if 401/402
            PlayerEngine.getExecutor().submit(() -> {
                try {
                    Player2HTTPUtils.sendRequest(player, "player2-ai-npc-minecraft", "/v1/health", false, null);
                    LOGGER.info("Health check passed for player {}", player.getName().getString());
                } catch (Exception e) {
                    LOGGER.warn("Health check failed for player {}: {}", player.getName().getString(), e.getMessage());
                }
            });
            CompanionManager.get(player).summonAllCompanionsAsync();
        });
        PlayerEvent.PLAYER_QUIT.register((player) -> {
            CompanionManager.get(player).dismissAllCompanions();
            CompanionManager.remove(player);
        });
        TickEvent.SERVER_POST.register(PlayerEngineController::staticServerTick);
        TickEvent.PLAYER_POST.register((player) -> {
            if (player instanceof ServerPlayer serverPlayer)
                CompanionManager.get(serverPlayer).serverTick();
        });
    }
}
