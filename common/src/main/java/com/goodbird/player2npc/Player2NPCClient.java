//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.goodbird.player2npc;

import com.goodbird.player2npc.client.gui.CharacterSelectionScreen;
import com.goodbird.player2npc.client.util.ClientPersistence;
import com.goodbird.player2npc.client.render.RenderAutomaton;
import com.goodbird.player2npc.network.AutomatonSpawnPacket;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.player2.playerengine.PlayerEngineClient;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import com.goodbird.player2npc.companion.AutomatoneEntity;
import com.player2.playerengine.player2api.utils.STTUtils;

public class Player2NPCClient {
    private static KeyMapping openCharacterScreenKeybind;
    private static long lastHeartbeatTime = System.nanoTime();
    private static KeyMapping ttsEnableKeybind;
    private static KeyMapping sttKeybind;

    public Player2NPCClient() {
    }

    public static void onInitializeClient() {
        EntityRendererRegistry.register(Player2NPC.AUTOMATONE, RenderAutomaton::new);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, Player2NPC.SPAWN_PACKET_ID,
                AutomatonSpawnPacket::handle);
        // https://www.glfw.org/docs/3.3/group__keys.html
        // 72 => H
        openCharacterScreenKeybind = new KeyMapping("key.player2npc.open_character_screen", Type.KEYSYM, 72,
                "category.player2npc.keys");
        // 79 => O
        ttsEnableKeybind = new KeyMapping("key.player2npc.tts_toggle", Type.KEYSYM, 79,
                "category.player2npc.keys");
        openCharacterScreenKeybind = new KeyMapping("key.player2npc.open_character_screen", Type.KEYSYM, 72, "category.player2npc.keys");
        // 86 => V
        sttKeybind = new KeyMapping("key.player2npc.stt_toggle", Type.KEYSYM, 86, "category.player2npc.keys");

        KeyMappingRegistry.register(openCharacterScreenKeybind);
        KeyMappingRegistry.register(ttsEnableKeybind);
        KeyMappingRegistry.register(sttKeybind);

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register((player) -> {
            if (!ClientPersistence.getTTStatus()) {
                player.sendSystemMessage(
                        Component.literal("Welcome to Player2NPC!"));
                player.sendSystemMessage(
                        Component.literal(
                                "To spawn a companion, press H. (Make sure you have at least one selected characeter at player2.game)"));
                player.sendSystemMessage(Component.literal("To toggle text to speech (TTS), press ").append(Component.keybind("key.player2npc.tts_toggle")));
                player.sendSystemMessage(Component.literal("To toggle speech to text (STT), press ").append(Component.keybind("key.player2npc.stt_toggle")));
                ClientPersistence.saveTTSStatus(true);
            }
        });
        ClientTickEvent.CLIENT_POST.register((client) -> {
            if (openCharacterScreenKeybind.consumeClick() && client.level != null) {
                client.setScreen(new CharacterSelectionScreen());
            }
            if (ttsEnableKeybind.consumeClick()) {
                PlayerEngineClient.enabledTTS = !PlayerEngineClient.enabledTTS;
                client.player.sendSystemMessage(
                        Component.literal(PlayerEngineClient.enabledTTS ? "Enabled TTS" : "Disabled TTS"));
            }
            if (sttKeybind.isDown()) {
                STTUtils.setIsListening(true, AutomatoneEntity.PLAYER2_GAME_ID);
            } else {
                STTUtils.setIsListening(false, AutomatoneEntity.PLAYER2_GAME_ID);
            }
        });

        ClientTickEvent.CLIENT_PRE.register((client) -> {
            STTUtils.update();
        });
    }
}
