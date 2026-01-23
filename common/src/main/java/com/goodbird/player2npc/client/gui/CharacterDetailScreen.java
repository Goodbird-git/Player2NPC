//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.goodbird.player2npc.client.gui;

import com.goodbird.player2npc.Player2NPC;
import com.player2.playerengine.player2api.Character;
import com.goodbird.player2npc.client.util.SkinManager;
import com.goodbird.player2npc.network.AutomatoneDespawnRequestPacket;
import com.goodbird.player2npc.network.AutomatoneSpawnRequestPacket;
import java.util.Objects;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public class CharacterDetailScreen extends Screen {
    private final Screen parent;
    private final Character character;

    public CharacterDetailScreen(Screen parent, Character character) {
        super(Component.nullToEmpty("Character Details"));
        this.parent = parent;
        this.character = character;
    }

    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.nullToEmpty("Summon"), (button) -> {
            System.out.println("Summoning: " + this.character.name());
            NetworkManager.sendToServer(Player2NPC.SPAWN_REQUEST_PACKET_ID, AutomatoneSpawnRequestPacket.create(this.character));
            if (this.minecraft != null) {
                this.minecraft.setScreen((Screen)null);
            }
        }).bounds((this.width-200)/2+10, (this.height-230)/2+170, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.nullToEmpty("Despawn"), (button) -> {
            System.out.println("Summoning: " + this.character.name());
            NetworkManager.sendToServer(Player2NPC.DESPAWN_REQUEST_PACKET_ID, AutomatoneDespawnRequestPacket.create(this.character));
            if (this.minecraft != null) {
                this.minecraft.setScreen((Screen)null);
            }

        }).bounds((this.width-200)/2+102, (this.height-230)/2+170, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.nullToEmpty("Back"), (button) -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }

        }).bounds((this.width-200)/2+58, (this.height-230)/2+200, 80, 20).build());
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        graphics.blit(ResourceLocation.of("player2npc:textures/gui/largebg.png", ':'), (this.width-200)/2, (this.height-230)/2, 0, 0, 256, 256);
        graphics.drawCenteredString(this.font, this.character.name(), (this.width-200)/2+100, (this.height-230)/2+8, 16777215);
        int headSize = 66;
        int headX = (this.width-200)/2+67;
        int headY = (this.height-230)/2+25;
        ResourceLocation skinId = SkinManager.getSkinIdentifier(this.character.skinURL());
        SkinManager.renderSkinHead(graphics, headX, headY, headSize, skinId);
        int textY = headY + headSize + 15;

        for(FormattedText line : this.font.getSplitter().splitLines(this.character.description(), 180, Style.EMPTY)) {
            graphics.drawCenteredString(this.font, line.getString(), (this.width-200)/2+95, textY, 11184810);
            Objects.requireNonNull(this.font);
            textY += 9 + 2;
            if(textY>(this.height-230)/2+150) break;
        }

        super.render(graphics, mouseX, mouseY, delta);
    }
}
