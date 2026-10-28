package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Дома, которые nailer'ы уже заложили в этом мире (tech_3) - чтобы начатый дом достраивался, а не
 * бросался. Без этого nailer выбирал площадку заново при каждой пересборке очереди: недостроенный
 * дом переставал считаться "свободным местом" (на площадке уже лежит его же булыжник) и выпадал из
 * очереди, а nailer начинал новый дом на другой площадке.
 * <p>
 * Хранится мировая позиция origin дома (как в placeInWorld) и шаблон - не привязка к деревне: план
 * деревни (VillageAutoPregen) живёт только в памяти и после перезапуска считается заново, в том числе
 * с другим якорем. "Дома этой деревни" - просто те, чей origin попадает в область её скана.
 */
public final class NailerHouses extends SavedData {

    private static final String DATA_NAME = "veridia_nailer_houses";

    public record House(BlockPos origin, ResourceLocation template) {}

    private final List<House> houses = new ArrayList<>();

    public static NailerHouses get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(NailerHouses::new, NailerHouses::load), DATA_NAME);
    }

    /** Дома, заложенные в пределах области скана этой деревни, в порядке закладки. */
    public List<House> inVillage(VillageScan scan) {
        List<House> result = new ArrayList<>();
        for (House house : houses) {
            int cx = house.origin().getX() - scan.minX();
            int cz = house.origin().getZ() - scan.minZ();
            if (scan.inBounds(cx, cz)) {
                result.add(house);
            }
        }
        return result;
    }

    public void add(House house) {
        houses.add(house);
        setDirty();
    }

    private static NailerHouses load(CompoundTag tag, HolderLookup.Provider provider) {
        NailerHouses data = new NailerHouses();
        ListTag list = tag.getList("houses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos origin = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            ResourceLocation template = ResourceLocation.tryParse(entry.getString("template"));
            if (template != null) {
                data.houses.add(new House(origin, template));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (House house : houses) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", house.origin().getX());
            entry.putInt("y", house.origin().getY());
            entry.putInt("z", house.origin().getZ());
            entry.putString("template", house.template().toString());
            list.add(entry);
        }
        tag.put("houses", list);
        return tag;
    }
}
