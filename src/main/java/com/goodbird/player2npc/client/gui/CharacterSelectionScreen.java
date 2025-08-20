package com.goodbird.player2npc.client.gui;

import adris.altoclef.player2api.Character;
import adris.altoclef.player2api.utils.CharacterUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public class CharacterSelectionScreen extends Screen {

    private Character[] characters = null;
    private boolean isLoading = true;

    public CharacterSelectionScreen() {
        super(Text.of("Select a Character"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();
        isLoading = true;

        CompletableFuture.supplyAsync(()->CharacterUtils.requestCharacters())
                .thenAcceptAsync(result -> {
                    this.characters = result;
                    this.isLoading = false;

                    this.client.execute(this::createCharacterCards);
                }, this.client);
    }

    private void createCharacterCards() {
        if (characters == null || characters.length == 0) return;

        int cardWidth = 100;
        int cardHeight = 130;
        int padding = 30;
        int cardsPerRow = Math.max(1, (this.width - padding) / (cardWidth + padding));

        int totalWidth = cardsPerRow * (cardWidth + padding) - padding;
        int startX = this.width / 2 - totalWidth / 2;
        int startY = 70;

        int currentX = startX;
        int currentY = startY;

        for (Character character : characters) {
            this.addDrawableChild(new CharacterCardWidget(currentX, currentY, cardWidth, cardHeight, character, this::onCharacterClicked));

            currentX += cardWidth + padding;
            if (currentX + cardWidth > startX + totalWidth) {
                currentX = startX;
                currentY += cardHeight + padding;
            }
        }
    }

    private void onCharacterClicked(Character character) {
        if (this.client != null) {
            this.client.setScreen(new CharacterDetailScreen(this, character));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);

        graphics.drawCenteredShadowedText(this.textRenderer, "Select a Character", this.width / 2, 20, 0xFFFFFF);

        if (isLoading) {
            graphics.drawCenteredShadowedText(this.textRenderer, "Loading...", this.width / 2, this.height / 2, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}