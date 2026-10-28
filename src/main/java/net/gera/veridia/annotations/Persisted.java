package net.gera.veridia.annotations;

import  java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity / block entity field that {@link PersistedFields#save} and
 * {@link PersistedFields#load} write to and read back from NBT automatically - instead of manual
 * {@code tag.putX/getX} in {@code addAdditionalSaveData}/{@code readAdditionalSaveData} (or
 * {@code saveAdditional}/{@code loadAdditional}).
 *
 * <p>Supported types: primitives and their wrappers, {@link String}, {@link java.util.UUID},
 * {@link net.minecraft.core.BlockPos}, {@link net.minecraft.resources.ResourceLocation}, any {@code enum}.
 *
 * <p>A {@code null} object field is not written. A key missing from the tag leaves the field as-is
 * on load (its initializer value) - a safe migration path for old saves.
 *
 * <p>{@code static} and {@code final} fields are rejected at scan time and never saved: a final
 * field could never be written back on load, so persisting it would only bloat the save file.
 *
 * <p>NOT for collections / inventories ({@code ItemStackHandler}, {@code FluidTank}, etc.) and NOT
 * for client sync - that is {@code SynchedEntityData}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Persisted {
    /** NBT key. Empty — the field name is used. Prefixed by {@link PersistedPrefix} when the declaring class has one. */
    String value() default "";

    /**
     * Old keys this field used to be saved under. Read on load when {@link #value} is absent from
     * the tag; never written. Lets a key be renamed without losing existing worlds — put the
     * previous key here and old saves keep loading.
     *
     * <p>Prefixed by {@link PersistedPrefix} exactly like {@link #value}, so a rename inside a
     * prefixed class only needs the short old name.
     */
    String[] aliases() default {};
}