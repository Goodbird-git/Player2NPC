package com.goodbird.player2npc.client.gui;

import adris.altoclef.player2api.Character;
import com.goodbird.player2npc.client.util.SkinManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class CharacterCardWidget extends ClickableWidget {

    private final Character character;
    private final Consumer<Character> onClick;
    private final int BACKGROUND_COLOR = 0xFF181825;

    public CharacterCardWidget(int x, int y, int width, int height, Character character, Consumer<Character> onClick) {
        super(x, y, width, height, Text.of(character.name()));
        this.character = character;
        this.onClick = onClick;
    }

    @Override
    protected void drawWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xff1b1f4c);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 30, 0x20FFFFFF);

        int headSize = this.width - 24;
        int headX = this.getX() + 12;
        int headY = this.getY() + 42;
        Identifier skinId = SkinManager.getSkinIdentifier(character.skinURL());
        SkinManager.renderSkinHead(graphics, headX, headY, headSize, skinId);

        Text nameText = Text.of(character.shortName());
        int textY = this.getY() + 12;
        graphics.drawCenteredShadowedText(MinecraftClient.getInstance().textRenderer, nameText, this.getX() + this.width / 2, textY, 0xFFFFFF);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.active && this.visible) {
            this.onClick.accept(this.character);
        }
    }

    @Override
    protected void updateNarration(NarrationMessageBuilder builder) {

    }
}