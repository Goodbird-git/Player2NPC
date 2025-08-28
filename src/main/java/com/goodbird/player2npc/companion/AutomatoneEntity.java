package com.goodbird.player2npc.companion;

import altoclef.AltoClefController;
import altoclef.player2api.Character;
import altoclef.player2api.Player2APIService;
import altoclef.player2api.utils.CharacterUtils;
import altoclef.player2api.EventQueueManager;
import altoclef.player2api.Player2APIService;
import baritone.api.IBaritone;
import baritone.api.entity.IAutomatone;
import baritone.api.entity.IHungerManagerProvider;
import baritone.api.entity.IInteractionManagerProvider;
import baritone.api.entity.IInventoryProvider;
import baritone.api.entity.LivingEntityHungerManager;
import baritone.api.entity.LivingEntityInteractionManager;
import baritone.api.entity.LivingEntityInventory;
import com.goodbird.player2npc.Player2NPCForge;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class AutomatoneEntity extends LivingEntity implements IAutomatone, IInventoryProvider, IInteractionManagerProvider, IHungerManagerProvider, IEntityAdditionalSpawnData {
   public LivingEntityInteractionManager manager;
   public LivingEntityInventory inventory;
   public LivingEntityHungerManager hungerManager;
   public AltoClefController controller;
   public Character character;
   public ResourceLocation textureLocation;
   protected Vec3 lastVelocity;
   private final String PLAYER2_GAME_ID = "player2-ai-npc-minecraft";

   public AutomatoneEntity(EntityType<? extends AutomatoneEntity> type, Level world) {
      super(type, world);
      this.init();
   }

   public void init() {
      this.setMaxUpStep(0.6F);
      this.setSpeed(0.4F);
      this.manager = new LivingEntityInteractionManager(this);
      this.inventory = new LivingEntityInventory(this);
      this.hungerManager = new LivingEntityHungerManager();
      if(!this.level().isClientSide && character != null){
         this.controller = new AltoClefController(IBaritone.KEY.get(this), character, PLAYER2_GAME_ID);
         EventQueueManager.sendGreeting(this.controller, character);
      }
   }

   public AutomatoneEntity(Level world, Character character) {
      super(Player2NPCForge.AUTOMATONE.get(), world);
      this.setCharacter(character);
      this.init();
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
      this.inventory.readNbt(nbtList);
      this.inventory.selectedSlot = tag.getInt("SelectedItemSlot");
      if (this.character == null && tag.contains("character")) {
         CompoundTag compound = tag.getCompound("character");
         this.character = CharacterUtils.readFromNBT(compound);
         EventQueueManager.sendGreeting(controller, character);
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("head_yaw", this.yHeadRot);
      tag.put("Inventory", this.inventory.writeNbt(new ListTag()));
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
      this.attackStrengthTicker++;
      if (!this.level().isClientSide) {
         this.controller.serverTick();
      }

      super.tick();
      this.updateSwingTime();
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

         for (ItemEntity itemEntity : this.level()
            .getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(vec3i.getX(), vec3i.getY(), vec3i.getZ()))) {
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

   public boolean doHurtTarget(Entity target) {
      this.attackStrengthTicker = 0;
      float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float g = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      if (target instanceof LivingEntity) {
         f += EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity)target).getMobType());
         g += EnchantmentHelper.getKnockbackBonus(this);
      }

      int i = EnchantmentHelper.getFireAspect(this);
      if (i > 0) {
         target.setSecondsOnFire(i * 4);
      }

      boolean bl = target.hurt(this.damageSources().mobAttack(this), f);
      if (bl) {
         if (g > 0.0F && target instanceof LivingEntity) {
            ((LivingEntity)target)
               .knockback(g * 0.5F, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)));
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
         }

         this.doEnchantDamageEffects(this, target);
         this.setLastHurtMob(target);
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
         return slot.getType() == Type.ARMOR ? (ItemStack)this.inventory.armor.get(slot.getIndex()) : ItemStack.EMPTY;
      }
   }

   public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
      if (slot == EquipmentSlot.MAINHAND) {
         this.inventory.setItem(this.inventory.selectedSlot, stack);
      } else if (slot == EquipmentSlot.OFFHAND) {
         this.inventory.offHand.set(0, stack);
      } else if (slot.getType() == Type.ARMOR) {
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
      return this.lastVelocity.lerp(this.getDeltaMovement(), delta);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }

   public Component getDisplayName() {
      return (Component)(this.character == null ? super.getDisplayName() : Component.literal(this.character.shortName()));
   }

   @Override
   public void writeSpawnData(FriendlyByteBuf friendlyByteBuf) {
      CharacterUtils.writeToBuf(friendlyByteBuf, this.character);
      CompoundTag compound = new CompoundTag();
      compound.put("inv", this.inventory.writeNbt(new ListTag()));
      friendlyByteBuf.writeNbt(compound);
   }

   @Override
   public void readSpawnData(FriendlyByteBuf friendlyByteBuf) {
      this.character = CharacterUtils.readFromBuf(friendlyByteBuf);
      this.inventory = new LivingEntityInventory(this);
      this.inventory.readNbt(friendlyByteBuf.readNbt().getList("inv", 10));
   }
}
