package net.gera.veridia.common.village.calc;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.gera.veridia.Veridia;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class VillageCalcCommands {

    private VillageCalcCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("calculate")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("start")
                                .executes(context -> start(context, null))
                                // start at x z - то же без игрока (консоль/rcon): центр скана в этой точке.
                                .then(Commands.literal("at")
                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(context -> start(context, new BlockPos(
                                                                IntegerArgumentType.getInteger(context, "x"), 64,
                                                                IntegerArgumentType.getInteger(context, "z"))))))))
                        .then(Commands.literal("result").executes(VillageCalcCommands::result))
                        .then(Commands.literal("reset").executes(VillageCalcCommands::reset))
                        .then(Commands.literal("pregen").executes(VillageCalcCommands::pregen))
                        // Что очередь настоящего nailer'а отбросила и почему, и сколько уже стоит в мире.
                        // fresh - то же по плану, посчитанному заново по текущему миру (+ сравнение с планом сессии).
                        .then(Commands.literal("nailer")
                                .executes(context -> nailerQueue(context, false, null, false))
                                .then(Commands.literal("fresh").executes(context -> nailerQueue(context, true, null, false)))
                                // at x z - то же без игрока (консоль/rcon): окно карты и ближайший план берутся от точки.
                                .then(Commands.literal("at")
                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(context -> nailerQueue(context, false,
                                                                new BlockPos(IntegerArgumentType.getInteger(context, "x"), 64,
                                                                        IntegerArgumentType.getInteger(context, "z")), false)))))
                                // scan x z - свежий план со скан-окном строго вокруг точки (как будто якорь деревни там).
                                .then(Commands.literal("scan")
                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(context -> nailerQueue(context, true,
                                                                new BlockPos(IntegerArgumentType.getInteger(context, "x"), 64,
                                                                        IntegerArgumentType.getInteger(context, "z")), true))))))
                        .then(Commands.literal("radius")
                                .then(Commands.literal("square")
                                        .then(Commands.argument("blocks", IntegerArgumentType.integer(1, VillageCalcSettings.MAX_SCAN_RADIUS))
                                                .executes(context -> setArea(context, VillageCalcSettings.ScanShape.SQUARE))))
                                .then(Commands.literal("circle")
                                        .then(Commands.argument("blocks", IntegerArgumentType.integer(1, VillageCalcSettings.MAX_SCAN_RADIUS))
                                                .executes(context -> setArea(context, VillageCalcSettings.ScanShape.CIRCLE)))))
                        .then(Commands.literal("source")
                                .then(Commands.literal("auto")
                                        .executes(context -> setSource(context, VillageCalcSettings.MarkerSource.AUTO)))
                                .then(Commands.literal("markers")
                                        .executes(context -> setSource(context, VillageCalcSettings.MarkerSource.MARKERS)))
                                .then(Commands.literal("village")
                                        .executes(context -> setSource(context, VillageCalcSettings.MarkerSource.VILLAGE))))
                        // Тесты апгрейда/башен (tech_1/tech_2) без реального nailer'а с прокачкой -
                        // просто два независимых переключателя на следующий /calculate result.
                        .then(Commands.literal("upgraded")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(VillageCalcCommands::setUpgraded)))
                        .then(Commands.literal("towers")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(VillageCalcCommands::setBuildTowers)))
                        // Дома (tech_3, см. HouseSitePlan/VillageCalc.buildHouse) - отдельно от
                        // result: по имени шаблона, один вызов - одна попытка поставить один дом.
                        .then(Commands.literal("building")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        // Подсказки по Tab - все имена из HOUSE_NAMES.
                                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider
                                                .suggest(VillageCalcSettings.HOUSE_NAMES, builder))
                                        .executes(context -> building(context, null))
                                        // building <имя> auto|0|90|180|270 - поворот вручную (auto - к дорожкам).
                                        .then(Commands.argument("turn", StringArgumentType.word())
                                                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider
                                                        .suggest(java.util.List.of("auto", "0", "90", "180", "270"), builder))
                                                .executes(context -> building(context, parseTurn(StringArgumentType.getString(context, "turn")))))))
        );
    }

    private static int setUpgraded(CommandContext<CommandSourceStack> context) {
        boolean value = BoolArgumentType.getBool(context, "value");
        VillageCalc.setUpgraded(value);
        say(context, "Апгрейд стены (tech_1) в /calculate result: " + (value ? "включён (\"2\"-шаблоны)" : "выключен (обычные)"));
        return 1;
    }

    private static int setBuildTowers(CommandContext<CommandSourceStack> context) {
        boolean value = BoolArgumentType.getBool(context, "value");
        VillageCalc.setBuildTowers(value);
        say(context, "Башни (tech_2) в /calculate result: " + (value ? "включены" : "выключены"));
        return 1;
    }

    private static int setSource(CommandContext<CommandSourceStack> context, VillageCalcSettings.MarkerSource source) {
        VillageCalc.setMarkerSource(source);
        say(context, switch (source) {
            case AUTO -> "Источник: сам - настоящая деревня, если она рядом, иначе цветные маркеры тестовой карты";
            case MARKERS -> "Источник: цветные маркеры тестовой карты (POI деревни не видны)";
            case VILLAGE -> "Источник: настоящая деревня - дома из POI кроватей, разное из рабочих мест и стогов, дороги из dirt_path";
        });
        return 1;
    }

    private static int setArea(CommandContext<CommandSourceStack> context, VillageCalcSettings.ScanShape shape) {
        int blocks = IntegerArgumentType.getInteger(context, "blocks");
        VillageCalc.setScanArea(shape, blocks);
        say(context, "Область поиска: " + (shape == VillageCalcSettings.ScanShape.SQUARE ? "квадрат" : "круг")
                + " радиусом " + VillageCalc.scanRadius() + " блоков");
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> context, BlockPos at) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        BlockPos center = at != null ? at : context.getSource().getPlayerOrException().blockPosition();

        long began = System.currentTimeMillis();
        VillageCalc.ScanReport report = VillageCalc.calculate(level, center);
        long took = System.currentTimeMillis() - began;

        say(context, "Обводка готова за " + took + " мс, область: "
                + (VillageCalc.scanShape() == VillageCalcSettings.ScanShape.SQUARE ? "квадрат" : "круг")
                + " " + VillageCalc.scanRadius());
        boolean fromVillage = VillageCalc.usedSource() == VillageCalcSettings.MarkerSource.VILLAGE;
        say(context, "  источник: " + (fromVillage ? "деревня" : "маркеры")
                + (VillageCalc.markerSource() == VillageCalcSettings.MarkerSource.AUTO ? " (выбран сам)" : ""));
        say(context, "  маркеры: домов " + report.houses()
                + ", разного " + report.misc()
                + ", складов " + report.storage()
                + ", тропинок " + report.paths());
        if (fromVillage) {
            say(context, "  POI деревни найдено: " + report.poiFound());
            if (report.poiFound() == 0) {
                say(context, "  POI пусто - деревня далеко, радиус мал или чанки не прогружены");
            }
        } else if (report.houses() == 0 && report.paths() > 0) {
            say(context, "  Домов ноль, а дорог много - похоже, это настоящая деревня.");
            say(context, "  Переключись: /calculate source village");
        }
        say(context, "  территория: " + report.territoryCells() + " клеток, из них под кронами " + report.treeCells());
        say(context, "  стена: " + report.segments() + " кусков ("
                + report.entrances() + " из них входы), "
                + report.fillers() + " столбиков, выброшено обрывков " + report.strays()
                + ", заплаток у обхода " + report.sealed());
        say(context, "  постройки без POI (заборы/сараи): поглощено " + report.structuresAbsorbed()
                + ", отклонено по размеру " + report.structuresRejectedSize()
                + " (крупнейший блок " + report.largestRejectedStructureBlob() + " клеток)"
                + ", отклонено по дальности " + report.structuresRejectedDistance());
        say(context, "  лёд: у деревни (в " + VillageCalcSettings.ICE_LAND_RANGE + " клетках, как земля, под стеной станет плотным) "
                + report.iceLandCells() + " клеток, дальше (остался постройкой) " + report.iceFarCells());
        say(context, "  проём у воды: " + report.waterGaps() + " клеток контура на реке");
        say(context, "  ворота: virtual weight выбранных " + report.gateWeights()
                + ", кандидатов было " + report.gateCandidates()
                + ", их диапазон " + report.gateWeightMin() + ".." + report.gateWeightMax());
        say(context, "  башни: зелёных углов " + report.towerGreenCandidates()
                + ", жёлтых прямых участков " + report.towerYellowCandidates()
                + ", выбрано после отбора " + report.towersChosen()
                + " (" + (VillageCalc.isBuildTowers() ? "построятся" : "выключены - /calculate towers true") + ")");
        say(context, "  башни-диагностика: клеток контура проверено " + report.towerBoundaryCells()
                + ", из них настоящих углов " + report.towerCorners()
                + ", забраковано по неровности " + report.towerCornersRejectedFlatness()
                + " (если тут почти все углы - смотри рельеф, не траву/цветы - они высоту не поднимают)");
        say(context, "  дома (tech_3, площадки 7x7 для наглядности - реальный дом ищет место под свой размер): подходящих площадок "
                + report.houseCandidates() + ", показано после отбора " + report.houseSitesChosen()
                + " (контур " + VillageCalcSettings.HOUSE_FOOTPRINT_SIZE + "x" + VillageCalcSettings.HOUSE_FOOTPRINT_SIZE + ", розовые частицы)");
        say(context, "Частицы: яркие точки - найденные маркеры (зелёный дом, синий разное, оранжевый тропинка),");
        say(context, "         жёлтые - заплатки у обхода, красные - контур, голубые - проём у воды, серые - территория,");
        say(context, "         высоко над стеной - башни: зелёный/жёлтый - все кандидаты, белый - реально выбранные,");
        say(context, "         розовые контуры на земле - площадки под дом (tech_3)");
        say(context, "Апгрейд (tech_1) в результате: " + (VillageCalc.isUpgraded() ? "включён" : "выключен - /calculate upgraded true"));
        say(context, "Стиль деревни: " + VillageCalc.style() + " (шаблоны из папки " + VillageCalc.style().name().toLowerCase() + ", заливка - "
                + VillageCalc.style().scrapWood().getDescriptionId() + ")");
        say(context, "  " + VillageCalc.pitSummary());
        say(context, "Дальше: /calculate result");
        return 1;
    }

    private static int result(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!VillageCalc.hasScan()) {
            say(context, "Сначала /calculate start");
            return 0;
        }
        int placed = VillageCalc.build(context.getSource().getLevel());
        if (placed < 0) {
            say(context, "Не нашёл структуры стены - проверь data/veridia/structure/wall/");
            return 0;
        }
        say(context, "Построено кусков и столбиков: " + placed
                + ", срублено блоков дерева: " + VillageCalc.treeBlocksCut()
                + ", решёток под водой: " + VillageCalc.waterBarriers()
                + ", пропущено из-за построек: " + VillageCalc.skipped()
                + ", одиночек убрано: " + VillageCalc.strayColumns());
        if (VillageCalc.isBuildTowers()) {
            say(context, "Башен построено: " + VillageCalc.towersPlaced()
                    + ", пропущено (не влезла) " + VillageCalc.towersSkipped());
        }
        for (String line : TowerJoinDebug.lastSummary()) { // TOWER-DEBUG
            say(context, "  " + line);
        }
        say(context, "Откатить: /calculate reset");
        return 1;
    }

    private static int building(CommandContext<CommandSourceStack> context, net.minecraft.world.level.block.Rotation forced) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        VillageCalc.HouseBuildResult result = VillageCalc.buildHouse(context.getSource().getLevel(), name, forced);
        switch (result) {
            case NO_SCAN -> say(context, "Сначала /calculate start");
            case WALL_NOT_BUILT -> say(context, "Сначала построй стену: /calculate result (дом ищет место уже с учётом настоящей стены/башен, а не только скана)");
            case UNKNOWN_NAME -> say(context, "Неизвестное имя \"" + name + "\". Пока есть: "
                    + String.join(", ", VillageCalcSettings.HOUSE_NAMES));
            case TEMPLATE_MISSING -> say(context, "NBT-структура не найдена - проверь data/veridia/structure/village/plains/" + name);
            case NO_SITE -> {
                say(context, "Не нашёл подходящей площадки под \"" + name + "\": ни внутри стены, ни у стены с расширением (в текущей области скана)");
                for (String line : VillageCalc.explainNoSite(context.getSource().getLevel(), name)) {
                    say(context, line);
                }
            }
            case PLACED, PLACED_WITH_WALL_EXPANSION -> {
                BoundingBox box = VillageCalc.lastHouseBox();
                say(context, "Построен \"" + name + "\" у " + box.minX() + "," + box.minY() + "," + box.minZ());
                say(context, "Поворот дома: " + rotationText(VillageCalc.lastHouseRotation()) + " (вход развёрнут к ближайшим дорожкам; без дорожек поблизости - как в шаблоне)");
                if (result == VillageCalc.HouseBuildResult.PLACED_WITH_WALL_EXPANSION) {
                    say(context, "Внутри места не было: стена расширена под дом и перестроена (построенные башни на месте, старый кусок стены убран)");
                } else {
                    say(context, "Площадка: не на тропинках, не вплотную к зданиям/стене/башням; деревья на ней срублены");
                }
                say(context, "Откатить: /calculate reset");
            }
        }
        return 1;
    }

    private static net.minecraft.world.level.block.Rotation parseTurn(String text) {
        return switch (text) {
            case "0" -> net.minecraft.world.level.block.Rotation.NONE;
            case "90" -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case "180" -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case "270" -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> null;
        };
    }

    private static String rotationText(net.minecraft.world.level.block.Rotation rotation) {
        return switch (rotation) {
            case NONE -> "0°";
            case CLOCKWISE_90 -> "90° по часовой";
            case CLOCKWISE_180 -> "180°";
            case COUNTERCLOCKWISE_90 -> "90° против часовой";
        };
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (VillageCalc.isWrongLevel(context.getSource().getLevel())) {
            say(context, "Стена стоит в другом измерении - откатываю там, а не там, где ты сейчас");
        }
        int restored = VillageCalc.reset(context.getSource().getLevel());
        say(context, "Возвращено блоков: " + restored + ", расчёт сброшен");
        return 1;
    }

    private static int pregen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        say(context, "Автопланы стен в этом мире: готово " + VillageAutoPregen.cachedPlans(level)
                + ", в очереди " + VillageAutoPregen.queuedVillages(level)
                + ", деревень найдено всего " + VillageAutoPregen.knownVillages(level));
        return 1;
    }

    private static int nailerQueue(CommandContext<CommandSourceStack> context, boolean fresh, BlockPos at, boolean centered) throws CommandSyntaxException {
        BlockPos near = at != null ? at : context.getSource().getPlayerOrException().blockPosition();
        NailerQueueAudit.Result result = NailerQueueAudit.run(context.getSource().getLevel(), near, fresh, centered);
        result.chat().forEach(line -> say(context, line));
        return 1;
    }

    private static void say(CommandContext<CommandSourceStack> context, String text) {
        context.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}