package com.goodbird.player2npc.companion;

import adris.altoclef.AltoClefController;
import adris.altoclef.player2api.EventQueueManager;
import adris.altoclef.player2api.Character;
import adris.altoclef.player2api.utils.CharacterUtils;
import baritone.api.IBaritone;
import baritone.api.entity.IAutomatone;
import baritone.api.entity.IHungerManagerProvider;
import baritone.api.entity.IInteractionManagerProvider;
import baritone.api.entity.IInventoryProvider;
import baritone.api.entity.LivingEntityHungerManager;
import baritone.api.entity.LivingEntityInteractionManager;
import baritone.api.entity.LivingEntityInventory;
import com.goodbird.player2npc.Player2NPC;
import com.goodbird.player2npc.network.AutomatonSpawnPacket;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * We implement:
 * - IAutomatone for this entity to be counted as an automatone (used for
 * tracking nearby automatons and such)
 * - IInventoryProvider for this entity to have a player-like inventory
 * - IInteractionManagerProvider for this entity to have a player-like
 * interaction manager (for breaking and placing blocks and using items)
 * - IHungerManagerProvider for this entity to have a hunger manager
 */
public class AutomatoneEntity extends LivingEntity
        implements IAutomatone, IInventoryProvider, IInteractionManagerProvider, IHungerManagerProvider {
    // Fields to store the provided instances of inventory and such
    public LivingEntityInteractionManager manager;
    public LivingEntityInventory inventory;
    public LivingEntityHungerManager hungerManager;

    // Main automatone controller
    public AltoClefController controller;

    // Player2 Character
    public Character character;

    // An identifier of a loading texture (used in rendering)
    public Identifier textureLocation;

    // Previous motion (used in rendering)
    protected Vec3d lastVelocity;

    // A final field for defining your game id
    private final String PLAYER2_GAME_ID = "player2-ai-npc-minecraft";

    // Standard constructor for entity registering
    public AutomatoneEntity(EntityType<? extends AutomatoneEntity> type, World world) {
        super(type, world);
        init();
    }

    public void init() {
        // We set its speed and step height
        this.setStepHeight(0.6f);
        setMovementSpeed(0.4f);
        // Initialize the provided managers and such
        manager = new LivingEntityInteractionManager(this);
        inventory = new LivingEntityInventory(this);
        hungerManager = new LivingEntityHungerManager();
        // We initialize the altoclef controller ONLY ON CLIENT SIDE!
        if (!getWorld().isClient && character != null) {
            this.controller = new AltoClefController(IBaritone.KEY.get(this), character, PLAYER2_GAME_ID);
            EventQueueManager.sendGreeting(this.controller, character);
        }
    }

    // Constructor for the manual entity creation
    public AutomatoneEntity(World world, Character character) {
        super(Player2NPC.AUTOMATONE, world);
        setCharacter(character); // If we got a character, we store it
        init();
    }

    // Interface implementation (just make the getters for the managers and the
    // inventory)
    @Override
    public LivingEntityInventory getLivingInventory() {
        return inventory;
    }

    @Override
    public LivingEntityInteractionManager getInteractionManager() {
        return manager;
    }

    @Override
    public LivingEntityHungerManager getHungerManager() {
        return hungerManager;
    }

    // We implement NBT read and write methods
    @Override
    public void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        if (tag.contains("head_yaw")) {
            this.headYaw = tag.getFloat("head_yaw");
        }
        NbtList nbtList = tag.getList("Inventory", 10);
        this.inventory.readNbt(nbtList);
        this.inventory.selectedSlot = tag.getInt("SelectedItemSlot");
        if (character == null && tag.contains("character")) { // If we have a character stored, we read it and
                                                              // initialize the controller with it
            NbtCompound compound = tag.getCompound("character");
            character = CharacterUtils.readFromNBT(compound);
            EventQueueManager.sendGreeting(controller, character);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        tag.putFloat("head_yaw", this.headYaw);
        tag.put("Inventory", this.inventory.writeNbt(new NbtList()));
        tag.putInt("SelectedItemSlot", this.inventory.selectedSlot);
        if (character != null) {
            NbtCompound compound = new NbtCompound();
            CharacterUtils.writeToNBT(compound, character);
            tag.put("character", compound);
        }
    }

    // We tick all the managers and stuff
    @Override
    public void tick() {
        this.lastVelocity = this.getVelocity(); // Setting prev velocity for rendering
        manager.update();
        inventory.updateItems();
        // hungerManager.update(this); //if you want your automatone to feel hunger -
        // you need to uncomment that
        lastAttackedTicks++; // Tick this for the NPC to attack (LivingEntities don't do that by default)
        if (!this.getWorld().isClient) // We tick the controller only on server side
            controller.serverTick();
        super.tick();
        tickHandSwing(); // For arm swing rendering
    }

    // We tweak motion a little bit
    @Override
    public void tickMovement() {
        if (this.isTouchingWater() && this.isSneaking() && this.shouldSwimInFluids()) {
            this.knockDownwards();
        }
        super.tickMovement();
        this.headYaw = this.getYaw();
        pickupItems(); // And tick the item pickup
    }

    // Pickup all the items in range of 3 blocks
    public void pickupItems() {
        if (!this.getWorld().isClient && this.isAlive() && !this.dead
                && this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            Vec3i vec3i = new Vec3i(3, 3, 3);
            for (ItemEntity itemEntity : this.getWorld().getNonSpectatingEntities(ItemEntity.class, this
                    .getBoundingBox().expand((double) vec3i.getX(), (double) vec3i.getY(), (double) vec3i.getZ()))) {
                if (!itemEntity.isRemoved() && !itemEntity.getStack().isEmpty() && !itemEntity.cannotPickup()) {
                    ItemStack itemStack = itemEntity.getStack();
                    int i = itemStack.getCount();
                    if (this.getLivingInventory().insertStack(itemStack)) {
                        this.sendPickup(itemEntity, i);
                        if (itemStack.isEmpty()) {
                            itemEntity.discard();
                            itemStack.setCount(i);
                        }
                    }
                }
            }
        }
    }

    // Attacking function (LivingEntities don't attack by default)
    @Override
    public boolean tryAttack(Entity target) {
        lastAttackedTicks = 0;
        float f = (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        float g = (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_KNOCKBACK);
        if (target instanceof LivingEntity) {
            f += EnchantmentHelper.getAttackDamage(this.getMainHandStack(), ((LivingEntity) target).getGroup());
            g += (float) EnchantmentHelper.getKnockback(this);
        }

        int i = EnchantmentHelper.getFireAspect(this);
        if (i > 0) {
            target.setOnFireFor(i * 4);
        }

        boolean bl = target.damage(this.getDamageSources().mobAttack(this), f);
        if (bl) {
            if (g > 0.0F && target instanceof LivingEntity) {
                ((LivingEntity) target).takeKnockback((double) (g * 0.5F),
                        (double) MathHelper.sin(this.getYaw() * ((float) Math.PI / 180F)),
                        (double) (-MathHelper.cos(this.getYaw() * ((float) Math.PI / 180F))));
                this.setVelocity(this.getVelocity().multiply(0.6, (double) 1.0F, 0.6));
            }

            this.applyDamageEffects(this, target);
            this.onAttacking(target);
        }

        return bl;
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        if (this.velocityModified) {
            super.takeKnockback(strength, x, z);
        }
    }

    // Inventory of abstract methods from LivingEntity
    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return getLivingInventory().armor;
    }

    public ItemStack getEquippedStack(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.inventory.getMainHandStack();
        } else if (slot == EquipmentSlot.OFFHAND) {
            return this.inventory.offHand.get(0);
        } else {
            return slot.getType() == EquipmentSlot.Type.ARMOR ? this.inventory.armor.get(slot.getEntitySlotId())
                    : ItemStack.EMPTY;
        }
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            this.inventory.setStack(this.inventory.selectedSlot, stack);
        } else if (slot == EquipmentSlot.OFFHAND) {
            this.inventory.offHand.set(0, stack);
        } else if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            inventory.armor.set(slot.getEntitySlotId(), stack);
        }
    }

    // Useful getters
    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    // For rendering
    public Vec3d lerpVelocity(float delta) {
        return this.lastVelocity.lerp(this.getVelocity(), (double) delta);
    }

    // Override the spawning packet
    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return AutomatonSpawnPacket.create(this);
    }

    // Override the name to be taken from the character instance
    @Override
    public Text getDisplayName() {
        if (character == null) {
            return super.getDisplayName();
        }
        return Text.literal(character.shortName());
    }
}
