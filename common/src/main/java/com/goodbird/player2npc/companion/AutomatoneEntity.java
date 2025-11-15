//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.goodbird.player2npc.companion;

import com.player2.playerengine.player2api.Character;
import com.goodbird.player2npc.Player2NPC;
import com.goodbird.player2npc.network.AutomatonSpawnPacket;
import com.player2.playerengine.PlayerEngineController;
import com.player2.playerengine.automaton.api.IBaritone;
import com.player2.playerengine.automaton.api.entity.IAutomatone;
import com.player2.playerengine.automaton.api.entity.IHungerManagerProvider;
import com.player2.playerengine.automaton.api.entity.IInteractionManagerProvider;
import com.player2.playerengine.automaton.api.entity.IInventoryProvider;
import com.player2.playerengine.automaton.api.entity.LivingEntityHungerManager;
import com.player2.playerengine.automaton.api.entity.LivingEntityInteractionManager;
import com.player2.playerengine.automaton.api.entity.LivingEntityInventory;
import com.player2.playerengine.player2api.manager.ConversationManager;
import com.player2.playerengine.player2api.utils.CharacterUtils;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AutomatoneEntity extends LivingEntity implements IAutomatone, IInventoryProvider, IInteractionManagerProvider, IHungerManagerProvider {
    public LivingEntityInteractionManager manager;
    public LivingEntityInventory inventory;
    public LivingEntityHungerManager hungerManager;
    public PlayerEngineController controller;
    public Character character;
    public ResourceLocation textureLocation;
    protected Vec3 lastVelocity;
    private final String PLAYER2_GAME_ID = "player2-ai-npc-minecraft";

    public AutomatoneEntity(EntityType<? extends AutomatoneEntity> type, Level world) {
        super(type, world);
        this.init();
    }

    public void init() {
//        this.setMaxUpStep(0.6F);
        this.setSpeed(0.4F);
        this.manager = new LivingEntityInteractionManager(this);
        this.inventory = new LivingEntityInventory(this);
        this.hungerManager = new LivingEntityHungerManager();
        if (!this.level().isClientSide && this.character != null) {
            this.controller = new PlayerEngineController((IBaritone)IBaritone.KEY.get(this), this.character, "player2-ai-npc-minecraft");
            ConversationManager.sendGreeting(this.controller, this.character);
        }

    }

    public AutomatoneEntity(Level world, Character character, Player owner) {
        super(Player2NPC.AUTOMATONE.get(), world);
        this.setCharacter(character);
        this.init();
        this.controller.setOwner(owner);
    }

    public LivingEntityInventory getLivingInventory() {
        return this.inventory;
    }

    public LivingEntityInteractionManager getInteractionManager() {
        return this.manager;
    }

    public LivingEntityHungerManager getHungerManager() {
        return this.hungerManager;
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("head_yaw")) {
            this.yHeadRot = tag.getFloat("head_yaw");
        }

        ListTag nbtList = tag.getList("Inventory", 10);
        this.inventory.readNbt(level().registryAccess(), nbtList);
        this.inventory.selectedSlot = tag.getInt("SelectedItemSlot");
        if (this.character == null && tag.contains("character")) {
            CompoundTag compound = tag.getCompound("character");
            this.character = CharacterUtils.readFromNBT(compound);
            if (this.controller == null) {
                this.controller = new PlayerEngineController((IBaritone)IBaritone.KEY.get(this), this.character, "player2-ai-npc-minecraft");
            }

            ConversationManager.sendGreeting(this.controller, this.character);
        }

    }

    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("head_yaw", this.yHeadRot);
        tag.put("Inventory", this.inventory.writeNbt(level().registryAccess(), new ListTag()));
        tag.putInt("SelectedItemSlot", this.inventory.selectedSlot);
        if (this.character != null) {
            CompoundTag compound = new CompoundTag();
            CharacterUtils.writeToNBT(compound, this.character);
            tag.put("character", compound);
        }

    }

    public void tick() {
        this.lastVelocity = this.getDeltaMovement();
        this.manager.update();
        this.inventory.updateItems();
        ++this.attackStrengthTicker;
        if (!this.level().isClientSide) {
            this.controller.serverTick();
        }

        super.tick();
        this.updateSwingTime();
        this.hungerManager.update(this);
    }

    public void aiStep() {
        if (this.isInWater() && this.isShiftKeyDown() && this.isAffectedByFluids()) {
            this.goDownInWater();
        }

        super.aiStep();
        this.yHeadRot = this.getYRot();
        this.pickupItems();
    }

    public void pickupItems() {
        if (!this.level().isClientSide && this.isAlive() && !this.dead && this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            Vec3i vec3i = new Vec3i(3, 3, 3);

            for(ItemEntity itemEntity : this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate((double)vec3i.getX(), (double)vec3i.getY(), (double)vec3i.getZ()))) {
                if (!itemEntity.isRemoved() && !itemEntity.getItem().isEmpty() && !itemEntity.hasPickUpDelay()) {
                    ItemStack itemStack = itemEntity.getItem();
                    int i = itemStack.getCount();
                    if (this.getLivingInventory().insertStack(itemStack)) {
                        this.take(itemEntity, i);
                        if (itemStack.isEmpty()) {
                            itemEntity.discard();
                            itemStack.setCount(i);
                        }
                    }
                }
            }
        }

    }

    public boolean doHurtTarget(Entity entity) {
        float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSource damageSource = this.damageSources().mobAttack(this);
        Level l = this.level();
        if (l instanceof ServerLevel serverLevel) {
            f = EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), entity, damageSource, f);
        }

        boolean bl = entity.hurt(damageSource, f);
        if (bl) {
            float g = this.getKnockback(entity, damageSource);
            if (g > 0.0F && entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entity;
                livingEntity.knockback((double)(g * 0.5F), (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))));
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, (double)1.0F, 0.6));
            }

            Level var7 = this.level();
            if (var7 instanceof ServerLevel) {
                ServerLevel serverLevel2 = (ServerLevel)var7;
                EnchantmentHelper.doPostAttackEffects(serverLevel2, entity, damageSource);
            }

            this.setLastHurtMob(entity);
        }

        return bl;
    }

    public void knockback(double strength, double x, double z) {
        if (this.hurtMarked) {
            super.knockback(strength, x, z);
        }

    }

    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    public Iterable<ItemStack> getArmorSlots() {
        return this.getLivingInventory().armor;
    }

    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.inventory.getMainHandStack();
        } else if (slot == EquipmentSlot.OFFHAND) {
            return (ItemStack)this.inventory.offHand.get(0);
        } else {
            return slot.getType() == Type.HUMANOID_ARMOR ? (ItemStack)this.inventory.armor.get(slot.getIndex()) : ItemStack.EMPTY;
        }
    }

    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            this.inventory.setItem(this.inventory.selectedSlot, stack);
        } else if (slot == EquipmentSlot.OFFHAND) {
            this.inventory.offHand.set(0, stack);
        } else if (slot.getType() == Type.HUMANOID_ARMOR) {
            this.inventory.armor.set(slot.getIndex(), stack);
        }

    }

    public Character getCharacter() {
        return this.character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public Vec3 lerpVelocity(float delta) {
        return this.lastVelocity.lerp(this.getDeltaMovement(), (double)delta);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return AutomatonSpawnPacket.create(level().registryAccess(), this);
    }

    public Component getDisplayName() {
        return (Component)(this.character == null ? super.getDisplayName() : Component.literal(this.character.shortName()));
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel serverLevel, DamageSource damageSource) {
        super.dropAllDeathLoot(serverLevel, damageSource);
        inventory.dropAll();
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if(!level().isClientSide()) {
            controller.getOwner().sendSystemMessage(Component.literal("Your companion "+character.shortName()+" died!"));
            controller.getOwner().sendSystemMessage(Component.literal("It was respawned near you!"));
            CompanionManager.get((ServerPlayer) controller.getOwner()).spawnCompanion(character);
        }
    }
}
