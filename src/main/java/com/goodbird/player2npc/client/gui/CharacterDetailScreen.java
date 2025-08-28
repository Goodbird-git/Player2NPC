package com.goodbird.player2npc.client.gui;

import altoclef.player2api.Character;
import com.goodbird.player2npc.client.util.SkinManager;
import com.goodbird.player2npc.network.AutomatoneDespawnRequestPacket;
import com.goodbird.player2npc.network.AutomatoneSpawnRequestPacket;
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
      this.addRenderableWidget(Button.builder(Component.nullToEmpty("Summon"), button -> {
         System.out.println("Summoning: " + this.character.name());
         AutomatoneSpawnRequestPacket.send(this.character);
         if (this.minecraft != null) {
            this.minecraft.setScreen(null);
         }
      }).bounds(this.width / 2 - 100, this.height - 100, 98, 20).build());
      this.addRenderableWidget(Button.builder(Component.nullToEmpty("Despawn"), button -> {
         System.out.println("Summoning: " + this.character.name());
         AutomatoneDespawnRequestPacket.send(this.character);
         if (this.minecraft != null) {
            this.minecraft.setScreen(null);
         }
      }).bounds(this.width / 2 - 100, this.height - 130, 98, 20).build());
      this.addRenderableWidget(Button.builder(Component.nullToEmpty("Back"), button -> {
         if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
         }
      }).bounds(this.width / 2 + 2, this.height - 100, 98, 20).build());
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      this.renderBackground(graphics);
      graphics.drawCenteredString(this.font, this.character.name(), this.width / 2, 130, 16777215);
      int headSize = 96;
      int headX = this.width / 2 - headSize / 2;
      int headY = 150;
      ResourceLocation skinId = SkinManager.getSkinIdentifier(this.character.skinURL());
      SkinManager.renderSkinHead(graphics, headX, headY, headSize, skinId);
      int textY = headY + headSize + 15;

      for (FormattedText line : this.font.getSplitter().splitLines(this.character.description(), 200, Style.EMPTY)) {
         graphics.drawCenteredString(this.font, line.getString(), this.width / 2, textY, 11184810);
         textY += 9 + 2;
      }

      super.render(graphics, mouseX, mouseY, delta);
   }
}
