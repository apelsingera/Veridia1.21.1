# `@Persisted` — сохранение полей в мир без ручного NBT

*(English version: [README.en.md](README.en.md))*

## Зачем это

Когда у блок-сущности (block entity) или у моба есть какое-то состояние — прогресс крафта,
кто владелец, включён ли, какой режим — это состояние надо **сохранять в мир**, иначе после
выхода из игры / выгрузки чанка оно потеряется.

Обычно это пишут руками:

```java
// СТАРЫЙ способ — руками, в ДВУХ местах, легко ошибиться
@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    tag.putInt("progress", progress);
    tag.putInt("max_progress", maxProgress);
    tag.putBoolean("active", active);
    super.saveAdditional(tag, registries);
}

@Override
protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    progress = tag.getInt("progress");
    maxProgress = tag.getInt("max_progress");
    active = tag.getBoolean("active");
}
```

Здесь три частых бага:

1. Добавил новое поле — забыл дописать его **в оба** метода.
2. Ключ в `save` и `load` разъехался (`"max_progress"` против `"maxprogress"`).
3. Не тот метод под тип (`getLong` вместо `getInt`).

`@Persisted` убирает это: помечаешь поле аннотацией — оно само пишется и читается.

```java
// НОВЫЙ способ
@Persisted("progress")      private int progress;
@Persisted("max_progress")  private int maxProgress = 200;
@Persisted("active")        private boolean active;

@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    PersistedFields.save(this, tag);
    super.saveAdditional(tag, registries);
}

@Override
protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    PersistedFields.load(this, tag);
}
```

---

## Как пользоваться — 3 шага

### Шаг 1. Пометь поля

Над каждым полем, которое надо сохранять, поставь `@Persisted("имя_ключа")`:

```java
import net.gera.veridia.annotations.Persisted;

@Persisted("altar.progress")
private int progress = 0;
```

Ключ (`"altar.progress"`) — это имя, под которым значение ляжет в файл мира. Правила по ключам
ниже, но коротко: **пиши осмысленный ключ с префиксом и больше его не меняй.**

### Шаг 2. Позови `save` и `load`

Один раз на класс, в методах сохранения/загрузки, рядом с `super.*`:

```java
import net.gera.veridia.annotations.PersistedFields;

// block entity:

protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
   PersistedFields.save(this, tag);
   super.saveAdditional(tag, registries);
}

protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
   super.loadAdditional(tag, registries);
   PersistedFields.load(this, tag);
}

// моб (Entity):
public void addAdditionalSaveData(CompoundTag tag) {
   super.addAdditionalSaveData(tag);
   PersistedFields.save(this, tag);
}

public void readAdditionalSaveData(CompoundTag tag) {
   super.readAdditionalSaveData(tag);
   PersistedFields.load(this, tag);
}
```

`this` — это твоя блок-сущность или моб. `tag` — тот `CompoundTag`, что пришёл в метод.
Порядок относительно `super.*` не важен, **но сам `super.*` вызвать обязательно** (иначе сломается
инвентарь, имя моба и прочее ванильное).

### Шаг 3. Пересобери мод

Аннотация читается во время игры, но попадает в код только при компиляции. Добавил/поменял
`@Persisted` — **пересобери** (`gradlew build` / перезапуск среды).

Всё. Больше ничего писать не нужно — ни в `save`, ни в `load`.

---

## Полный пример: новая блок-сущность

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
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@PersistedPrefix("altar")   // ко всем ключам ниже добавится "altar."
public class AltarBlockEntity extends BlockEntity {

   public enum Mode {IDLE, CHARGING, READY}

   // Инвентарь — @Persisted его НЕ трогает, сохраняем как обычно (см. save/load ниже).
   public final ItemStackHandler itemHandler = new ItemStackHandler(3);

   // Эти поля сохранятся и загрузятся сами:
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

   // Поле БЕЗ аннотации — временное, в мир не пишется (сбрасывается при перезаходе). Так и надо.
   private int soundCooldown;

   public AltarBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.ALTAR_BE.get(), pos, state);
   }

   @Override
   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      tag.put("inventory", itemHandler.serializeNBT(registries)); // инвентарь — руками
      PersistedFields.save(this, tag);                            // всё @Persisted — автоматом
      super.saveAdditional(tag, registries);
   }

   @Override
   protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
      PersistedFields.load(this, tag);
   }
}
```

---

## Какие типы полей поддерживаются

| Категория | Типы |
|---|---|
| Числа | `int`, `long`, `short`, `byte`, `float`, `double` (и обёртки `Integer`, `Long`, …) |
| Логика | `boolean` / `Boolean` |
| Текст | `String` |
| Идентификаторы | `java.util.UUID`, `net.minecraft.resources.ResourceLocation` |
| Координаты | `net.minecraft.core.BlockPos` |
| Перечисления | любой твой `enum` |

**Не поддерживаются** (будет ошибка в лог, поле просто не сохранится):

- Списки, массивы, `Map`, `Set`.
- Инвентари `ItemStackHandler`, баки `FluidTank`, отдельные `ItemStack`.
- Любые свои классы / объекты.

Такие вещи сериализуй руками рядом с вызовом `PersistedFields.save/load` — как `"inventory"` в
примере выше.

---

## Правила (важно, но просто)

1. **Поле не должно быть `static`.** `@Persisted` — про состояние конкретного блока/моба, а не
   про весь класс. Поставишь на `static` → в лог `@Persisted on a static field — ignored`.

2. **Поле не должно быть `final`.** Загрузка меняет значение поля — `final` этого не позволяет.
   Поставишь на `final` → поле отсеивается при сканировании класса (один раз, а не на каждом
   сохранении) и **не пишется в файл мира вообще**: смысла в ключе, который никогда не прочитается,
   нет. В лог: `@Persisted on a final field — ignored`.

3. **Дефолт пиши прямо в поле:** `private int maxProgress = 200;`. Если в файле мира ключа нет
   (первый запуск блока, старый сейв без этого поля) — `load` поле **не трогает**, и оно
   останется `200`. Если дефолт не написать — останется `0` / `false` / `null`.

4. **Меняешь ключ — оставь старый в `aliases`.** Ключ это «адрес» значения в файле мира, и
   старые миры знают только прежнее имя. Поэтому переименование делается так:

   ```java
   @Persisted(value = "charge", aliases = "progress")
   private int charge;
   ```

   `aliases` читаются на загрузке, если основного ключа в теге нет, и никогда не пишутся — так
   мир постепенно переезжает на новое имя сам. Имя Java-поля меняй свободно, ключ от него не
   зависит.

5. **Ключ с префиксом — вешай `@PersistedPrefix` на класс.** Все `@Persisted`-поля одного объекта
   пишутся в один общий `CompoundTag` рядом с ванильными ключами, поэтому префикс нужен, чтобы не
   столкнуться. Писать его в каждом поле не надо:

   ```java
   @PersistedPrefix("altar")
   public class AltarBlockEntity extends BlockEntity {
       @Persisted("progress") private int progress;   // ключ: altar.progress
   ```

   Префикс действует только на поля, **объявленные в самом этом классе** — он не наследуется,
   чтобы наследник не мог незаметно переписать ключи родителя и сломать старые миры.

6. **`@Persisted` без аргумента** — ключом станет имя поля (`@Persisted int foo` → ключ `"foo"`).
   Быстро для черновика, но для настоящего кода лучше явный ключ (см. п.4).

7. **`null` не сохраняется.** Если `@Nullable UUID ownerId == null`, в файл ничего не пишется, а
   при загрузке поле останется как есть. Это нормально.

8. **`super.*` вызывай всегда.** `PersistedFields` заменяет только твои `put*/get*`, но не
   ванильную часть.

9. **Пересобирай мод** после добавления/изменения аннотаций.

---

## Чего `@Persisted` НЕ делает

- **Не синхронизирует с клиентом сам по себе.** Это отдельная тема (`SynchedEntityData` у мобов,
  `ContainerData` / `getUpdateTag` у блок-сущностей). `@Persisted` — только про сохранение в мир.
  *Замечание:* если у блок-сущности `getUpdateTag` возвращает `saveWithoutMetadata(...)` (частый
  шаблон), то `@Persisted`-поля поедут на клиент заодно — обычно это ок.
- **Не сохраняет инвентари и жидкости.** См. таблицу выше.
- **Не делает поле «настройкой».** Это не конфиг, значение живёт в конкретном блоке/мобе в мире.

---

## Если что-то не сохраняется — смотри лог

Все сообщения начинаются с `[persisted]`:

Сообщения в логе английские (как и весь код):

| Сообщение в логе | Что значит | Что делать |
|---|---|---|
| `@Persisted on a static field — ignored` | поле `static` | убери `static` |
| `@Persisted on a final field — ignored` | поле `final` | убери `final` |
| `type X is not supported` | тип не из таблицы | сохраняй это поле руками |
| `duplicate key 'K': A и B` | два поля пишутся под одним ключом (легко словить через наследование) | дай разные ключи или разведи по `@PersistedPrefix` |
| `key 'K' holds a different tag type in the save — default kept` | в мире под этим ключом лежит тег другого вида — обычно потому, что тип поля поменялся между версиями | значение не подхватится, поле останется с дефолтом; нужен новый ключ + старый в `aliases` |
| `unknown enum constant 'FOO'` | в сейве значение enum, которого больше нет в коде | переименовал/удалил константу enum — поле останется с дефолтом |
| `saving X.Y failed` / `loading X.Y failed` | что-то неожиданное | смотри стек-трейс под сообщением |

Первые четыре строки печатаются **на старте игры**, ещё до того как ты зайдёшь в мир: при
`FMLCommonSetupEvent` прогоняется самопроверка по всем классам мода с `@Persisted`
(см. `PersistedSelfCheck`). Ищи в логе:

```
[persisted] self-check: 5 class(es) OK
```

Список классов берётся из данных сканирования модфайла, руками ничего перечислять не нужно —
новый класс с `@Persisted` попадает в проверку сам.

Ничего в логе, но значение не сохраняется — проверь: **позвал ли `PersistedFields.save/load`**,
**пересобрал ли мод**, **правильный ли ключ** (совпадает в старом сейве).

---

## Как это устроено внутри (можно не читать)

`PersistedFields.save/load` через рефлексию проходит по всем полям класса (и родителей),
берёт помеченные `@Persisted`, и по типу поля вызывает нужный `tag.putX` / `tag.getX`.
Список полей на каждый класс считается один раз и кешируется. Ошибка на одном поле логируется
и не мешает остальным.

Всё, что видно без объекта — `static`/`final`, неподдерживаемый тип, дубль ключа — проверяется
в момент сканирования класса, а не при каждом сохранении, и такие поля до файла мира не доходят.
Ключи (вместе с префиксом и `aliases`) считаются там же и кладутся в кеш готовыми строками.

На загрузке перед присваиванием проверяется **вид тега**. Это не перестраховка: ванильный
`tag.getInt` на строковом теге молча возвращает `0`, и без проверки мы бы затирали живой дефолт
нулём, ничего не сообщая.

Файлы: [`Persisted.java`](../Persisted.java) — сама аннотация,
[`PersistedPrefix.java`](../PersistedPrefix.java) — префикс ключей на класс,
[`PersistedFields.java`](../PersistedFields.java) — логика,
[`PersistedSelfCheck.java`](../PersistedSelfCheck.java) — самопроверка на старте.
