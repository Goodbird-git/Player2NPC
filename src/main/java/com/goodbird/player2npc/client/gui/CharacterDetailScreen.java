package com.goodbird.player2npc.client.gui;

import adris.altoclef.player2api.Character;
import com.goodbird.player2npc.client.util.SkinManager;
import com.goodbird.player2npc.network.AutomatoneDespawnRequestPacket;
import com.goodbird.player2npc.network.AutomatoneSpawnRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class CharacterDetailScreen extends Screen {

    private final Screen parent;
    private final Character character;

    public CharacterDetailScreen(Screen parent, Character character) {
        super(Text.of("Character Details"));
        this.parent = parent;
        this.character = character;
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(ButtonWidget.builder(Text.of("Summon"), button -> {
            System.out.println("Summoning: " + character.name());
            client.getNetworkHandler().sendPacket(AutomatoneSpawnRequestPacket.create(character));
            if (this.client != null) {
                this.client.setScreen(null);
            }
        }).positionAndSize(this.width / 2 - 100, this.height - 100, 98, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("Despawn"), button -> {
            System.out.println("Summoning: " + character.name());
            client.getNetworkHandler().sendPacket(AutomatoneDespawnRequestPacket.create(character));
            if (this.client != null) {
                this.client.setScreen(null);
            }
        }).positionAndSize(this.width / 2 - 100, this.height - 130, 98, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }).positionAndSize(this.width / 2 + 2, this.height - 100, 98, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);

        graphics.drawCenteredShadowedText(this.textRenderer, character.name(), this.width / 2, 130, 0xFFFFFF);

        int headSize = 96;
        int headX = this.width / 2 - headSize / 2;
        int headY = 150;
        Identifier skinId = SkinManager.getSkinIdentifier(character.skinURL());
        SkinManager.renderSkinHead(graphics, headX, headY, headSize, skinId);

        int textY = headY + headSize + 15;
        List<StringVisitable> lines = this.textRenderer.getTextHandler().wrapLines(character.description(), 200, Style.EMPTY);
        for (StringVisitable line : lines) {
            graphics.drawCenteredShadowedText(textRenderer, line.getString(), this.width / 2, textY, 0xAAAAAA);
            textY += textRenderer.fontHeight + 2;
        }

        super.render(graphics, mouseX, mouseY, delta);
    }
}