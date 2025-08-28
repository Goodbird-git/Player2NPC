package com.goodbird.player2npc.client;

import com.goodbird.player2npc.Player2NPCForge;
import com.goodbird.player2npc.client.render.RenderAutomaton;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Player2NPCForge.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    public static KeyMapping openCharacterScreenKeybind;

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Player2NPCForge.AUTOMATONE.get(), RenderAutomaton::new);
    }

    @SubscribeEvent
    public static void onRegisterKeybindings(RegisterKeyMappingsEvent event) {
        openCharacterScreenKeybind = new KeyMapping(
                "key.player2npc.open_character_screen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.player2npc.keys"
        );
        event.register(openCharacterScreenKeybind);
    }
}