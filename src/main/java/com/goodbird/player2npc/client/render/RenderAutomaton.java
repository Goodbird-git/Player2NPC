package com.goodbird.player2npc.client.render;

import com.goodbird.player2npc.client.util.ImageDownloadAlt;
import com.goodbird.player2npc.client.util.ResourceDownloader;
import com.goodbird.player2npc.companion.AutomatoneEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.feature.StuckArrowsFeatureRenderer;
import net.minecraft.client.render.entity.feature.StuckStingersFeatureRenderer;
import net.minecraft.client.render.entity.feature.TridentRiptideFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedArmorEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.io.File;

public class RenderAutomaton extends LivingEntityRenderer<AutomatoneEntity, PlayerEntityModel<AutomatoneEntity>> {
    public RenderAutomaton(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5F);
        boolean slim = false;
        this.addFeature(new ArmorFeatureRenderer(this, new BipedArmorEntityModel(ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM_INNER_ARMOR : EntityModelLayers.PLAYER_INNER_ARMOR)), new BipedArmorEntityModel(ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR : EntityModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
        this.addFeature(new PlayerHeldItemFeatureRenderer(this, ctx.getHeldItemRenderer()));
        this.addFeature(new StuckArrowsFeatureRenderer(ctx, this));
        this.addFeature(new HeadFeatureRenderer(this, ctx.getModelLoader(), ctx.getHeldItemRenderer()));
        this.addFeature(new ElytraFeatureRenderer(this, ctx.getModelLoader()));
        this.addFeature(new TridentRiptideFeatureRenderer(this, ctx.getModelLoader()));
        this.addFeature(new StuckStingersFeatureRenderer(this));
    }

    public void render(AutomatoneEntity automatoneEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        try {
            this.setModelPose(automatoneEntity);
            super.render(automatoneEntity, f, g, matrixStack, vertexConsumerProvider, i);
        }catch (Exception ignored){}
    }

    public Vec3d getPositionOffset(AutomatoneEntity automatoneEntity, float f) {
        return automatoneEntity.isInSneakingPose() ? new Vec3d((double) 0.0F, (double) -0.125F, (double) 0.0F) : super.getPositionOffset(automatoneEntity, f);
    }

    private void setModelPose(AutomatoneEntity player) {
        PlayerEntityModel<AutomatoneEntity> playerEntityModel = (PlayerEntityModel) this.getModel();
        if (player.isSpectator()) {
            playerEntityModel.setVisible(false);
            playerEntityModel.head.visible = true;
            playerEntityModel.hat.visible = true;
        } else {
            playerEntityModel.setVisible(true);
            playerEntityModel.hat.visible = true;
            playerEntityModel.jacket.visible = true;
            playerEntityModel.leftPants.visible = true;
            playerEntityModel.rightPants.visible = true;
            playerEntityModel.leftSleeve.visible = true;
            playerEntityModel.rightSleeve.visible = true;
            playerEntityModel.sneaking = player.isInSneakingPose();
            BipedEntityModel.ArmPose armPose = getArmPose(player, Hand.MAIN_HAND);
            BipedEntityModel.ArmPose armPose2 = getArmPose(player, Hand.OFF_HAND);
            if (armPose.isTwoHanded()) {
                armPose2 = player.getOffHandStack().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
            }

            if (player.getMainArm() == Arm.RIGHT) {
                playerEntityModel.rightArmPose = armPose;
                playerEntityModel.leftArmPose = armPose2;
            } else {
                playerEntityModel.rightArmPose = armPose2;
                playerEntityModel.leftArmPose = armPose;
            }
        }

    }

    private static BipedEntityModel.ArmPose getArmPose(AutomatoneEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isEmpty()) {
            return BipedEntityModel.ArmPose.EMPTY;
        } else {
            if (player.getActiveHand() == hand && player.getItemUseTimeLeft() > 0) {
                UseAction useAction = itemStack.getUseAction();
                if (useAction == UseAction.BLOCK) {
                    return BipedEntityModel.ArmPose.BLOCK;
                }

                if (useAction == UseAction.BOW) {
                    return BipedEntityModel.ArmPose.BOW_AND_ARROW;
                }

                if (useAction == UseAction.SPEAR) {
                    return BipedEntityModel.ArmPose.THROW_SPEAR;
                }

                if (useAction == UseAction.CROSSBOW && hand == player.getActiveHand()) {
                    return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (useAction == UseAction.SPYGLASS) {
                    return BipedEntityModel.ArmPose.SPYGLASS;
                }

                if (useAction == UseAction.TOOT_HORN) {
                    return BipedEntityModel.ArmPose.TOOT_HORN;
                }

                if (useAction == UseAction.BRUSH) {
                    return BipedEntityModel.ArmPose.BRUSH;
                }
            } else if (!player.handSwinging && itemStack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack)) {
                return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
            }

            return BipedEntityModel.ArmPose.ITEM;
        }
    }

    @Override
    public Identifier getTexture(AutomatoneEntity npc) {
        if (npc.textureLocation == null) {
            try {
                boolean fixSkin = true;
                File file = ResourceDownloader.getUrlFile(npc.getCharacter().skinURL(), fixSkin);
                npc.textureLocation = ResourceDownloader.getUrlResourceLocation(npc.getCharacter().skinURL(), fixSkin);
                this.loadSkin(file, npc.textureLocation, npc.getCharacter().skinURL(), fixSkin);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return npc.textureLocation == null ? DefaultSkinHelper.getTexture() : npc.textureLocation;
    }

    private void loadSkin(File file, Identifier resource, String par1Str, boolean fix64) {
        TextureManager texturemanager = MinecraftClient.getInstance().getTextureManager();
        AbstractTexture object = texturemanager.getOrDefault(resource, (AbstractTexture) null);
        if (object == null) {
            ResourceDownloader.load(new ImageDownloadAlt(file, par1Str, resource, DefaultSkinHelper.getTexture(), fix64, () -> {
            }));
        }
    }

    protected void scale(AutomatoneEntity automatoneEntity, MatrixStack matrixStack, float f) {
        float g = 0.9375F;
        matrixStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    protected void setupTransforms(AutomatoneEntity automatoneEntity, MatrixStack matrixStack, float f, float g, float h) {
        float i = automatoneEntity.getLeaningPitch(h);
        if (automatoneEntity.isFallFlying()) {
            super.setupTransforms(automatoneEntity, matrixStack, f, g, h);
            float j = (float) automatoneEntity.getRoll() + h;
            float k = MathHelper.clamp(j * j / 100.0F, 0.0F, 1.0F);
            if (!automatoneEntity.isUsingRiptide()) {
                matrixStack.multiply(Axis.X_POSITIVE.rotationDegrees(k * (-90.0F - automatoneEntity.getPitch())));
            }

            Vec3d vec3d = automatoneEntity.getRotationVec(h);
            Vec3d vec3d2 = automatoneEntity.lerpVelocity(h);
            double d = vec3d2.horizontalLengthSquared();
            double e = vec3d.horizontalLengthSquared();
            if (d > (double) 0.0F && e > (double) 0.0F) {
                double l = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e);
                double m = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
                matrixStack.multiply(Axis.Y_POSITIVE.rotation((float) (Math.signum(m) * Math.acos(l))));
            }
        } else if (i > 0.0F) {
            super.setupTransforms(automatoneEntity, matrixStack, f, g, h);
            float j = automatoneEntity.isTouchingWater() ? -90.0F - automatoneEntity.getPitch() : -90.0F;
            float k = MathHelper.lerp(i, 0.0F, j);
            matrixStack.multiply(Axis.X_POSITIVE.rotationDegrees(k));
            if (automatoneEntity.isInSwimmingPose()) {
                matrixStack.translate(0.0F, -1.0F, 0.3F);
            }
        } else {
            super.setupTransforms(automatoneEntity, matrixStack, f, g, h);
        }

    }
}
