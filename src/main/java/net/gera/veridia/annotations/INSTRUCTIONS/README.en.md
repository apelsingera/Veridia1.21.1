# `@Persisted` — save fields to the world without handwritten NBT

*(Русская версия: [README.ru.md](README.ru.md))*

## Why

When a block entity or a mob has some state — crafting progress, who the owner is, whether it's
on, which mode it's in — that state must be **saved to the world**, otherwise it's lost when you
quit the game or the chunk unloads.

Normally you write this by hand:
```java
// OLD way — by hand, in TWO places, easy to get wrong
@Override
private void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    tag.putInt("progress", progress);
    tag.putInt("max_progress", maxProgress);
    tag.putBoolean("active", active);
    super.saveAdditional(tag, registries);
}

@Override
private void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    progress = tag.getInt("progress");
    maxProgress = tag.getInt("max_progress");
    active = tag.getBoolean("active");
}
```

Three common bugs here:

1. You add a new field — you forget to add it **in both** methods.
2. The key drifts between `save` and `load` (`"max_progress"` vs `"maxprogress"`).
3. Wrong accessor for the type (`getLong` instead of `getInt`).

`@Persisted` removes all of this: mark a field with the annotation, and it writes and reads itself.

```java
// NEW way
@Persisted("progress")      private int progress;
@Persisted("max_progress")  private int maxProgress = 200;
@Persisted("active")        private boolean active;

@Override
private void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    PersistedFields.save(this, tag);
    super.saveAdditional(tag, registries);
}

@Override
private void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    PersistedFields.load(this, tag);
}
```

---

## How to use it — 3 steps

### Step 1. Mark the fields

Put `@Persisted("key_name")` above every field that must be saved:

```java
import net.gera.veridia.annotations.Persisted;

@Persisted("altar.progress")
private int progress = 0;
```

The key (`"altar.progress"`) is the name the value is stored under in the world file. Full key
rules are below, but short version: **pick a meaningful, prefixed key and never change it.**

### Step 2. Call `save` and `load`

Once per class, in the save/load methods, next to `super.*`:

```java
import net.gera.veridia.annotations.PersistedFields;

// block entity:

private void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    PersistedFields.save(this, tag);
    super.saveAdditional(tag, registries);
}

private void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    PersistedFields.load(this, tag);
}

// mob (Entity):
public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    PersistedFields.save(this, tag);
}

public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    PersistedFields.load(this, tag);
}
```

`this` is your block entity or mob. `tag` is the `CompoundTag` passed into the method.
Order relative to `super.*` doesn't matter, **but you must call `super.*`** (otherwise you break
the inventory, the mob's custom name, and other vanilla state).

### Step 3. Rebuild the mod

The annotation is read at runtime, but it only ends up in the code at compile time. Added or
changed a `@Persisted` — **rebuild** (`gradlew build` / restart the dev environment).

That's it. Nothing else to write — not in `save`, not in `load`.

---

## Full example: a new block entity

```java
package net.gera.veridia.common.block.entity.types;

import net.gera.veridia.annotations.Persisted;
import net.gera.veridia.annotations.PersistedFields;
import net.gera.veridia.annotations.PersistedPrefix;
import net.gera.veridia.common.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@PersistedPrefix("altar")   // every key below gets "altar." prepended
public class AltarBlockEntity extends BlockEntity {

    public enum Mode {IDLE, CHARGING, READY}

    // Inventory — @Persisted does NOT touch it, save it as usual (see save/load below).
    public final ItemStackHandler itemHandler = new ItemStackHandler(3);

    // These fields save and load themselves:
    @Persisted("progress")
    private int progress;
    @Persisted("max_progress")
    private int maxProgress = 200;
    @Persisted("active")
    private boolean active;
    @Persisted("mode")
    private Mode mode = Mode.IDLE;
    @Persisted("owner")
    @Nullable
    private UUID ownerId;
    @Persisted("link")
    @Nullable
    private BlockPos linkedPos;

    // Field WITHOUT the annotation — transient, not written to the world (resets on relog). That's intended.
    private int soundCooldown;

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALTAR_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries)); // inventory — by hand
        PersistedFields.save(this, tag);                            // everything @Persisted — automatic
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        PersistedFields.load(this, tag);
    }
}
```

---

## Supported field types

| Category    | Types                                                                                 |
|-------------|---------------------------------------------------------------------------------------|
| Numbers     | `int`, `long`, `short`, `byte`, `float`, `double` (and wrappers `Integer`, `Long`, …) |
| Boolean     | `boolean` / `Boolean`                                                                 |
| Text        | `String`                                                                              |
| Identifiers | `java.util.UUID`, `net.minecraft.resources.ResourceLocation`                          |
| Coordinates | `net.minecraft.core.BlockPos`                                                         |
| Enums       | any of your `enum` types                                                              |

**Not supported** (an error is logged, the field is simply not saved):

- Lists, arrays, `Map`, `Set`.
- `ItemStackHandler` inventories, `FluidTank` tanks, individual `ItemStack`s.
- Any custom classes / objects.

Serialize those by hand next to the `PersistedFields.save/load` call — like `"inventory"` in the
example above.

---

## Rules (important, but simple)

1. **The field must not be `static`.** `@Persisted` is about the state of one block/mob, not the
   whole class. Put it on a `static` field → log: `@Persisted on a static field — ignored`.

2. **The field must not be `final`.** Loading changes the field's value, and `final` forbids that.
   Put it on a `final` field → it is dropped when the class is scanned (once, not on every save)
   and **never written to the world at all**: a key that can never be read back is only bloat.
   Log: `@Persisted on a final field — ignored`.

3. **Write the default right on the field:** `private int maxProgress = 200;`. If the key is
   missing from the world file (first time the block runs, an old save without this field), `load`
   **leaves the field alone**, so it stays `200`. If you don't write a default, it stays
   `0` / `false` / `null`.

4. **Renaming a key? Keep the old one in `aliases`.** The key is the "address" of the value in the
   world file, and old worlds only know the previous name. So rename like this:

   ```java
   @Persisted(value = "charge", aliases = "progress")
   private int charge;
   ```

   Aliases are read on load when the main key is absent from the tag, and never written — so the
   world migrates to the new name on its own. The Java field name is free to change; the key does
   not depend on it.

5. **Prefix your keys — put `@PersistedPrefix` on the class.** All `@Persisted` fields of one
   object are written into a single shared `CompoundTag` alongside vanilla keys, so a prefix is
   what keeps them apart. No need to repeat it in every field:

   ```java
   @PersistedPrefix("altar")
   public class AltarBlockEntity extends BlockEntity {
       @Persisted("progress") private int progress; }
      // key: altar.progress
   ```

   The prefix applies only to fields **declared in that class itself** — it is not inherited, so a
   subclass can never silently rewrite its parent's keys and break existing saves.

6. **`@Persisted` with no argument** — the key becomes the field name (`@Persisted int foo` → key
   `"foo"`). Quick for a draft, but for real code prefer an explicit key (see rule 4).

7. **`null` is not saved.** If `@Nullable UUID ownerId == null`, nothing is written, and on load
   the field is left as-is. This is normal.

8. **Always call `super.*`.** `PersistedFields` only replaces your own `put*/get*`, not the
   vanilla part.

9. **Rebuild the mod** after adding or changing annotations.

---

## What `@Persisted` does NOT do

- **It does not sync to the client by itself.** That's a separate topic (`SynchedEntityData` for
  mobs, `ContainerData` / `getUpdateTag` for block entities). `@Persisted` is only about saving to
  the world.
  *Note:* if a block entity's `getUpdateTag` returns `saveWithoutMetadata(...)` (a common
  pattern), then `@Persisted` fields ride along to the client too — usually that's fine.
- **It does not save inventories and fluids.** See the table above.
- **It does not make a field a "setting".** This is not config; the value lives in one specific
  block/mob in the world.

---

## If something isn't being saved — check the log

Every message starts with `[persisted]`:

| Log message                                                     | Meaning                                                                                                          | Fix                                                                                             |
|-----------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `@Persisted on a static field — ignored`                        | field is `static`                                                                                                | remove `static`                                                                                 |
| `@Persisted on a final field — ignored`                         | field is `final`                                                                                                 | remove `final`                                                                                  |
| `type X is not supported`                                       | type not in the table                                                                                            | save that field by hand                                                                         |
| `duplicate key 'K': A and B`                                    | two fields write under the same key (easy to hit through inheritance)                                            | give them different keys, or split them with `@PersistedPrefix`                                 |
| `key 'K' holds a different tag type in the save — default kept` | the world stores a different tag kind under that key — usually because the field's type changed between versions | the value is not picked up and the default is kept; use a new key with the old one in `aliases` |
| `unknown enum constant 'FOO'`                                   | the save has an enum value that no longer exists in code                                                         | you renamed/removed an enum constant — the field stays at its default                           |
| `saving X.Y failed` / `loading X.Y failed`                      | something unexpected                                                                                             | read the stack trace below the message                                                          |

The first four are printed **at game startup**, before you even load a world: a self-check runs at
`FMLCommonSetupEvent` over every class in the mod that has `@Persisted` (see `PersistedSelfCheck`).
Look for:

```
[persisted] self-check: 5 class(es) OK
```

The class list comes from the mod file's scan data, so nothing has to be listed by hand — a new
class with `@Persisted` joins the check on its own.

Nothing in the log but the value still isn't saved — check: **did you call
`PersistedFields.save/load`**, **did you rebuild the mod**, **is the key correct** (matches the
one in the old save).

---

## How it works internally (optional reading)

`PersistedFields.save/load` uses reflection to walk every field of the class (and its
superclasses), takes the ones marked `@Persisted`, and calls the right `tag.putX` / `tag.getX`
based on the field type. The field list is computed once per class and cached. An error on one
field is logged and does not affect the others.

Everything visible without an instance — `static`/`final`, unsupported type, duplicate key — is
checked while scanning the class rather than on every save, and such fields never reach the world
file. Keys (prefix and `aliases` included) are resolved there too and cached as ready strings.

On load the **tag kind** is checked before assigning. That is not paranoia: vanilla `tag.getInt`
on a string tag silently returns `0`, so without the check a live default would be overwritten
with zero and nothing would be logged.

Files: [`Persisted.java`](../Persisted.java) — the annotation itself,
[`PersistedPrefix.java`](../PersistedPrefix.java) — per-class key prefix,
[`PersistedFields.java`](../PersistedFields.java) — the logic,
[`PersistedSelfCheck.java`](../PersistedSelfCheck.java) — the startup self-check.
