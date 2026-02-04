package com.visitors.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.visitors.data.VisitorsSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Registro de todos los comandos del mod.
 */
public class ModCommands {

    public static final java.util.Set<java.util.UUID> chairEditors = new java.util.HashSet<>();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /visitors resetstars - Reset all ratings
        dispatcher.register(Commands.literal("visitors")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("resetstars")
                        .executes(context -> {
                            ServerLevel level = context.getSource().getLevel();
                            VisitorsSavedData.get(level).resetReviews(level);
                            context.getSource()
                                    .sendSuccess(() -> Component.literal(
                                            "§c§l[Reset] §fTodas las estrellas y valoraciones han sido reseteadas."),
                                            true);
                            return 1;
                        }))
                .then(Commands.literal("spawntrash")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerLevel level = player.serverLevel();
                            com.visitors.entity.TrashEntity trash = new com.visitors.entity.TrashEntity(
                                    com.visitors.entity.ModEntities.TRASH.get(), level);
                            trash.setPos(player.getX(), player.getY(), player.getZ());
                            level.addFreshEntity(trash);
                            player.sendSystemMessage(Component.literal("§e¡Basura spawneada!"));
                            return 1;
                        }))
                .then(Commands.literal("resettimes")
                        .executes(context -> {
                            ServerLevel level = context.getSource().getLevel();
                            long currentTime = level.dayTime();
                            VisitorsSavedData data = VisitorsSavedData.get(level);

                            data.setLastContractorTime(currentTime);
                            data.setLastInspectionTime(currentTime);

                            context.getSource().sendSuccess(() -> Component.literal(
                                    "§a§l[Reset] §fTiempos de gestión restablecidos al día actual."), true);
                            return 1;
                        })));

        // /visitorspos1 - Set first corner of visitor area
        dispatcher.register(Commands.literal("visitorspos1")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();

                    VisitorsSavedData data = VisitorsSavedData.get(level);
                    data.setPos1(pos);

                    player.sendSystemMessage(Component.translatable("commands.visitors.pos1.set",
                            String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));

                    if (data.hasValidArea()) {
                        player.sendSystemMessage(Component.translatable("commands.visitors.area.complete"));
                    }

                    return 1;
                }));

        // /visitorspos2 - Set second corner of visitor area
        dispatcher.register(Commands.literal("visitorspos2")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();

                    VisitorsSavedData data = VisitorsSavedData.get(level);
                    data.setPos2(pos);

                    player.sendSystemMessage(Component.translatable("commands.visitors.pos2.set",
                            String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));

                    if (data.hasValidArea()) {
                        player.sendSystemMessage(Component.translatable("commands.visitors.area.complete"));
                    }

                    return 1;
                }));

        // /visitorspos1_2 - Set first corner of SECOND visitor area
        dispatcher.register(Commands.literal("visitorspos1_2")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();

                    VisitorsSavedData data = VisitorsSavedData.get(level);
                    VisitorsSavedData.Area area = data.getArea(1); // Area index 1
                    area.pos1 = pos;
                    data.setDirty();

                    player.sendSystemMessage(Component.translatable("commands.visitors.pos1.set",
                            String.format("Area 2 (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));

                    if (area.isValid()) {
                        player.sendSystemMessage(Component.translatable("commands.visitors.area.complete"));
                    }

                    return 1;
                }));

        // /visitorspos2_2 - Set second corner of SECOND visitor area
        dispatcher.register(Commands.literal("visitorspos2_2")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();

                    VisitorsSavedData data = VisitorsSavedData.get(level);
                    VisitorsSavedData.Area area = data.getArea(1); // Area index 1
                    area.pos2 = pos;
                    data.setDirty();

                    player.sendSystemMessage(Component.translatable("commands.visitors.pos2.set",
                            String.format("Area 2 (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));

                    if (area.isValid()) {
                        player.sendSystemMessage(Component.translatable("commands.visitors.area.complete"));
                    }

                    return 1;
                }));

        // /visitorspawn1 - Set first spawn point
        dispatcher.register(Commands.literal("visitorspawn1")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();

                    VisitorsSavedData data = VisitorsSavedData.get(level);
                    data.setSpawn1(pos);

                    player.sendSystemMessage(Component.translatable("commands.visitors.spawn1.set",
                            String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));

                    if (data.hasValidSpawn()) {
                        player.sendSystemMessage(Component.translatable("commands.visitors.spawn.complete"));
                    }

                    return 1;
                }));

        // /visitorspawn2 - Set second spawn point
        dispatcher.register(Commands.literal("visitorspawn2")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();

                    VisitorsSavedData data = VisitorsSavedData.get(level);
                    data.setSpawn2(pos);

                    player.sendSystemMessage(Component.translatable("commands.visitors.spawn2.set",
                            String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));

                    if (data.hasValidSpawn()) {
                        player.sendSystemMessage(Component.translatable("commands.visitors.spawn.complete"));
                    }

                    return 1;
                }));

        // /visitorsconfig - Show current configuration
        dispatcher.register(Commands.literal("visitorsconfig")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();

                    VisitorsSavedData data = VisitorsSavedData.get(level);

                    player.sendSystemMessage(Component.literal("§6=== Visitors Configuration ==="));

                    java.util.List<VisitorsSavedData.Area> areas = data.getAreas();
                    for (int i = 0; i < areas.size(); i++) {
                        VisitorsSavedData.Area area = areas.get(i);
                        player.sendSystemMessage(Component.literal(String.format("§e--- Area %d ---", i + 1)));
                        if (area.pos1 != null) {
                            BlockPos p = area.pos1;
                            player.sendSystemMessage(Component
                                    .literal(String.format("§aPos1: §f(%d, %d, %d)", p.getX(), p.getY(), p.getZ())));
                        } else {
                            player.sendSystemMessage(Component.literal("§cPos1: §7Not set"));
                        }
                        if (area.pos2 != null) {
                            BlockPos p = area.pos2;
                            player.sendSystemMessage(Component
                                    .literal(String.format("§aPos2: §f(%d, %d, %d)", p.getX(), p.getY(), p.getZ())));
                        } else {
                            player.sendSystemMessage(Component.literal("§cPos2: §7Not set"));
                        }
                    }

                    if (data.getSpawn1() != null) {
                        BlockPos p = data.getSpawn1();
                        player.sendSystemMessage(Component
                                .literal(String.format("§aSpawn1: §f(%d, %d, %d)", p.getX(), p.getY(), p.getZ())));
                    } else {
                        player.sendSystemMessage(Component.literal("§cSpawn1: §7Not set"));
                    }

                    if (data.getSpawn2() != null) {
                        BlockPos p = data.getSpawn2();
                        player.sendSystemMessage(Component
                                .literal(String.format("§aSpawn2: §f(%d, %d, %d)", p.getX(), p.getY(), p.getZ())));
                    } else {
                        player.sendSystemMessage(Component.literal("§cSpawn2: §7Not set"));
                    }

                    player.sendSystemMessage(
                            Component.literal(String.format("§eMax Visitors: §f%d", data.getMaxVisitors())));

                    // Birthday zone info
                    player.sendSystemMessage(Component.literal("§e--- Zona Cumpleaños ---"));
                    if (data.getBirthdayPos1() != null) {
                        BlockPos p = data.getBirthdayPos1();
                        player.sendSystemMessage(Component
                                .literal(
                                        String.format("§aBirthdayPos1: §f(%d, %d, %d)", p.getX(), p.getY(), p.getZ())));
                    } else {
                        player.sendSystemMessage(Component.literal("§cBirthdayPos1: §7No configurado"));
                    }
                    if (data.getBirthdayPos2() != null) {
                        BlockPos p = data.getBirthdayPos2();
                        player.sendSystemMessage(Component
                                .literal(
                                        String.format("§aBirthdayPos2: §f(%d, %d, %d)", p.getX(), p.getY(), p.getZ())));
                    } else {
                        player.sendSystemMessage(Component.literal("§cBirthdayPos2: §7No configurado"));
                    }

                    // Baby zone info
                    player.sendSystemMessage(Component.literal("§e--- Zona Baby ---"));
                    if (data.getBabyZonePos1() != null) {
                        BlockPos p = data.getBabyZonePos1();
                        player.sendSystemMessage(Component
                                .literal(
                                        String.format("§aBabyZonePos1: §f(%d, %d, %d)", p.getX(), p.getY(), p.getZ())));
                    } else {
                        player.sendSystemMessage(Component.literal("§cBabyZonePos1: §7No configurado"));
                    }
                    if (data.getBabyZonePos2() != null) {
                        BlockPos p = data.getBabyZonePos2();
                        player.sendSystemMessage(Component
                                .literal(
                                        String.format("§aBabyZonePos2: §f(%d, %d, %d)", p.getX(), p.getY(), p.getZ())));
                    } else {
                        player.sendSystemMessage(Component.literal("§cBabyZonePos2: §7No configurado"));
                    }

                    if (data.isFullyConfigured()) {
                        player.sendSystemMessage(Component.literal("§aStatus: §f¡Listo para spawnear visitantes!"));
                    } else {
                        player.sendSystemMessage(Component.literal("§cStatus: §fNo completamente configurado"));
                    }

                    return 1;
                }));

        // /visitorsclear - Remove all visitors
        dispatcher.register(Commands.literal("visitorsclear")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();

                    int removed = 0;
                    for (var entity : level.getAllEntities()) {
                        if (entity instanceof com.visitors.entity.VisitorEntity) {
                            com.visitors.entity.VisitorEntity visitor = (com.visitors.entity.VisitorEntity) entity;
                            visitor.discard();
                            removed++;
                        }
                    }

                    player.sendSystemMessage(Component.literal(String.format("§aRemoved %d visitors", removed)));
                    return 1;
                }));

        // /visitorsmax <amount>
        dispatcher.register(Commands.literal("visitorsmax")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,
                                    "amount");

                            VisitorsSavedData data = VisitorsSavedData.get(player.serverLevel());
                            data.setMaxVisitors(amount);

                            player.sendSystemMessage(Component.literal("§aMax visitors set to: " + amount));
                            return 1;
                        })));

        // /visitorbirthdaypos1 - Set first corner of birthday area
        dispatcher.register(Commands.literal("visitorbirthdaypos1")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();

                    VisitorsSavedData data = VisitorsSavedData.get(level);
                    data.setBirthdayPos1(pos);

                    player.sendSystemMessage(Component.literal(String.format(
                            "§aPosición 1 de zona de cumpleaños: §f(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));

                    if (data.hasValidBirthdayZone()) {
                        player.sendSystemMessage(Component.literal("§a¡Zona de cumpleaños configurada correctamente!"));
                    }

                    return 1;
                }));

        // /visitorbirthdaypos2 - Set second corner of birthday area
        dispatcher.register(Commands.literal("visitorbirthdaypos2")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();

                    VisitorsSavedData data = VisitorsSavedData.get(level);
                    data.setBirthdayPos2(pos);

                    player.sendSystemMessage(Component.literal(String.format(
                            "§aPosición 2 de zona de cumpleaños: §f(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));

                    if (data.hasValidBirthdayZone()) {
                        player.sendSystemMessage(Component.literal("§a¡Zona de cumpleaños configurada correctamente!"));
                    }

                    return 1;
                }));

        // /visitorbabyzonepos1 - Set first corner of baby zone
        dispatcher.register(Commands.literal("visitorbabyzonepos1")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();

                    VisitorsSavedData data = VisitorsSavedData.get(level);
                    data.setBabyZonePos1(pos);

                    player.sendSystemMessage(Component.literal(String.format(
                            "§aPosición 1 de zona baby: §f(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));

                    if (data.hasValidBabyZone()) {
                        player.sendSystemMessage(Component.literal("§a¡Zona baby configurada correctamente!"));
                    }

                    return 1;
                }));

        // /visitorbabyzonepos2 - Set second corner of baby zone
        dispatcher.register(Commands.literal("visitorbabyzonepos2")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();

                    VisitorsSavedData data = VisitorsSavedData.get(level);
                    data.setBabyZonePos2(pos);

                    player.sendSystemMessage(Component.literal(String.format(
                            "§aPosición 2 de zona baby: §f(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));

                    if (data.hasValidBabyZone()) {
                        player.sendSystemMessage(Component.literal("§a¡Zona baby configurada correctamente!"));
                    }

                    return 1;
                }));

        // /visitorbirthday - Force trigger a birthday event
        dispatcher.register(Commands.literal("visitorbirthday")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();

                    VisitorsSavedData data = VisitorsSavedData.get(level);

                    if (!data.hasValidBirthdayZone()) {
                        player.sendSystemMessage(Component.literal(
                                "§c¡Configura la zona de cumpleaños primero! Usa /visitorbirthdaypos1 y /visitorbirthdaypos2"));
                        return 0;
                    }

                    // Trigger birthday event via SpawnManager
                    com.visitors.manager.VisitorSpawnManager.INSTANCE.triggerBirthdayEvent(level, data);

                    return 1;
                }));

        // /stars - Show current restaurant rating
        dispatcher.register(Commands.literal("stars")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    VisitorsSavedData data = VisitorsSavedData.get(level);

                    float rating = data.getRating();
                    int fullStars = Math.round(rating);

                    StringBuilder stars = new StringBuilder();
                    for (int i = 0; i < 5; i++) {
                        if (i < fullStars) {
                            stars.append("§6★"); // Golden star
                        } else {
                            stars.append("§7☆"); // Grey empty star
                        }
                    }

                    player.sendSystemMessage(Component.literal("§e§l--- Reputación de la Pizzería ---"));
                    player.sendSystemMessage(Component
                            .literal("§fRating: " + stars.toString() + " §e" + String.format("%.1f/5.0", rating)));

                    int count = data.getRatingCount();
                    player.sendSystemMessage(Component.literal("§7(Basado en " + count + " reseñas)"));

                    return 1;
                }));

        // /visitorschair - Chair management
        dispatcher.register(Commands.literal("visitorschair")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            BlockPos pos = player.blockPosition().below();
                            VisitorsSavedData.get(player.serverLevel()).addChair(pos);
                            player.sendSystemMessage(
                                    Component.literal("§a¡Silla agregada en " + pos.toShortString() + "!"));
                            return 1;
                        }))
                .then(Commands.literal("clear")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            VisitorsSavedData.get(player.serverLevel()).getChairs().clear();
                            VisitorsSavedData.get(player.serverLevel()).setDirty();
                            player.sendSystemMessage(Component.literal("§c¡Todas las sillas han sido eliminadas!"));
                            return 1;
                        }))
                .then(Commands.literal("edit")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            UUID uuid = player.getUUID();
                            if (chairEditors.contains(uuid)) {
                                chairEditors.remove(uuid);
                                player.sendSystemMessage(Component.literal("§c§l[Modo Edición] §fDesactivado."));
                            } else {
                                chairEditors.add(uuid);
                                player.sendSystemMessage(Component.literal("§a§l[Modo Edición] §fActivado."));
                                player.sendSystemMessage(Component.literal(
                                        "§7Haz click derecho en un bloque para agregarlo o quitarlo como silla."));
                            }
                            return 1;
                        }))
                .then(Commands.literal("offset")
                        .then(Commands.argument("value", FloatArgumentType.floatArg())
                                .executes(context -> {
                                    float value = FloatArgumentType.getFloat(context, "value");
                                    ServerLevel level = context.getSource().getLevel();
                                    VisitorsSavedData.get(level).setChairYOffset(value);
                                    context.getSource().sendSuccess(
                                            () -> Component
                                                    .literal("§a§l[Config] §fAltura de sillas ajustada a: §e" + value),
                                            true);
                                    return 1;
                                }))));
    }
}
