package com.goodbird.player2npc.network;

import altoclef.player2api.Character;
import altoclef.player2api.utils.CharacterUtils;
import com.goodbird.player2npc.capability.CompanionCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AutomatoneDespawnRequestPacket {
   private final Character character;

   private AutomatoneDespawnRequestPacket(Character character) {
      this.character = character;
   }

   public AutomatoneDespawnRequestPacket(FriendlyByteBuf buf) {
      this.character = CharacterUtils.readFromBuf(buf);
   }

   public void write(FriendlyByteBuf buf) {
      CharacterUtils.writeToBuf(buf, this.character);
   }

   public static void send(Character character) {
      ForgeNetwork.sendToServer(new AutomatoneDespawnRequestPacket(character));
   }

   public static void handle(AutomatoneDespawnRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer player = context.getSender();
         if (player == null) {
            return;
         }
         player.getCapability(CompanionCapability.INSTANCE).ifPresent(companionManager -> {
            companionManager.dismissCompanion(msg.character.name());
         });
      });
      context.setPacketHandled(true);
   }
}
