package com.goodbird.player2npc;

import com.goodbird.player2npc.companion.AutomatoneEntity;
import com.goodbird.player2npc.companion.CompanionManager;
import com.goodbird.player2npc.network.AutomatoneDespawnRequestPacket;
import com.goodbird.player2npc.network.AutomatoneSpawnRequestPacket;

import adris.altoclef.AltoClefController;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Player2NPC implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Otomaton");
    public static final String MOD_ID = "player2npc";

    public static final Identifier SPAWN_PACKET_ID = new Identifier(MOD_ID, "spawn_automatone");
    public static final Identifier SPAWN_REQUEST_PACKET_ID = new Identifier(MOD_ID, "request_spawn_automatone");
    public static final Identifier DESPAWN_REQUEST_PACKET_ID = new Identifier(MOD_ID, "request_despawn_automatone");

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static final EntityType<AutomatoneEntity> AUTOMATONE = FabricEntityTypeBuilder.<AutomatoneEntity>createLiving()
            .spawnGroup(SpawnGroup.MISC)
            .entityFactory(AutomatoneEntity::new)
            .defaultAttributes(ZombieEntity::createAttributes)
            .dimensions(EntityDimensions.changing(EntityType.PLAYER.getWidth(), EntityType.PLAYER.getHeight()))
            .trackRangeBlocks(64)
            .trackedUpdateRate(1)
            .forceTrackedVelocityUpdates(true)
            .build();

    @Override
    public void onInitialize() {
        Registry.register(Registries.ENTITY_TYPE, id("aicompanion"), AUTOMATONE);

        ServerPlayNetworking.registerGlobalReceiver(SPAWN_REQUEST_PACKET_ID, AutomatoneSpawnRequestPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(DESPAWN_REQUEST_PACKET_ID, AutomatoneDespawnRequestPacket::handle);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CompanionManager.KEY.get(handler.player).summonAllCompanionsAsync();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            AltoClefController.staticServerTick(server);
        });
    }
}
