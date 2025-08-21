package com.goodbird.player2npc.companion;

import adris.altoclef.player2api.Character;
import adris.altoclef.player2api.utils.CharacterUtils;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CompanionManager implements Component, ServerTickingComponent {

    public static final ComponentKey<CompanionManager> KEY =
            ComponentRegistry.getOrCreate(new Identifier("automatone", "companion_manager"), CompanionManager.class);

    private final ServerPlayerEntity _player;

    private final Map<String, UUID> _companionMap = new ConcurrentHashMap<>();

    private List<Character> _assignedCharacters = new ArrayList<>();
    private boolean _needsToSummon = false;

    public CompanionManager(ServerPlayerEntity player) {
        this._player = player;
    }

    public void summonAllCompanionsAsync() {
        _needsToSummon = true;
        CompletableFuture.supplyAsync(()->CharacterUtils.requestCharacters(), _player.getServer())
                .thenAcceptAsync(characters -> this._assignedCharacters = new ArrayList<>(Arrays.asList(characters)), _player.getServer());
    }

    private void summonCompanions() {
        if (_assignedCharacters.isEmpty()) {
            return;
        }

        List<String> assignedNames = _assignedCharacters.stream().map(c -> c.name()).toList();
        List<String> toDismiss = new ArrayList<>();
        _companionMap.forEach((name, uuid) -> {
            if (!assignedNames.contains(name)) {
                toDismiss.add(name);
            }
        });
        toDismiss.forEach(this::dismissCompanion);

        for (Character character : _assignedCharacters) {
            ensureCompanionExists(character);
        }

        _assignedCharacters.clear();
    }

    public void ensureCompanionExists(Character character) {
        if (_player.getWorld() == null || _player.getServer() == null) return;

        UUID companionUuid = _companionMap.get(character.name());
        ServerWorld world = _player.getServerWorld();
        Entity existingCompanion = (companionUuid != null) ? world.getEntity(companionUuid) : null;

        BlockPos spawnPos = _player.getBlockPos().add(
                _player.getRandom().nextInt(3) - 1,
                1,
                _player.getRandom().nextInt(3) - 1
        );

        if (existingCompanion instanceof AutomatoneEntity && existingCompanion.isAlive()) {
            existingCompanion.teleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            System.out.println("Teleported existing companion: " + character.name() + " for player " + _player.getName().getString());
        } else {
            AutomatoneEntity newCompanion = new AutomatoneEntity(_player.getWorld(), character);
            newCompanion.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, _player.getYaw(), 0);

            world.spawnEntity(newCompanion);
            _companionMap.put(character.name(), newCompanion.getUuid());
            System.out.println("Summoned new companion: " + character.name() + " for player " + _player.getName().getString());
        }
    }

    public void dismissCompanion(String characterName) {
        UUID companionUuid = _companionMap.remove(characterName);
        if (companionUuid != null && _player.getServer() != null) {
            for (ServerWorld world : _player.getServer().getWorlds()) {
                Entity companion = world.getEntity(companionUuid);
                if (companion instanceof AutomatoneEntity) {
                    companion.discard();
                    System.out.println("Dismissed companion: " + characterName + " for player " + _player.getName().getString());
                    return;
                }
            }
        }
    }

    public void dismissAllCompanions() {
        List<String> names = new ArrayList<>(_companionMap.keySet());
        names.forEach(this::dismissCompanion);
        _companionMap.clear();
    }

    public List<AutomatoneEntity> getActiveCompanions() {
        List<AutomatoneEntity> companions = new ArrayList<>();
        if (_player.getServer() == null) return companions;

        for (UUID uuid : _companionMap.values()) {
            for (ServerWorld world : _player.getServer().getWorlds()) {
                Entity entity = world.getEntity(uuid);
                if (entity instanceof AutomatoneEntity companion && companion.isAlive()) {
                    companions.add(companion);
                    break;
                }
            }
        }
        return companions;
    }

    @Override
    public void serverTick() {
        if (_needsToSummon) {
            summonCompanions();
            _needsToSummon = false;
        }
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        NbtCompound companionsTag = tag.getCompound("companions");
        for (String key : companionsTag.getKeys()) {
            _companionMap.put(key, companionsTag.getUuid(key));
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtCompound companionsTag = new NbtCompound();
        _companionMap.forEach(companionsTag::putUuid);
        tag.put("companions", companionsTag);
    }
}