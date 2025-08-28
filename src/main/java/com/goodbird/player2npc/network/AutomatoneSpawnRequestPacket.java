package com.goodbird.player2npc.network;

import adris.altoclef.player2api.Character;
import adris.altoclef.player2api.utils.CharacterUtils;
import com.goodbird.player2npc.Player2NPC;
import com.goodbird.player2npc.companion.CompanionManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;




public class AutomatoneSpawnRequestPacket implements FabricPacket {
   private static final Logger LOGGER = LogManager.getLogger();
    public static final PacketType<AutomatonSpawnPacket> TYPE = PacketType.create(
            Player2NPC.SPAWN_REQUEST_PACKET_ID,
            AutomatonSpawnPacket::new
    );

    private final Character character;

    private AutomatoneSpawnRequestPacket(Character character) {
        this.character = character;
    }

    public AutomatoneSpawnRequestPacket(PacketByteBuf buf) {
        this.character = CharacterUtils.readFromBuf(buf);
    }

    public static Packet<ServerPlayPacketListener> create(Character character) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        new AutomatoneSpawnRequestPacket(character).write(buf);
        return ClientPlayNetworking.createC2SPacket(Player2NPC.SPAWN_REQUEST_PACKET_ID, buf);
    }

    @Override
    public void write(PacketByteBuf buf) {
        CharacterUtils.writeToBuf(buf, character);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static void handle(MinecraftServer var1, ServerPlayerEntity var2, ServerPlayNetworkHandler var3, PacketByteBuf var4, PacketSender var5) {
        AutomatoneSpawnRequestPacket packet = new AutomatoneSpawnRequestPacket(var4);
        LOGGER.info("AutomatoneSpawnReqPacket C2S/ character={}", packet.character);
        if(packet.character != null){
            var1.execute(() -> CompanionManager.KEY.get(var2).ensureCompanionExists(packet.character));
        }
    }
}
