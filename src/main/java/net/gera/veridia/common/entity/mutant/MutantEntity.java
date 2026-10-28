package net.gera.veridia.common.entity.mutant;

import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.mana.spell.MobGemAttack;
import net.gera.veridia.common.util.ModTags;
import net.gera.veridia.annotations.Persisted;
import net.gera.veridia.annotations.PersistedFields;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Consumer;

public class MutantEntity extends Monster {

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState attack1AnimationState = new AnimationState();
    public final AnimationState attack2AnimationState = new AnimationState();
    public final AnimationState chargeStartAnimationState = new AnimationState();
    public final AnimationState chargeImpactAnimationState = new AnimationState();
    public final AnimationState stealAnimationState = new AnimationState();
    public final AnimationState passItemAnimationState = new AnimationState();
    public final AnimationState hitWithItemAnimationState = new AnimationState();
    public final AnimationState eatAnimationState = new AnimationState();
    public final AnimationState healAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();

    public int idleVariant;

    private int attack1RemainingTicks;
    private int attack2RemainingTicks;
    private int chargeImpactRemainingTicks;
    private int stealRemainingTicks;
    private int passItemRemainingTicks;
    private int hitWithItemRemainingTicks;
    private int eatRemainingTicks;
    private int healRemainingTicks;

    private static final byte EVENT_ATTACK1 = 70;
    private static final byte EVENT_ATTACK2 = 71;
    private static final byte EVENT_CHARGE_START = 72;
    private static final byte EVENT_CHARGE_IMPACT = 73;
    private static final byte EVENT_STEAL = 74;
    private static final byte EVENT_HIT_WITH_ITEM = 75;
    private static final byte EVENT_PASS_ITEM = 76;
    private static final byte EVENT_EAT = 77;
    private static final byte EVENT_HEAL = 78;

    private static final int ATTACK1_DURATION_TICKS = 23;
    private static final int ATTACK2_DURATION_TICKS = 13;
    private static final int CHARGE_IMPACT_DURATION_TICKS = 40;
    private static final int STEAL_DURATION_TICKS = 14;
    private static final int HIT_WITH_ITEM_DURATION_TICKS = 14;
    private static final int STOLEN_ITEM_MAX_COUNT = 8;
    private static final int STOLEN_FOOD_MAX_EATS = 3;
    private static final int STOLEN_FOOD_EAT_DURATION_TICKS = 12;
    private static final int STOLEN_FOOD_EAT_INTERVAL_TICKS = 200;
    private static final int STOLEN_FOOD_SPEED_DURATION_TICKS = 100;
    private static final float STOLEN_FOOD_HEAL = 2.0F;
    private static final float STOLEN_FOOD_GOLDEN_HEAL = 4.0F;

    private static final float HEAL_TRIGGER_FRACTION = 0.5F;
    private static final float HEAL_AMOUNT_FRACTION = 0.25F;
    private static final double HEAL_RETREAT_DISTANCE = 15.0D;
    private static final double HEAL_SAFE_DISTANCE = 13.0D;
    private static final int HEAL_MAX_RETREAT_TICKS = 80;
    private static final int HEAL_ANIM_TICKS = 56;
    private static final double HEAL_RETREAT_SPEED = 2.0D;
    private static final int HEAL_SPEED_EFFECT_TICKS = 200;
    private static final int HEAL_SPEED_AMPLIFIER = 1;
    private static final int HEAL_ATTACK2_COMBO = 5;

    private static final EntityDataAccessor<Boolean> DATA_HEALED_FORM =
            SynchedEntityData.defineId(MutantEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float ATTACK1_DAMAGE = 8.0F;
    private static final float ATTACK2_DAMAGE = 6.0F;
    private static final double ATTACK2_KNOCKBACK_STRENGTH = 2.4D;
    private static final float CHARGE_IMPACT_DAMAGE = 12.0F;
    private static final double CHARGE_SPEED = 0.5D;
    private static final int CHARGE_MAX_DURATION_TICKS = 60;

    private static final double MELEE_RANGE = 3.0D;
    private static final double MELEE_RANGE_SQR = MELEE_RANGE * MELEE_RANGE;
    private static final double STEAL_RANGE = 3.5D;
    private static final double STEAL_RANGE_SQR = STEAL_RANGE * STEAL_RANGE;
    private static final double CHARGE_MIN_RANGE = 7.0D;
    private static final double CHARGE_MIN_RANGE_SQR = CHARGE_MIN_RANGE * CHARGE_MIN_RANGE;

    private static final float CHARGE_CHANCE = 8.0F;
    private static final float STEAL_CHANCE = 5.0F;
    private static final float ATTACK2_CHANCE = 15.0F;
    private static final float ATTACK1_CHANCE = 100.0F - CHARGE_CHANCE - STEAL_CHANCE - ATTACK2_CHANCE;
    private static final int ATTACK_RECOVER_TICKS = 8;
    private static final int SHORT_RETRY_TICKS = 5;
    private static final int POST_CHARGE_COOLDOWN_TICKS = 20;
    private static final double MOVE_SPEED_MODIFIER = 1.0D;

    private int actionCooldown;
    private int busyTicks;
    private int stolenItemUsesRemaining;

    @Persisted("HealUsed")
    private boolean healUsed;
    private boolean healActive;
    private boolean healInterrupted;
    private int healPhase;
    private int healAnimTicks;
    private int healRetreatTicks;
    @Persisted("Attack2ComboRemaining")
    private int attack2ComboRemaining;
    @Nullable
    private BlockPos healAnchorPos;
    @Nullable
    private Vec3 healSpot;

    private boolean charging;
    private Vec3 chargeDirection = Vec3.ZERO;
    private int chargeTicksRemaining;

    private static final float ELITE_CHANCE = 0.10F;
    private static final float INFESTED_BRAIN_DROP_CHANCE = 0.15F;
    private static final float TWIN_INFESTED_BRAIN_DROP_CHANCE = 0.025F;
    private int gemChannelTicksRemaining;
    @Nullable
    private Consumer<LivingEntity> gemChannelTick;
    @Nullable
    private Runnable gemChannelEnd;

    public MutantEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new MoveControl(this);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HEALED_FORM, false);
    }

    public boolean isHealedForm() {
        return this.entityData.get(DATA_HEALED_FORM);
    }

    private void setHealedForm(boolean value) {
        this.entityData.set(DATA_HEALED_FORM, value);
    }

    @SuppressWarnings("deprecation")
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        if (this.random.nextFloat() < ELITE_CHANCE) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.NEWBIE_MANTLE_HELMET.get()));
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.NEWBIE_MANTLE_CHESTPLATE.get()));
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.NEWBIE_MANTLE_BOOTS.get()));
            this.setDropChance(EquipmentSlot.HEAD, 0.0F);
            this.setDropChance(EquipmentSlot.CHEST, 0.0F);
            this.setDropChance(EquipmentSlot.FEET, 0.0F);
        }

        assert data != null;
        return data;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public boolean isElite() {
        return this.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.NEWBIE_MANTLE_CHESTPLATE.get());
    }

    public void startGemChannel(int durationTicks, Consumer<LivingEntity> onTick, Runnable onEnd) {
        this.gemChannelTicksRemaining = durationTicks;
        this.gemChannelTick = onTick;
        this.gemChannelEnd = onEnd;
    }

    private void tickGemChannel() {
        if (gemChannelTicksRemaining <= 0) return;

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && gemChannelTick != null) {
            gemChannelTick.accept(target);
        }

        gemChannelTicksRemaining--;
        if (gemChannelTicksRemaining <= 0) {
            if (gemChannelEnd != null) {
                gemChannelEnd.run();
            }
            gemChannelTick = null;
            gemChannelEnd = null;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, ATTACK1_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HealGoal(this));
        this.goalSelector.addGoal(2, new CombatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
    }

    private static boolean isValidTarget(LivingEntity entity) {
        return entity instanceof Player
                || entity instanceof AbstractVillager
                || entity instanceof AbstractGolem
                || entity instanceof Turtle;
    }

    private static boolean isValidCombatTarget(LivingEntity entity) {
        return isValidTarget(entity) || entity instanceof Monster;
    }

    public ItemStack getStolenItem() {
        return this.getItemBySlot(EquipmentSlot.OFFHAND);
    }

    private void setStolenItem(ItemStack stack) {
        this.setItemSlot(EquipmentSlot.OFFHAND, stack);
    }

    public boolean hasStolenItem() {
        return !getStolenItem().isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            clientAnimationTick();
        } else {
            tickGemChannel();
            tickHealTracking();
        }
    }

    private void tickHealTracking() {
        if (this.getHealth() > this.getMaxHealth() * HEAL_TRIGGER_FRACTION) {
            this.healAnchorPos = this.blockPosition();
        }
    }

    private void clientAnimationTick() {
        boolean attackPlaying = attack1AnimationState.isStarted() || attack2AnimationState.isStarted()
                || chargeStartAnimationState.isStarted() || chargeImpactAnimationState.isStarted()
                || stealAnimationState.isStarted() || passItemAnimationState.isStarted()
                || hitWithItemAnimationState.isStarted() || eatAnimationState.isStarted()
                || healAnimationState.isStarted() || deathAnimationState.isStarted();

        if (!attackPlaying) {
            boolean moving = this.walkAnimation.speed() > 1.0E-3F;
            if (moving) {
                if (!walkAnimationState.isStarted()) {
                    walkAnimationState.start(this.tickCount);
                }
                idleAnimationState.stop();
            } else {
                if (!idleAnimationState.isStarted()) {
                    idleVariant = this.random.nextInt(2);
                    idleAnimationState.start(this.tickCount);
                }
                walkAnimationState.stop();
            }
        } else {
            idleAnimationState.stop();
            walkAnimationState.stop();
        }

        attack1RemainingTicks = tickDown(attack1AnimationState, attack1RemainingTicks);
        attack2RemainingTicks = tickDown(attack2AnimationState, attack2RemainingTicks);
        chargeImpactRemainingTicks = tickDown(chargeImpactAnimationState, chargeImpactRemainingTicks);
        stealRemainingTicks = tickDown(stealAnimationState, stealRemainingTicks);
        passItemRemainingTicks = tickDown(passItemAnimationState, passItemRemainingTicks);
        hitWithItemRemainingTicks = tickDown(hitWithItemAnimationState, hitWithItemRemainingTicks);
        eatRemainingTicks = tickDown(eatAnimationState, eatRemainingTicks);
        healRemainingTicks = tickDown(healAnimationState, healRemainingTicks);
    }

    private static int tickDown(AnimationState state, int remaining) {
        if (!state.isStarted() || remaining <= 0) {
            return 0;
        }
        remaining--;
        if (remaining <= 0) {
            state.stop();
        }
        return remaining;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_ATTACK1 -> {
                attack1AnimationState.start(this.tickCount);
                attack1RemainingTicks = ATTACK1_DURATION_TICKS;
            }
            case EVENT_ATTACK2 -> {
                attack2AnimationState.start(this.tickCount);
                attack2RemainingTicks = ATTACK2_DURATION_TICKS;
            }
            case EVENT_CHARGE_START -> chargeStartAnimationState.start(this.tickCount);
            case EVENT_CHARGE_IMPACT -> {
                chargeStartAnimationState.stop();
                chargeImpactAnimationState.start(this.tickCount);
                chargeImpactRemainingTicks = CHARGE_IMPACT_DURATION_TICKS;
            }
            case EVENT_STEAL -> {
                stealAnimationState.start(this.tickCount);
                stealRemainingTicks = STEAL_DURATION_TICKS;
            }
            case EVENT_PASS_ITEM -> {
                passItemAnimationState.start(this.tickCount);
                passItemRemainingTicks = STEAL_DURATION_TICKS;
            }
            case EVENT_HIT_WITH_ITEM -> {
                hitWithItemAnimationState.start(this.tickCount);
                hitWithItemRemainingTicks = HIT_WITH_ITEM_DURATION_TICKS;
            }
            case EVENT_EAT -> {
                eatAnimationState.start(this.tickCount);
                eatRemainingTicks = STOLEN_FOOD_EAT_DURATION_TICKS;
            }
            case EVENT_HEAL -> {
                healAnimationState.start(this.tickCount);
                healRemainingTicks = HEAL_ANIM_TICKS;
            }
            default -> super.handleEntityEvent(id);
        }
    }
//server
    private void aiCombatTick(LivingEntity target) {
        if (charging) {
            tickCharge();
            return;
        }
        if (busyTicks > 0) {
            busyTicks--;
            return;
        }

        this.getNavigation().moveTo(target, MOVE_SPEED_MODIFIER);

        if (actionCooldown > 0) {
            actionCooldown--;
            return;
        }

        decideAndAct(target);
    }

    private void decideAndAct(LivingEntity target) {
        double distSqr = this.distanceToSqr(target);

        if (hasStolenItem()) {
            ItemStack stolen = getStolenItem();
            if (isEdible(stolen)) {
                performEatStolenFood(stolen);
            } else if (distSqr <= MELEE_RANGE_SQR) {
                performHitWithItem(target);
            } else {
                this.actionCooldown = SHORT_RETRY_TICKS;
            }
            return;
        }

        if (attack2ComboRemaining > 0) {
            if (distSqr <= MELEE_RANGE_SQR) {
                attack2ComboRemaining--;
                performAttack2(target);
            } else {
                this.actionCooldown = SHORT_RETRY_TICKS;
            }
            return;
        }

        float roll = this.random.nextFloat() * 100.0F;
        if (roll < CHARGE_CHANCE) {
            if (distSqr >= CHARGE_MIN_RANGE_SQR) {
                beginCharge(target);
            } else {
                this.actionCooldown = SHORT_RETRY_TICKS;
            }
            return;
        }
        roll -= CHARGE_CHANCE;

        if (roll < STEAL_CHANCE) {
            if (distSqr <= STEAL_RANGE_SQR) {
                performSteal(target);
            } else {
                this.actionCooldown = SHORT_RETRY_TICKS;
            }
            return;
        }
        roll -= STEAL_CHANCE;

        if (distSqr > MELEE_RANGE_SQR) {
            this.actionCooldown = SHORT_RETRY_TICKS;
            return;
        }

        if (roll < ATTACK1_CHANCE) {
            performAttack1(target);
        } else {
            performAttack2(target);
        }
    }

    private void performAttack1(LivingEntity target) {
        target.hurt(this.damageSources().mobAttack(this), ATTACK1_DAMAGE);
        this.level().broadcastEntityEvent(this, EVENT_ATTACK1);
        this.busyTicks = ATTACK1_DURATION_TICKS;
        this.actionCooldown = ATTACK1_DURATION_TICKS + ATTACK_RECOVER_TICKS;
    }

    private void performAttack2(LivingEntity target) {
        target.hurt(this.damageSources().mobAttack(this), ATTACK2_DAMAGE);

        Vec3 diff = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(diff.x, 0.0D, diff.z);
        Vec3 push = (horizontal.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : horizontal.normalize())
                .scale(ATTACK2_KNOCKBACK_STRENGTH);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.45D, push.z));
        target.hurtMarked = true;

        this.level().broadcastEntityEvent(this, EVENT_ATTACK2);
        this.busyTicks = ATTACK2_DURATION_TICKS;
        this.actionCooldown = ATTACK2_DURATION_TICKS + ATTACK_RECOVER_TICKS;
    }

    private void performSteal(LivingEntity target) {
        this.busyTicks = STEAL_DURATION_TICKS;
        this.actionCooldown = STEAL_DURATION_TICKS + ATTACK_RECOVER_TICKS;

        boolean stoleFood = false;
        if (target instanceof Player player && this.distanceToSqr(target) <= STEAL_RANGE_SQR) {
            ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.isEmpty()) {
                int take = Math.min(heldItem.getCount(), STOLEN_ITEM_MAX_COUNT);
                ItemStack stolen = heldItem.copyWithCount(take);
                ItemStack remainder = heldItem.copyWithCount(heldItem.getCount() - take);
                player.setItemInHand(InteractionHand.MAIN_HAND, remainder.isEmpty() ? ItemStack.EMPTY : remainder);
                setStolenItem(stolen);
                this.stolenItemUsesRemaining = stolen.is(ModTags.Items.GEM) ? 1
                        : isEdible(stolen) ? STOLEN_FOOD_MAX_EATS
                        : 3;
                stoleFood = isEdible(stolen);
            }
        }

        this.level().broadcastEntityEvent(this, stoleFood ? EVENT_PASS_ITEM : EVENT_STEAL);
    }

    private void performHitWithItem(LivingEntity target) {
        ItemStack stolen = getStolenItem();
        if (stolen.isEmpty()) return;

        this.level().broadcastEntityEvent(this, EVENT_HIT_WITH_ITEM);
        this.busyTicks = HIT_WITH_ITEM_DURATION_TICKS;
        this.actionCooldown = HIT_WITH_ITEM_DURATION_TICKS + ATTACK_RECOVER_TICKS;

        if (stolen.is(ModTags.Items.GEM) && stolen.getItem() instanceof MobGemAttack gemAttack) {
            boolean handlesOwnDrop = gemAttack.castByMob(this, target, stolen);
            if (!handlesOwnDrop) {
                dropStolenItem();
            }
            return;
        }

        target.hurt(this.damageSources().mobAttack(this), stolenItemInHandDamage(stolen));
        stolenItemUsesRemaining--;
        if (stolenItemUsesRemaining <= 0) {
            dropStolenItem();
        }
    }

    private static boolean isEdible(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    private static boolean isGoldenFood(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.is(Items.GOLDEN_CARROT);
    }

    private void performEatStolenFood(ItemStack stolen) {
        this.level().broadcastEntityEvent(this, EVENT_EAT);
        this.playSound(this.getEatingSound(stolen), 1.0F,
                1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, STOLEN_FOOD_SPEED_DURATION_TICKS, 0));
        this.heal(isGoldenFood(stolen) ? STOLEN_FOOD_GOLDEN_HEAL : STOLEN_FOOD_HEAL);

        this.busyTicks = STOLEN_FOOD_EAT_DURATION_TICKS;
        this.actionCooldown = STOLEN_FOOD_EAT_INTERVAL_TICKS;

        stolenItemUsesRemaining--;
        if (stolenItemUsesRemaining <= 0) {
            dropStolenItem();
        }
    }


    private float stolenItemInHandDamage(ItemStack stack) {
        double[] addValue = {0.0D};
        double[] addMultipliedBase = {0.0D};
        double[] addMultipliedTotal = {1.0D};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (!attribute.is(Attributes.ATTACK_DAMAGE)) {
                return;
            }
            switch (modifier.operation()) {
                case ADD_VALUE -> addValue[0] += modifier.amount();
                case ADD_MULTIPLIED_BASE -> addMultipliedBase[0] += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> addMultipliedTotal[0] *= 1.0D + modifier.amount();
            }
        });

        double damage = this.getAttributeValue(Attributes.ATTACK_DAMAGE) + addValue[0];
        damage *= 1.0D + addMultipliedBase[0];
        damage *= addMultipliedTotal[0];
        return (float) Math.max(damage, 0.0D);
    }

    public void dropStolenItem() {
        ItemStack stolen = getStolenItem();
        if (!stolen.isEmpty()) {
            this.spawnAtLocation(stolen);
        }
        setStolenItem(ItemStack.EMPTY);
        this.stolenItemUsesRemaining = 0;
    }

    public void throwStolenItemAway() {
        ItemStack stolen = getStolenItem();
        if (!stolen.isEmpty() && this.level() instanceof ServerLevel level) {
            ItemEntity itemEntity = new ItemEntity(level, this.getX(), this.getY(0.5D), this.getZ(), stolen);
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            itemEntity.setDeltaMovement(Math.cos(angle) * 1.6D, 0.45D, Math.sin(angle) * 1.6D);
            itemEntity.setPickUpDelay(10);
            level.addFreshEntity(itemEntity);
        }
        setStolenItem(ItemStack.EMPTY);
        this.stolenItemUsesRemaining = 0;
    }

    private void beginCharge(LivingEntity target) {
        Vec3 diff = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(diff.x, 0.0D, diff.z);
        this.chargeDirection = horizontal.lengthSqr() < 1.0E-4D ? Vec3.ZERO : horizontal.normalize();

        if (chargeDirection.lengthSqr() > 1.0E-4D) {
            float yaw = (float) (Mth.atan2(-chargeDirection.x, chargeDirection.z) * (180D / Math.PI));
            this.setYRot(yaw);
            this.yRotO = yaw;
            this.yBodyRot = yaw;
            this.yBodyRotO = yaw;
            this.yHeadRot = yaw;
            this.yHeadRotO = yaw;
        }

        this.charging = true;
        this.chargeTicksRemaining = CHARGE_MAX_DURATION_TICKS;
        this.getNavigation().stop();
        this.level().broadcastEntityEvent(this, EVENT_CHARGE_START);
    }

    private void tickCharge() {
        this.setDeltaMovement(chargeDirection.x * CHARGE_SPEED, this.getDeltaMovement().y, chargeDirection.z * CHARGE_SPEED);
        this.getLookControl().setLookAt(this.getX() + chargeDirection.x, this.getY(), this.getZ() + chargeDirection.z);

        boolean hitEntity = false;
        for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(0.3D), e -> e != this && e.isAlive())) {
            if (isValidTarget(nearby)) {
                nearby.hurt(this.damageSources().mobAttack(this), CHARGE_IMPACT_DAMAGE);
                Vec3 knock = new Vec3(chargeDirection.x, 0.3D, chargeDirection.z).scale(1.2D);
                nearby.setDeltaMovement(nearby.getDeltaMovement().add(knock));
                nearby.hurtMarked = true;
            }
            hitEntity = true;
            break;
        }

        chargeTicksRemaining--;
        if (hitEntity || this.horizontalCollision || chargeTicksRemaining <= 0) {
            endCharge();
        }
    }

    private void endCharge() {
        charging = false;
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        this.level().broadcastEntityEvent(this, EVENT_CHARGE_IMPACT);
        this.busyTicks = CHARGE_IMPACT_DURATION_TICKS;
        this.actionCooldown = POST_CHARGE_COOLDOWN_TICKS;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        boolean wasHurt = super.hurt(damageSource, amount);
        if (wasHurt && !this.level().isClientSide()) {
            if (charging) {
                interruptCharge();
            }
            if (healActive) {
                healInterrupted = true;
            }
        }
        return wasHurt;
    }

    private void interruptCharge() {
        charging = false;
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        this.busyTicks = 0;
        this.actionCooldown = 0;
    }

    @Override
    public void die(DamageSource damageSource) {
        if (hasStolenItem() && !this.level().isClientSide()) {
            dropStolenItem();
        }
        if (!this.level().isClientSide()) {
            dropDeathLoot();
        }
        super.die(damageSource);
    }

    @Override
    protected void tickDeath() {
        if (this.deathTime == 0 && this.level().isClientSide()) {
            this.deathAnimationState.start(this.tickCount);
        }
        super.tickDeath();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        PersistedFields.save(this, tag);
        tag.putBoolean("HealedForm", this.isHealedForm());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        PersistedFields.load(this, tag);
        this.setHealedForm(tag.getBoolean("HealedForm"));
    }

    private Vec3 computeHealSpot() {
        LivingEntity threat = this.getLastHurtByMob() != null ? this.getLastHurtByMob() : this.getTarget();
        Vec3 from = this.position();

        Vec3 away = threat != null
                ? from.subtract(threat.position())
                : Vec3.directionFromRotation(0.0F, this.getYRot()).reverse();
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        }
        away = away.normalize();

        Vec3 origin = threat != null ? threat.position() : from;
        for (float deg : new float[]{0.0F, 20.0F, -20.0F, 45.0F, -45.0F, 75.0F, -75.0F}) {
            Vec3 dir = away.yRot((float) Math.toRadians(deg));
            Vec3 candidate = origin.add(dir.scale(HEAL_RETREAT_DISTANCE));
            Path path = this.getNavigation().createPath(BlockPos.containing(candidate), 1);
            if (path != null && path.canReach()) {
                return candidate;
            }
        }
        return origin.add(away.scale(HEAL_RETREAT_DISTANCE));
    }

    private void dropDeathLoot() {
        int rottenFleshCount = this.random.nextInt(3);
        if (rottenFleshCount > 0) {
            this.spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH, rottenFleshCount));
        }
        if (this.random.nextFloat() < INFESTED_BRAIN_DROP_CHANCE) {
            this.spawnAtLocation(new ItemStack(ModItems.INFESTED_BRAIN.get()));
        }
        if (this.random.nextFloat() < TWIN_INFESTED_BRAIN_DROP_CHANCE) {
            this.spawnAtLocation(new ItemStack(ModItems.INFESTED_BRAIN.get(), 2));
        }
    }

    private static class CombatGoal extends Goal {
        private final MutantEntity mutant;

        CombatGoal(MutantEntity mutant) {
            this.mutant = mutant;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mutant.getTarget();
            return target != null && target.isAlive() && isValidCombatTarget(target);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            mutant.charging = false;
            mutant.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = mutant.getTarget();
            if (target == null) return;
            if (!mutant.charging) {
                mutant.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            mutant.aiCombatTick(target);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    private static class HealGoal extends Goal {
        private final MutantEntity mutant;

        HealGoal(MutantEntity mutant) {
            this.mutant = mutant;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (mutant.healUsed || !mutant.isAlive() || mutant.charging) {
                return false;
            }
            return mutant.getHealth() <= mutant.getMaxHealth() * HEAL_TRIGGER_FRACTION;
        }

        @Override
        public boolean canContinueToUse() {
            if (mutant.healInterrupted || mutant.healUsed || !mutant.isAlive()) {
                return false;
            }
            return mutant.healPhase == 0 || mutant.healAnimTicks < HEAL_ANIM_TICKS;
        }

        @Override
        public void start() {
            mutant.healActive = true;
            mutant.healInterrupted = false;
            mutant.healPhase = 0;
            mutant.healAnimTicks = 0;
            mutant.healRetreatTicks = 0;
            mutant.charging = false;
            mutant.busyTicks = 0;
            mutant.actionCooldown = 0;
            mutant.setTarget(null);
            mutant.setAggressive(false);
            mutant.getNavigation().stop();
            mutant.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                    HEAL_SPEED_EFFECT_TICKS, HEAL_SPEED_AMPLIFIER, false, false));
            mutant.healSpot = mutant.computeHealSpot();
        }

        @Override
        public void tick() {
            if (mutant.healPhase == 0) {
                LivingEntity threat = mutant.getLastHurtByMob() != null ? mutant.getLastHurtByMob() : mutant.getTarget();
                boolean farFromThreat = threat == null
                        || mutant.distanceToSqr(threat) >= HEAL_SAFE_DISTANCE * HEAL_SAFE_DISTANCE;

                if ((mutant.healRetreatTicks >= 10 && farFromThreat && mutant.getNavigation().isDone())
                        || mutant.healRetreatTicks >= HEAL_MAX_RETREAT_TICKS) {
                    beginHealing();
                    return;
                }
                mutant.healRetreatTicks++;

                if (mutant.healSpot == null || mutant.getNavigation().isDone()
                        || mutant.healRetreatTicks % 8 == 0) {
                    mutant.healSpot = mutant.computeHealSpot();
                }
                Vec3 spot = mutant.healSpot;
                boolean pathed = spot != null
                        && mutant.getNavigation().moveTo(spot.x, spot.y, spot.z, HEAL_RETREAT_SPEED);
                if (!pathed && threat != null) {
                    Vec3 away = mutant.position().subtract(threat.position());
                    away = new Vec3(away.x, 0.0D, away.z);
                    if (away.lengthSqr() > 1.0E-4D) {
                        Vec3 t = mutant.position().add(away.normalize().scale(6.0D));
                        mutant.getMoveControl().setWantedPosition(t.x, t.y, t.z, HEAL_RETREAT_SPEED);
                    }
                }
            } else {
                mutant.getNavigation().stop();
                mutant.healAnimTicks++;
            }
        }

        private void beginHealing() {
            mutant.healPhase = 1;
            mutant.healAnimTicks = 0;
            mutant.getNavigation().stop();
            mutant.level().broadcastEntityEvent(mutant, EVENT_HEAL);
        }

        @Override
        public void stop() {
            boolean success = !mutant.healInterrupted
                    && mutant.healPhase == 1
                    && mutant.healAnimTicks >= HEAL_ANIM_TICKS;

            mutant.healActive = false;
            mutant.healUsed = true;
            mutant.healSpot = null;

            if (success) {
                mutant.heal(mutant.getMaxHealth() * HEAL_AMOUNT_FRACTION);
                mutant.setHealedForm(true);
                mutant.attack2ComboRemaining = HEAL_ATTACK2_COMBO;
            } else {
                mutant.removeEffect(MobEffects.MOVEMENT_SPEED);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }
}