package net.gera.veridia.common.villager.upgrade;

import com.mojang.serialization.Codec;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.recipe.villagerupgrade.VillagerUpgradeRecipe;
import net.gera.veridia.common.recipe.villagerupgrade.VillagerUpgradeRecipeInput;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Общий "движок" дерева улучшений жителя одной профессии - используется LumberjackUpgrades,
 * FarmerUpgrades и т.п., чтобы не копировать attachment/разблокировку/рецепты на каждую
 * профессию заново. Одно дерево = один экземпляр {@code new VillagerUpgradeTree<>("farmer")},
 * узлы описываются вызовами add(id, type, prerequisiteId, x, y) в статическом блоке владельца,
 * последний вызов - build().
 * <p>
 * id узла используется и как имя attachment'а (&lt;treeName&gt;_upgrade_&lt;id&gt;), и как путь
 * рецепта стоимости - data/veridia/recipe/villager_upgrade/&lt;treeName&gt;/&lt;id&gt;.json (тип
 * "veridia:villager_upgrade", один общий тип на все деревья, см. VillagerUpgradeRecipe).
 * prerequisiteId - id узла-родителя (null у корня), резолвится в индекс лениво по id, а не по
 * сырому числу - вставка/добавление узлов ничего не ломает.
 * <p>
 * Состояние разблокировки хранится НА ЖИТЕЛЕ (attachment на Villager, не на Player) - "1 апгрейд
 * жителя на одного жителя": прокачанный лесоруб не делает сильнее всех остальных лесорубов в
 * мире, у каждого своя независимая прокачка. Стоимость (costDisplayStack/hasCost/consumeCost)
 * по-прежнему берётся из инвентаря ИГРОКА - он платит за апгрейд конкретного жителя из своих
 * запасов, поэтому tryUnlock принимает и игрока (откуда списать предмет), и жителя (кому
 * засчитать разблокировку).
 */
public final class VillagerUpgradeTree<T extends Enum<T>> {

    public record Node<T>(String id, T type, @Nullable String prerequisiteId,
                           Supplier<AttachmentType<Boolean>> attachment, int x, int y) {
    }

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Veridia.MOD_ID);

    /** Вызвать один раз из Veridia - регистрирует attachment'ы ВСЕХ деревьев улучшений сразу. */
    public static void registerAll(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

    private final String treeName;
    private final List<Node<T>> mutableNodes = new ArrayList<>();
    private final Map<String, Integer> indexById = new HashMap<>();
    private List<Node<T>> nodes = List.of();

    public VillagerUpgradeTree(String treeName) {
        this.treeName = treeName;
    }

    public Node<T> add(String id, T type, @Nullable String prerequisiteId, int x, int y) {
        Supplier<AttachmentType<Boolean>> attachment = ATTACHMENT_TYPES.register(
                treeName + "_upgrade_" + id,
                () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).sync(ByteBufCodecs.BOOL).build());
        Node<T> node = new Node<>(id, type, prerequisiteId, attachment, x, y);
        indexById.put(id, mutableNodes.size());
        mutableNodes.add(node);
        return node;
    }

    /** Вызвать после всех add() - фиксирует список узлов. Возвращаемое значение удобно сразу
     * присвоить публичному полю NODES владельца. */
    public List<Node<T>> build() {
        nodes = List.copyOf(mutableNodes);
        return nodes;
    }

    public List<Node<T>> nodes() {
        return nodes;
    }

    public int indexOf(String id) {
        Integer index = indexById.get(id);
        return index != null ? index : -1;
    }

    /** -1, если у узла нет родителя (корень дерева). */
    public int prerequisiteIndex(int nodeIndex) {
        String prerequisiteId = nodes.get(nodeIndex).prerequisiteId();
        return prerequisiteId == null ? -1 : indexOf(prerequisiteId);
    }

    public boolean isUnlocked(Villager villager, int nodeIndex) {
        return villager.getData(nodes.get(nodeIndex).attachment());
    }

    public void unlock(Villager villager, int nodeIndex) {
        villager.setData(nodes.get(nodeIndex).attachment(), true);
    }

    public boolean isAvailable(Villager villager, int nodeIndex) {
        int prerequisite = prerequisiteIndex(nodeIndex);
        return prerequisite == -1 || isUnlocked(villager, prerequisite);
    }

    private ResourceLocation recipeId(int nodeIndex) {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,
                "villager_upgrade/" + treeName + "/" + nodes.get(nodeIndex).id());
    }

    private Optional<VillagerUpgradeRecipe> recipeFor(Player player, int nodeIndex) {
        return player.level().getRecipeManager().byKey(recipeId(nodeIndex))
                .map(RecipeHolder::value)
                .filter(VillagerUpgradeRecipe.class::isInstance)
                .map(VillagerUpgradeRecipe.class::cast);
    }

    /** Пример предмета-стоимости узла - для тултипа на клиенте, с реальным нужным количеством
     * (recipe.count(), а не тем count=1, что даёт Ingredient.getItems() сам по себе - иначе
     * тултип показывал название предмета без числа). ItemStack.EMPTY, если рецепт не найден
     * (датапак ещё не загружен/удалён). */
    public ItemStack costDisplayStack(Player player, int nodeIndex) {
        return recipeFor(player, nodeIndex)
                .map(recipe -> recipe.ingredient().getItems().length > 0
                        ? recipe.ingredient().getItems()[0].copyWithCount(recipe.count())
                        : ItemStack.EMPTY)
                .orElse(ItemStack.EMPTY);
    }

    public boolean hasCost(Player player, int nodeIndex) {
        Optional<VillagerUpgradeRecipe> recipe = recipeFor(player, nodeIndex);
        if (recipe.isEmpty()) return false;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && recipe.get().matches(new VillagerUpgradeRecipeInput(stack), player.level())) return true;
        }
        return false;
    }

    private void consumeCost(Player player, int nodeIndex) {
        recipeFor(player, nodeIndex).ifPresent(recipe -> {
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && recipe.matches(new VillagerUpgradeRecipeInput(stack), player.level())) {
                    stack.shrink(recipe.count());
                    return;
                }
            }
        });
    }

    /** Проверяет доступность узла (предыдущий открыт у ЭТОГО жителя), что он ещё не разблокирован
     * у него же, и списывает подходящий предмет из инвентаря игрока по его рецепту. true - узел
     * разблокирован этим вызовом. */
    public boolean tryUnlock(Player player, Villager villager, int nodeIndex) {
        if (nodeIndex < 0 || nodeIndex >= nodes.size()) return false;
        if (isUnlocked(villager, nodeIndex) || !isAvailable(villager, nodeIndex) || !hasCost(player, nodeIndex)) {
            return false;
        }
        consumeCost(player, nodeIndex);
        unlock(villager, nodeIndex);
        return true;
    }

    /** Сколько узлов такого типа разблокировано у ЭТОГО конкретного жителя - не у всех жителей
     * профессии и не у игрока. */
    public int countUnlockedOfType(Villager villager, T type) {
        int count = 0;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).type() == type && isUnlocked(villager, i)) count++;
        }
        return count;
    }
}
