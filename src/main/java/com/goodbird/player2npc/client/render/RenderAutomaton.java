package com.goodbird.player2npc.client.render;

import com.goodbird.player2npc.client.util.ImageDownloadAlt;
import com.goodbird.player2npc.client.util.ResourceDownloader;
import com.goodbird.player2npc.companion.AutomatoneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

public class RenderAutomaton extends LivingEntityRenderer<AutomatoneEntity, PlayerModel<AutomatoneEntity>> {
   public RenderAutomaton(Context ctx) {
      super(ctx, new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      boolean slim = false;
      this.addLayer(
         new HumanoidArmorLayer(
            this,
            new HumanoidArmorModel(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidArmorModel(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)),
            ctx.getModelManager()
         )
      );
      this.addLayer(new PlayerItemInHandLayer(this, ctx.getItemInHandRenderer()));
      this.addLayer(new ArrowLayer(ctx, this));
      this.addLayer(new CustomHeadLayer(this, ctx.getModelSet(), ctx.getItemInHandRenderer()));
      this.addLayer(new ElytraLayer(this, ctx.getModelSet()));
      this.addLayer(new SpinAttackEffectLayer(this, ctx.getModelSet()));
      this.addLayer(new BeeStingerLayer(this));
   }

   public void render(AutomatoneEntity automatoneEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i) {
      try {
         this.setModelPose(automatoneEntity);
         super.render(automatoneEntity, f, g, matrixStack, vertexConsumerProvider, i);
      } catch (Exception var8) {
      }
   }

   public Vec3 getPositionOffset(AutomatoneEntity automatoneEntity, float f) {
      return automatoneEntity.isCrouching() ? new Vec3(0.0, -0.125, 0.0) : super.getRenderOffset(automatoneEntity, f);
   }

   private void setModelPose(AutomatoneEntity player) {
      PlayerModel<AutomatoneEntity> playerEntityModel = (PlayerModel<AutomatoneEntity>)this.getModel();
      if (player.isSpectator()) {
         playerEntityModel.setAllVisible(false);
         playerEntityModel.head.visible = true;
         playerEntityModel.hat.visible = true;
      } else {
         playerEntityModel.setAllVisible(true);
         playerEntityModel.hat.visible = true;
         playerEntityModel.jacket.visible = true;
         playerEntityModel.leftPants.visible = true;
         playerEntityModel.rightPants.visible = true;
         playerEntityModel.leftSleeve.visible = true;
         playerEntityModel.rightSleeve.visible = true;
         playerEntityModel.crouching = player.isCrouching();
         ArmPose armPose = getArmPose(player, InteractionHand.MAIN_HAND);
         ArmPose armPose2 = getArmPose(player, InteractionHand.OFF_HAND);
         if (armPose.isTwoHanded()) {
            armPose2 = player.getOffhandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
         }

         if (player.getMainArm() == HumanoidArm.RIGHT) {
            playerEntityModel.rightArmPose = armPose;
            playerEntityModel.leftArmPose = armPose2;
         } else {
            playerEntityModel.rightArmPose = armPose2;
            playerEntityModel.leftArmPose = armPose;
         }
      }
   }

   private static ArmPose getArmPose(AutomatoneEntity player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (itemStack.isEmpty()) {
         return ArmPose.EMPTY;
      } else {
         if (player.getUsedItemHand() == hand && player.getUseItemRemainingTicks() > 0) {
            UseAnim useAction = itemStack.getUseAnimation();
            if (useAction == UseAnim.BLOCK) {
               return ArmPose.BLOCK;
            }

            if (useAction == UseAnim.BOW) {
               return ArmPose.BOW_AND_ARROW;
            }

            if (useAction == UseAnim.SPEAR) {
               return ArmPose.THROW_SPEAR;
            }

            if (useAction == UseAnim.CROSSBOW && hand == player.getUsedItemHand()) {
               return ArmPose.CROSSBOW_CHARGE;
            }

            if (useAction == UseAnim.SPYGLASS) {
               return ArmPose.SPYGLASS;
            }

            if (useAction == UseAnim.TOOT_HORN) {
               return ArmPose.TOOT_HORN;
            }

            if (useAction == UseAnim.BRUSH) {
               return ArmPose.BRUSH;
            }
         } else if (!player.swinging && itemStack.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack)) {
            return ArmPose.CROSSBOW_HOLD;
         }

         return ArmPose.ITEM;
      }
   }

   public ResourceLocation getTextureLocation(AutomatoneEntity npc) {
      if (npc.textureLocation == null) {
         try {
            boolean fixSkin = true;
            File file = ResourceDownloader.getUrlFile(npc.getCharacter().skinURL(), fixSkin);
            npc.textureLocation = ResourceDownloader.getUrlResourceLocation(npc.getCharacter().skinURL(), fixSkin);
            this.loadSkin(file, npc.textureLocation, npc.getCharacter().skinURL(), fixSkin);
         } catch (Exception var4) {
            var4.printStackTrace();
         }
      }

      return npc.textureLocation == null ? DefaultPlayerSkin.getDefaultSkin() : npc.textureLocation;
   }

   private void loadSkin(File file, ResourceLocation resource, String par1Str, boolean fix64) {
      TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
      AbstractTexture object = texturemanager.getTexture(resource, (AbstractTexture)null);
      if (object == null) {
         ResourceDownloader.load(new ImageDownloadAlt(file, par1Str, resource, DefaultPlayerSkin.getDefaultSkin(), fix64, () -> {}));
      }
   }

   protected void scale(AutomatoneEntity automatoneEntity, PoseStack matrixStack, float f) {
      float g = 0.9375F;
      matrixStack.scale(0.9375F, 0.9375F, 0.9375F);
   }

   protected void setupTransforms(AutomatoneEntity automatoneEntity, PoseStack matrixStack, float f, float g, float h) {
      float i = automatoneEntity.getSwimAmount(h);
      if (automatoneEntity.isFallFlying()) {
         super.setupRotations(automatoneEntity, matrixStack, f, g, h);
         float j = automatoneEntity.getFallFlyingTicks() + h;
         float k = Mth.clamp(j * j / 100.0F, 0.0F, 1.0F);
         if (!automatoneEntity.isAutoSpinAttack()) {
            matrixStack.mulPose(Axis.XP.rotationDegrees(k * (-90.0F - automatoneEntity.getXRot())));
         }

         Vec3 vec3d = automatoneEntity.getViewVector(h);
         Vec3 vec3d2 = automatoneEntity.lerpVelocity(h);
         double d = vec3d2.horizontalDistanceSqr();
         double e = vec3d.horizontalDistanceSqr();
         if (d > 0.0 && e > 0.0) {
            double l = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e);
            double m = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
            matrixStack.mulPose(Axis.YP.rotation((float)(Math.signum(m) * Math.acos(l))));
         }
      } else if (i > 0.0F) {
         super.setupRotations(automatoneEntity, matrixStack, f, g, h);
         float jx = automatoneEntity.isInWater() ? -90.0F - automatoneEntity.getXRot() : -90.0F;
         float kx = Mth.lerp(i, 0.0F, jx);
         matrixStack.mulPose(Axis.XP.rotationDegrees(kx));
         if (automatoneEntity.isVisuallySwimming()) {
            matrixStack.translate(0.0F, -1.0F, 0.3F);
         }
      } else {
         super.setupRotations(automatoneEntity, matrixStack, f, g, h);
      }
   }
}
