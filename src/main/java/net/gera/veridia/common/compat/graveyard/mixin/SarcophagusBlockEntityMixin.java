package net.gera.veridia.common.compat.graveyard.mixin;

import net.gera.veridia.common.compat.graveyard.CoffinTrapHolder;
import net.gera.veridia.common.compat.graveyard.CoffinTrapSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Блок-сущность гроба хранит ещё и данные ловушки (ключи {@link CoffinTrapSpec}): читает и пишет их вместе со своим
 * NBT, поэтому они задаются обычными /setblock, block_entity_data предмета и структурами. Наследуем BlockEntity
 * (он и так суперкласс цели), чтобы звать его setChanged.
 */
@Mixin(targets = "com.moomba.graveyard.blockentities.SarcophagusBlockEntity", remap = false)
public abstract class SarcophagusBlockEntityMixin extends BlockEntity implements CoffinTrapHolder {
    @Unique
    private final CompoundTag veridia$trap = new CompoundTag();

    // Конструктор для компилятора: миксин сам по себе никто не создаёт
    private SarcophagusBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Загрузка заменяет данные целиком: нет ключа в NBT - значит ловушки уже нет
    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = false)
    private void veridia$loadTrap(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        veridia$forgetTrap();
        for (String key : CoffinTrapSpec.TRAP_KEYS) {
            Tag value = tag.get(key);
            if (value != null) {
                veridia$trap.put(key, value.copy());
            }
        }
    }

    @Unique
    private void veridia$forgetTrap() {
        CoffinTrapSpec.TRAP_KEYS.forEach(veridia$trap::remove);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = false)
    private void veridia$saveTrap(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        tag.merge(veridia$trap);
    }

    @Override
    public CompoundTag getCoffinTrap() {
        return veridia$trap;
    }

    @Override
    public void clearCoffinTrap() {
        veridia$forgetTrap();
        setChanged();
    }
}
