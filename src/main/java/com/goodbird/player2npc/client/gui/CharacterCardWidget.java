package com.goodbird.player2npc.client.gui;

import altoclef.player2api.Character;
import com.goodbird.player2npc.client.util.SkinManager;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CharacterCardWidget extends AbstractWidget {
   private final Character character;
   private final Consumer<Character> onClick;
   private final int BACKGROUND_COLOR = -15198171;

   public CharacterCardWidget(int x, int y, int width, int height, Character character, Consumer<Character> onClick) {
      super(x, y, width, height, Component.nullToEmpty(character.name()));
      this.character = character;
      this.onClick = onClick;
   }

   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
      graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -14999732);
      graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 30, 553648127);
      int headSize = this.width - 24;
      int headX = this.getX() + 12;
      int headY = this.getY() + 42;
      ResourceLocation skinId = SkinManager.getSkinIdentifier(this.character.skinURL());
      SkinManager.renderSkinHead(graphics, headX, headY, headSize, skinId);
      Component nameText = Component.nullToEmpty(this.character.shortName());
      int textY = this.getY() + 12;
      graphics.drawCenteredString(Minecraft.getInstance().font, nameText, this.getX() + this.width / 2, textY, 16777215);
   }

   public void onClick(double mouseX, double mouseY) {
      if (this.active && this.visible) {
         this.onClick.accept(this.character);
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput builder) {
   }
}
