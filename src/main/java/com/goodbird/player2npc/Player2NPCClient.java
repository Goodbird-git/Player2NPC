package com.goodbird.player2npc;

import com.goodbird.player2npc.client.gui.CharacterSelectionScreen;
import com.goodbird.player2npc.client.render.RenderAutomaton;
import com.goodbird.player2npc.network.AutomatonSpawnPacket;
import com.mojang.blaze3d.platform.InputUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBind;
import org.lwjgl.glfw.GLFW;

public class Player2NPCClient implements ClientModInitializer {

    private static KeyBind openCharacterScreenKeybind;
    private static long lastHeartbeatTime = System.nanoTime();

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(Player2NPC.AUTOMATONE, RenderAutomaton::new);

        ClientPlayNetworking.registerGlobalReceiver(Player2NPC.SPAWN_PACKET_ID, AutomatonSpawnPacket::handle);
        openCharacterScreenKeybind = KeyBindingHelper.registerKeyBinding(new KeyBind(
                "key.player2npc.open_character_screen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.player2npc.keys"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long now = System.nanoTime();
            if (now - lastHeartbeatTime > 60_000_000_000L) {
                // TODO: fix
                // AICommandBridge.sendHeartbeat("player2-ai-npc-minecraft");
                lastHeartbeatTime = now;
            }

            if (openCharacterScreenKeybind.wasPressed()) {
                if (client.world != null) {
                    client.setScreen(new CharacterSelectionScreen());
                }
            }
        });
    }
}
