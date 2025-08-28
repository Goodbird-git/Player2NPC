package com.goodbird.player2npc.companion;

import altoclef.player2api.Character;
import altoclef.player2api.utils.CharacterUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class CompanionManager  {
   private static final  Logger LOGGER  = LogManager.getLogger();
   private final ServerPlayer _player;
   private final Map<String, UUID> _companionMap = new ConcurrentHashMap<>();
   private List<Character> _assignedCharacters = new ArrayList<>();
   private boolean _needsToSummon = false;

   public CompanionManager(ServerPlayer player) {
      this._player = player;
   }

   public void summonAllCompanionsAsync() {
      this._needsToSummon = true;
      CompletableFuture.<Character[]>supplyAsync(() -> CharacterUtils.requestCharacters("player2-ai-npc-minecraft"), this._player.getServer())
         .thenAcceptAsync(characters -> this._assignedCharacters = new ArrayList<>(Arrays.asList((Character[])characters)), this._player.getServer());
   }

   private void summonCompanions() {
      if (!this._assignedCharacters.isEmpty()) {
         List<String> assignedNames = this._assignedCharacters.stream().map(c -> c.name()).toList();
         List<String> toDismiss = new ArrayList<>();
         this._companionMap.forEach((name, uuid) -> {
            if (!assignedNames.contains(name)) {
               toDismiss.add(name);
            }
         });
         toDismiss.forEach(this::dismissCompanion);
         this._assignedCharacters.stream().filter(character -> character != null).forEach(character -> {
            LOGGER.info("summonCompanions for character={}", character);
            this.ensureCompanionExists(character);
         });

         this._assignedCharacters.clear();
      }
   }

   public void ensureCompanionExists(Character character) {
      LOGGER.info("ensureCompanionExists for character={}", character);
      if (this._player.level() != null && this._player.getServer() != null) {
         UUID companionUuid = this._companionMap.get(character.name());
         ServerLevel world = this._player.serverLevel();
         Entity existingCompanion = companionUuid != null ? world.getEntity(companionUuid) : null;
         BlockPos spawnPos = this._player.blockPosition().offset(this._player.getRandom().nextInt(3) - 1, 1, this._player.getRandom().nextInt(3) - 1);
         if (existingCompanion instanceof AutomatoneEntity && existingCompanion.isAlive()) {
            existingCompanion.teleportToWithTicket(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            System.out.println("Teleported existing companion: " + character.name() + " for player " + this._player.getName().getString());
         } else {
            AutomatoneEntity newCompanion = new AutomatoneEntity(this._player.level(), character);
            newCompanion.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, this._player.getYRot(), 0.0F);
            world.addFreshEntity(newCompanion);
            this._companionMap.put(character.name(), newCompanion.getUUID());
            System.out.println("Summoned new companion: " + character.name() + " for player " + this._player.getName().getString());
         }
      }
   }

   public void dismissCompanion(String characterName) {
      UUID companionUuid = this._companionMap.remove(characterName);
      if (companionUuid != null && this._player.getServer() != null) {
         for (ServerLevel world : this._player.getServer().getAllLevels()) {
            Entity companion = world.getEntity(companionUuid);
            if (companion instanceof AutomatoneEntity) {
               companion.discard();
               System.out.println("Dismissed companion: " + characterName + " for player " + this._player.getName().getString());
               return;
            }
         }
      }
   }

   public void dismissAllCompanions() {
      List<String> names = new ArrayList<>(this._companionMap.keySet());
      names.forEach(this::dismissCompanion);
      this._companionMap.clear();
   }

   public List<AutomatoneEntity> getActiveCompanions() {
      List<AutomatoneEntity> companions = new ArrayList<>();
      if (this._player.getServer() == null) {
         return companions;
      } else {
         for (UUID uuid : this._companionMap.values()) {
            for (ServerLevel world : this._player.getServer().getAllLevels()) {
               if (world.getEntity(uuid) instanceof AutomatoneEntity companion && companion.isAlive()) {
                  companions.add(companion);
                  break;
               }
            }
         }

         return companions;
      }
   }

   public void serverTick() {
      if (this._needsToSummon) {
         this.summonCompanions();
         this._needsToSummon = false;
      }
   }

   public void readFromNbt(CompoundTag tag) {
      CompoundTag companionsTag = tag.getCompound("companions");

      for (String key : companionsTag.getAllKeys()) {
         this._companionMap.put(key, companionsTag.getUUID(key));
      }
   }

   public void writeToNbt(CompoundTag tag) {
      CompoundTag companionsTag = new CompoundTag();
      this._companionMap.forEach(companionsTag::putUUID);
      tag.put("companions", companionsTag);
   }
}
