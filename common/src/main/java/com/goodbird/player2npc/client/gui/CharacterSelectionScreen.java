//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.goodbird.player2npc.client.gui;

import com.goodbird.player2npc.Player2NPC;
import com.goodbird.player2npc.network.AutomatoneSpawnRequestPacket;
import com.player2.playerengine.player2api.Character;
import com.player2.playerengine.player2api.auth.AuthenticationManager;
import com.player2.playerengine.player2api.auth.TokenStorage;
import com.player2.playerengine.player2api.utils.CharacterUtils;
import java.util.concurrent.CompletableFuture;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CharacterSelectionScreen extends Screen {
    private Character[] characters = null;
    private boolean isLoading = true;

    public CharacterSelectionScreen() {
        super(Component.nullToEmpty("Available characters"));
    }

    protected void init() {
        super.init();
        this.clearWidgets();
        this.isLoading = true;
        CompletableFuture.supplyAsync(() -> CharacterUtils.requestCharacters(Minecraft.getInstance().player, "player2-ai-npc-minecraft")).thenAcceptAsync((result) -> {
            this.characters = result;
            this.isLoading = false;
            this.minecraft.execute(this::createCharacterCards);
        }, this.minecraft);

        this.addRenderableWidget(Button.builder(Component.nullToEmpty("Re-authentificate"), (button) -> {
            CompletableFuture.runAsync(()-> {
                TokenStorage.clearAllTokens();
                AuthenticationManager.getInstance().checkAuth(minecraft.player, "player2-ai-npc-minecraft");
            });
        }).bounds((this.width-256)/2+8, (this.height-200)/2+170, 120, 20).build());
    }

    private void createCharacterCards() {
        if (this.characters != null && this.characters.length != 0) {
            int cardWidth = 45;
            int cardHeight = 60;
            int padding = 5;
            int cardsPerRow = 5;
            int totalWidth = cardsPerRow * (cardWidth + padding) - padding;
            int startX = (this.width-256)/2+6;
            int startY = (this.height-200)/2+22;
            int currentX = startX;
            int currentY = startY;

            for(Character character : this.characters) {
                this.addRenderableWidget(new CharacterCardWidget(currentX, currentY, cardWidth, cardHeight, character, this::onCharacterClicked));
                currentX += cardWidth + padding;
                if (currentX + cardWidth > startX + totalWidth) {
                    currentX = startX;
                    currentY += cardHeight + padding;
                }
            }

        }
    }

    private void onCharacterClicked(Character character) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new CharacterDetailScreen(this, character));
        }

    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {

    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(ResourceLocation.bySeparator("player2npc:textures/gui/standardbg.png", ':'), (this.width-256)/2, (this.height-200)/2, 0, 0, 256, 256);
        graphics.drawCenteredString(this.font, "Available characters", (this.width-256)/2+130, (this.height-200)/2+7, 16777215);
        if (this.isLoading) {
            graphics.drawCenteredString(this.font, "Loading...", this.width / 2, this.height / 2, 11184810);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    public boolean isPauseScreen() {
        return false;
    }
}
