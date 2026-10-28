package net.gera.veridia.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

    /**
 * Common NBT key prefix for every {@link Persisted} field declared in this class. The final key is
 * {@code value() + "." + fieldKey}, so
 *
 * <pre>{@code
 * @PersistedPrefix("cooking_table")
 * public class CookingTableBlockEntity ... {
 *     @Persisted("progress") private int progress;   // key: cooking_table.progress
 * }
 * }</pre>
 *
 * <p>All {@code @Persisted} fields of one object share a single {@code CompoundTag} with vanilla
 * keys, so a prefix is what keeps them from colliding. Writing it once on the class beats repeating
 * it in every field and makes the rule automatic instead of a habit.
 *
 * <p>Applies only to fields <b>declared in the annotated class itself</b> - it is deliberately not
 * inherited, so a subclass can never silently rewrite the keys of its parent's fields (that would
 * break existing saves).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PersistedPrefix {

    /** Prefix without the trailing dot, e.g. {@code "cooking_table"}. */
    String value();
}