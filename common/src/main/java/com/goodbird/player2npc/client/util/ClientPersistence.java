package com.goodbird.player2npc.client.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.architectury.platform.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

public class ClientPersistence {
    private static final Path FILE = Platform.getConfigFolder()
            .resolve("player2NPC-client.dat");

    public static void saveTTSStatus(boolean flag) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ttsHint", flag);
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(FILE))) {
            NbtIo.write(tag, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean getTTStatus() {
        if (!Files.exists(FILE))
            return false; // default to false if DNE
        try (DataInputStream in = new DataInputStream(Files.newInputStream(FILE))) {
            CompoundTag tag = NbtIo.read(in);
            return tag.getBoolean("ttsHint");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}