//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.goodbird.player2npc.companion;

import com.goodbird.player2npc.mixins.IEntityPersistentData;
import com.player2.playerengine.player2api.Character;
import com.player2.playerengine.player2api.utils.CharacterUtils;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CompanionManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServerPlayer _player;
    private final Map<String, UUID> _companionMap = new ConcurrentHashMap();
    private final Map<String, CompoundTag> _despawnedCompanionData = new ConcurrentHashMap();
    private List<Character> _assignedCharacters = new ArrayList();
    private boolean _needsToSummon = false;
    private static final Map<String, CompanionManager> cache = new HashMap<>();

    public CompanionManager(ServerPlayer player) {
        this._player = player;
    }

    public static CompanionManager get(ServerPlayer player){
        return cache.computeIfAbsent(player.getName().getString(), (name)-> {
            CompanionManager manager = new CompanionManager(player);
            manager.readFromNbt();
            return manager;
        });
    }

    public static void remove(ServerPlayer player){
        cache.remove(player.getName().getString());
    }

    public void summonAllCompanionsAsync() {
        this._needsToSummon = true;
        CompletableFuture.supplyAsync(() -> CharacterUtils.requestCharacters(this._player, "player2-ai-npc-minecraft")).thenAcceptAsync((characters) -> this._assignedCharacters = new ArrayList(Arrays.asList(characters)), this._player.getServer());
    }

    private void summonCompanions() {
        if (!this._assignedCharacters.isEmpty()) {
            List<String> assignedNames = this._assignedCharacters.stream().map((c) -> c.name()).toList();
            List<String> toDismiss = new ArrayList();
            this._companionMap.forEach((name, uuid) -> {
                if (!assignedNames.contains(name)) {
                    toDismiss.add(name);
                }

            });
            toDismiss.forEach(this::dismissCompanion);
            this._assignedCharacters.stream().filter((character) -> character != null).forEach((character) -> {
                LOGGER.info("summonCompanions for character={}", character);
                this.ensureCompanionExists(character);
            });
            this._assignedCharacters.clear();
            writeToNbt();
        }
    }

    public void ensureCompanionExists(Character character) {
        LOGGER.info("ensureCompanionExists for character={}", character);
        if (this._player.level() != null && this._player.getServer() != null) {
            LOGGER.info("ensureCompanionExists NOTNULL");
            UUID companionUuid = (UUID)this._companionMap.get(character.name());
            ServerLevel world = this._player.serverLevel();
            if (this._despawnedCompanionData.containsKey(character.name())) {
                LOGGER.info("ensureCompanionExists DESPAWNED");
                try {
                    CompoundTag savedState = (CompoundTag) this._despawnedCompanionData.remove(character.name());
                    AutomatoneEntity restoredCompanion = new AutomatoneEntity(this._player.level(), character, this._player);
                    restoredCompanion.readAdditionalSaveData(savedState);
                    BlockPos spawnPos = this._player.blockPosition().offset(this._player.getRandom().nextInt(3) - 1, 1, this._player.getRandom().nextInt(3) - 1);
                    restoredCompanion.moveTo((double) spawnPos.getX() + (double) 0.5F, (double) spawnPos.getY(), (double) spawnPos.getZ() + (double) 0.5F, this._player.getYRot(), 0.0F);
                    world.addFreshEntity(restoredCompanion);
                    this._companionMap.put(character.name(), restoredCompanion.getUUID());
                    PrintStream var10000 = System.out;
                    String var10001 = character.name();
                    LOGGER.info("Restored companion from saved state: " + var10001 + " for player " + this._player.getName().getString());
                }catch (Exception e){
                    e.printStackTrace();
                }
                writeToNbt();
            } else {
                Entity existingCompanion = companionUuid != null ? world.getEntity(companionUuid) : null;
                BlockPos spawnPos = this._player.blockPosition().offset(this._player.getRandom().nextInt(3) - 1, 1, this._player.getRandom().nextInt(3) - 1);
                if (existingCompanion instanceof AutomatoneEntity && existingCompanion.isAlive()) {
                    LOGGER.info("ensureCompanionExists TP");
                    existingCompanion.moveTo((double)spawnPos.getX() + (double)0.5F, (double)spawnPos.getY(), (double)spawnPos.getZ() + (double)0.5F);
                    PrintStream var11 = System.out;
                    String var13 = character.name();
                    var11.println("Teleported existing companion: " + var13 + " for player " + this._player.getName().getString());
                } else {
                    LOGGER.info("ensureCompanionExists SPAWN");
                    try {
                        spawnCompanion(character);
                        System.out.println("Summoned new companion: " + character.name() + " for player " + this._player.getName().getString());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    writeToNbt();
                }
            }

        }
    }

    public void spawnCompanion(Character character){
        BlockPos spawnPos = this._player.blockPosition().offset(this._player.getRandom().nextInt(3) - 1, 1, this._player.getRandom().nextInt(3) - 1);
        AutomatoneEntity newCompanion = new AutomatoneEntity(this._player.level(), character, this._player);
        newCompanion.moveTo((double) spawnPos.getX() + (double) 0.5F, (double) spawnPos.getY(), (double) spawnPos.getZ() + (double) 0.5F, this._player.getYRot(), 0.0F);
        this._player.level().addFreshEntity(newCompanion);
        this._companionMap.put(character.name(), newCompanion.getUUID());
    }

    public void dismissCompanion(String characterName) {
        UUID companionUuid = (UUID)this._companionMap.remove(characterName);
        if (companionUuid != null && this._player.getServer() != null) {
            for(ServerLevel world : this._player.getServer().getAllLevels()) {
                Entity companion = world.getEntity(companionUuid);
                if (companion instanceof AutomatoneEntity) {
                    AutomatoneEntity automatone = (AutomatoneEntity)companion;
                    CompoundTag savedState = new CompoundTag();
                    automatone.addAdditionalSaveData(savedState);
                    this._despawnedCompanionData.put(characterName, savedState);
                    companion.discard();
                    System.out.println("Dismissed companion: " + characterName + " for player " + this._player.getName().getString());
                    writeToNbt();
                    return;
                }
            }

        }

    }

    public void dismissAllCompanions() {
        List<String> names = new ArrayList(this._companionMap.keySet());
        names.forEach(this::dismissCompanion);
        this._companionMap.clear();
    }

    public List<AutomatoneEntity> getActiveCompanions() {
        List<AutomatoneEntity> companions = new ArrayList();
        if (this._player.getServer() == null) {
            return companions;
        } else {
            for(UUID uuid : this._companionMap.values()) {
                for(ServerLevel world : this._player.getServer().getAllLevels()) {
                    Entity entity = world.getEntity(uuid);
                    if (entity instanceof AutomatoneEntity) {
                        AutomatoneEntity companion = (AutomatoneEntity)entity;
                        if (companion.isAlive()) {
                            companions.add(companion);
                            break;
                        }
                    }
                }
            }

            return companions;
        }
    }

    public void serverTick() {
        if (this._needsToSummon && !this._assignedCharacters.isEmpty()) {
            this.summonCompanions();
            this._needsToSummon = false;
        }

    }

    public void readFromNbt() {
        CompoundTag tag = ((IEntityPersistentData)_player).getPersistentData();
        if(tag.isEmpty()) return;
        this._companionMap.clear();
        this._despawnedCompanionData.clear();
        CompoundTag companionsTag = tag.getCompound("companions");

        for(String key : companionsTag.getAllKeys()) {
            this._companionMap.put(key, companionsTag.getUUID(key));
        }

        CompoundTag despawnedTag = tag.getCompound("despawnedCompanions");

        for(String key : despawnedTag.getAllKeys()) {
            this._despawnedCompanionData.put(key, despawnedTag.getCompound(key));
        }

    }

    public void writeToNbt() {
        CompoundTag tag = ((IEntityPersistentData)_player).getPersistentData();
        CompoundTag companionsTag = new CompoundTag();
        this._companionMap.forEach(companionsTag::putUUID);
        tag.put("companions", companionsTag);
        CompoundTag despawnedTag = new CompoundTag();
        this._despawnedCompanionData.forEach(despawnedTag::put);
        tag.put("despawnedCompanions", despawnedTag);
    }
}
