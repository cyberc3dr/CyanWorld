package ru.cyanworld.cyanuniverse;


import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import net.minecraft.server.v1_12_R1.NBTTagString;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import ru.cyanworld.cyan1dex.api.ItemBuilder;
import ru.cyanworld.cyan1dex.api.NBTFactory;

import java.util.*;

import static ru.cyanworld.cyanuniverse.CyanUniverse.plugin;
import static ru.cyanworld.cyanuniverse.CyanUniverse.server;


public class Coding implements Listener {
    public static Map<World, CodeEventHandler> codeMap = new HashMap<>();
    public static List<UUID> canceledEvents = new ArrayList<>();
    public static String clearChat = getCleatChat();
    public static ItemStack kit_playerevent = addPlaceAndBreakTags(
        new ItemBuilder(Material.DIAMOND_BLOCK).name("§bСобытие игрока").lore(Arrays.asList("§7Когда игрок что-то делает...")).build(),
        Arrays.asList("minecraft:repeating_command_block"),
        Arrays.asList("minecraft:wall_sign")
    );

    public static ItemStack kit_playeraction = addPlaceAndBreakTags(
        new ItemBuilder(Material.COBBLESTONE).name("§aСделать игроку").lore(Arrays.asList("§7Сделать чтото с игроком...")).build(),
        Arrays.asList("minecraft:chain_command_block"),
        Arrays.asList("minecraft:wall_sign")
    );

    public static ItemStack kit_warp = addPlaceAndBreakTags(
        new ItemBuilder(Material.LAPIS_BLOCK).name("§dВыполнить строку").lore(Arrays.asList("§7Получился длинный код? Разбейте его на строки!")).build(),
        Arrays.asList("minecraft:chain_command_block", "minecraft:repeating_command_block"),
        Arrays.asList("minecraft:wall_sign")
    );

    public static ItemStack kit_ifplayer = addPlaceAndBreakTags(
        new ItemBuilder(Material.WOOD).name("§6Если игрок...").lore(Arrays.asList("§7Если игрок ___, делать ___")).build(),
        Arrays.asList("minecraft:chain_command_block"),
        Arrays.asList("minecraft:wall_sign")
    );

    public static ItemStack kit_ifentity = addPlaceAndBreakTags(
        new ItemBuilder(Material.BRICK).name("§2Если моб...").lore(Arrays.asList("§7Если моб ___, делать ___")).build(),
        Arrays.asList("minecraft:chain_command_block"),
        Arrays.asList("minecraft:wall_sign")
    );

    public static ItemStack kit_gameaction = addPlaceAndBreakTags(
        new ItemBuilder(Material.NETHER_BRICK).name("§9Параметры игры").lore(Arrays.asList("§7Разные параметры, которые можно изменять")).build(),
        Arrays.asList("minecraft:chain_command_block"),
        Arrays.asList("minecraft:wall_sign"));

    public static ItemStack kit_else = addPlaceAndBreakTags(
        new ItemBuilder(Material.ENDER_STONE).name("§3Иначе").lore(Arrays.asList("§7Поставьте этот блок после", "§7оператора \"Если...\" для цепочки", "§7Если игрок ___, делать ___, иначе ___")).build(),
        Arrays.asList("minecraft:chain_command_block"),
        Arrays.asList("minecraft:wall_sign")
    );

    public static ItemStack kit_scheduler = addPlaceAndBreakTags(
        new ItemBuilder(Material.EMERALD_BLOCK).name("§aПланировщик").lore(Arrays.asList("§7Выполняет команды спустя время")).build(),
        Arrays.asList("minecraft:chain_command_block", "minecraft:repeating_command_block"),
        Arrays.asList("minecraft:wall_sign")
    );

    public static ItemStack kit_glass = addPlaceAndBreakTags(new ItemStack(Material.STAINED_GLASS, 1, (short) 0), Arrays.asList("minecraft:glass"), Arrays.asList("minecraft:stained_glass", "minecraft:wall_sign"));

    public static ItemStack kit_variable = new ItemBuilder(Material.IRON_INGOT).name("§bПеременные").build();

    public Coding() {
        server.getPluginManager().registerEvents(this, plugin);
    }

    public static String getCleatChat() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb.append(" \n ");
        }
        return sb.toString();
    }

    public static void giveCodingKit(Player player) {
        Inventory inv = player.getInventory();
        inv.setItem(0, kit_playerevent);
        inv.setItem(1, kit_ifplayer);
        inv.setItem(2, kit_playeraction);
        inv.setItem(3, kit_gameaction);

        inv.setItem(28, kit_ifentity);
        inv.setItem(17, kit_glass);


        // inv.setItem(11, kit_warp);
        inv.setItem(10, kit_else);
        inv.setItem(9, kit_scheduler);
        inv.setItem(8, kit_variable);

    }

    public static ItemStack addPlaceAndBreakTags(ItemStack itemStack, List<String> place, List<String> destroy) {
        NBTFactory factory = new NBTFactory(itemStack);
        NBTTagCompound nbt = factory.getNbtTagCompound();

        NBTTagList canplaceon = new NBTTagList();
        place.forEach(string -> canplaceon.add(new NBTTagString(string)));
        NBTTagList candestroy = new NBTTagList();
        destroy.forEach(string -> candestroy.add(new NBTTagString(string)));
        nbt.set("CanPlaceOn", canplaceon);
        nbt.set("CanDestroy", candestroy);
        return factory.build(nbt);
    }


    public static void codingOnPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        World world = block.getWorld();
        com.sk89q.worldedit.world.World faweworld = FaweAPI.getWorld(block.getWorld().getName());
        Location location = block.getLocation();
        switch (block.getType()) {
            case DIAMOND_BLOCK: {
                server.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.getInventory().addItem(kit_playerevent));

                Block signblock = location.clone().add(0, 0, 1).getBlock();
                signblock.setType(Material.WALL_SIGN);
                signblock.setData((byte) 3);
                Sign sign = (Sign) signblock.getState();
                sign.setLine(0, "§lСобытие игрока");
                sign.setLine(1, "§o*Кликни блоком*");
                sign.update();

                Block stoneblock = location.clone().add(1, 0, 0).getBlock();
                stoneblock.setType(Material.DIAMOND_ORE);

                break;
            }

            case EMERALD_BLOCK: {
                server.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.getInventory().addItem(kit_scheduler));

                switch (world.getBlockAt(block.getLocation().add(0, -1, 0)).getType()) {
                    case COMMAND_CHAIN: {
                        Block stoneblock = location.clone().add(1, 0, 0).getBlock();
                        if (stoneblock.getType() != Material.AIR) {
                            Block lastblock = new Location(stoneblock.getWorld(), 1099, stoneblock.getY(), stoneblock.getZ()).getBlock();
                            if (lastblock.getType() != Material.AIR) {
                                event.setCancelled(true);
                                player.sendMessage("Строка полностью забита");
                            } else {
                                Block signblock = location.clone().add(0, 0, 1).getBlock();
                                signblock.setType(Material.WALL_SIGN);
                                signblock.setData((byte) 3);
                                Sign sign = (Sign) signblock.getState();
                                sign.setLine(0, "§lПланировщик");
                                sign.setLine(1, "§o*Кликни блоком*");
                                sign.update();

                                moveBlocks(faweworld,
                                    new Vector(stoneblock.getX(), stoneblock.getY(), stoneblock.getZ()),
                                    new Vector(1099, stoneblock.getY() + 1, stoneblock.getZ() + 1),
                                    new Vector(stoneblock.getX() + 4, stoneblock.getY(), stoneblock.getZ()));

                                stoneblock.setType(Material.PISTON_BASE);
                                stoneblock.setData((byte) 5);
                                stoneblock.getLocation().add(1, 0, 0).getBlock().setType(Material.AIR);

                                Block endpiston = stoneblock.getLocation().add(2, 0, 0).getBlock();
                                if (endpiston.getType() == Material.AIR || endpiston.getType() == Material.STAINED_GLASS) {
                                    endpiston.setType(Material.PISTON_BASE);
                                    endpiston.setData((byte) 4);
                                }
                            }
                        } else {
                            Block signblock = location.clone().add(0, 0, 1).getBlock();
                            signblock.setType(Material.WALL_SIGN);
                            signblock.setData((byte) 3);
                            Sign sign = (Sign) signblock.getState();
                            sign.setLine(0, "§lПланировщик");
                            sign.setLine(1, "§o*Кликни блоком*");
                            sign.update();
                            stoneblock.setType(Material.PISTON_BASE);
                            stoneblock.setData((byte) 5);

                            Block endpiston = stoneblock.getLocation().add(2, 0, 0).getBlock();
                            if (endpiston.getType() == Material.AIR) {
                                endpiston.setType(Material.PISTON_BASE);
                                endpiston.setData((byte) 4);
                            } else {
                                moveBlocks(faweworld,
                                    new Vector(endpiston.getX(), endpiston.getY(), endpiston.getZ()),
                                    new Vector(1100, endpiston.getY() + 1, endpiston.getZ() + 1),
                                    new Vector(endpiston.getX() + 2, endpiston.getY(), endpiston.getZ()));
                                server.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                                    endpiston.setType(Material.PISTON_BASE);
                                    endpiston.setData((byte) 4);
                                });
                            }
                        }
                        break;
                    }
                    case COMMAND_REPEATING: {
                        Block signblock = location.clone().add(0, 0, 1).getBlock();
                        signblock.setType(Material.WALL_SIGN);
                        signblock.setData((byte) 3);
                        Sign sign = (Sign) signblock.getState();
                        sign.setLine(0, "§lПланировщик");
                        sign.setLine(1, "§o*Кликни блоком*");
                        sign.update();

                        Block stoneblock = location.clone().add(1, 0, 0).getBlock();
                        stoneblock.setType(Material.EMERALD_ORE);
                        break;
                    }
                }
                break;
            }

            case COBBLESTONE: {
                server.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.getInventory().addItem(kit_playeraction));

                Block stoneblock = location.clone().add(1, 0, 0).getBlock();
                if (stoneblock.getType() != Material.AIR) {
                    Block lastblock = new Location(stoneblock.getWorld(), 1099, stoneblock.getY(), stoneblock.getZ()).getBlock();
                    if (lastblock.getType() != Material.AIR) {
                        event.setCancelled(true);
                        player.sendMessage("Строка полностью забита");
                    } else {
                        Block signblock = location.clone().add(0, 0, 1).getBlock();
                        signblock.setType(Material.WALL_SIGN);
                        signblock.setData((byte) 3);
                        Sign sign = (Sign) signblock.getState();
                        sign.setLine(0, "§lСделать игроку");
                        sign.setLine(1, "§o*Кликни блоком*");
                        sign.update();

                        moveBlocks(faweworld,
                            new Vector(stoneblock.getX(), stoneblock.getY(), stoneblock.getZ()),
                            new Vector(1099, stoneblock.getY() + 1, stoneblock.getZ() + 1),
                            new Vector(stoneblock.getX() + 2, stoneblock.getY(), stoneblock.getZ())
                        );

                        stoneblock.setType(Material.STONE);
                        stoneblock.getLocation().add(1, 0, 0).getBlock().setType(Material.AIR);
                    }
                } else {
                    Block signblock = location.clone().add(0, 0, 1).getBlock();
                    signblock.setType(Material.WALL_SIGN);
                    signblock.setData((byte) 3);
                    Sign sign = (Sign) signblock.getState();
                    sign.setLine(0, "§lСделать игроку");
                    sign.setLine(1, "§o*Кликни блоком*");
                    sign.update();
                    stoneblock.setType(Material.STONE);
                }
                break;
            }

            case LAPIS_BLOCK: {
                server.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.getInventory().addItem(kit_warp));

                Block signblock = location.clone().add(0, 0, 1).getBlock();
                signblock.setType(Material.WALL_SIGN);
                signblock.setData((byte) 3);
                Sign sign = (Sign) signblock.getState();
                sign.setLine(0, "§lВыполнить строку");
                sign.setLine(1, "§o*Кликни блоком*");
                sign.update();
                if (location.clone().add(0, -1, 0).getBlock().getType() == Material.COMMAND_CHAIN) {
                    Block chestblock = location.clone().add(0, 1, 0).getBlock();
                    chestblock.setType(Material.CHEST);
                    Chest chest = (Chest) chestblock.getState();
                    chest.setCustomName("Число");
                } else {
                    location.clone().add(1, 0, 0).getBlock().setType(Material.LAPIS_ORE);
                }
                break;
            }

            case WOOD: {
                server.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.getInventory().addItem(kit_ifplayer));

                Block stoneblock = location.clone().add(1, 0, 0).getBlock();
                if (stoneblock.getType() != Material.AIR) {
                    Block lastblock = new Location(stoneblock.getWorld(), 1099, stoneblock.getY(), stoneblock.getZ()).getBlock();
                    if (lastblock.getType() != Material.AIR) {
                        event.setCancelled(true);
                        player.sendMessage("Строка полностью забита");
                    } else {
                        Block signblock = location.clone().add(0, 0, 1).getBlock();
                        signblock.setType(Material.WALL_SIGN);
                        signblock.setData((byte) 3);
                        Sign sign = (Sign) signblock.getState();
                        sign.setLine(0, "§lЕсли игрок");
                        sign.setLine(1, "§o*Кликни блоком*");
                        sign.update();

                        moveBlocks(faweworld,
                            new Vector(stoneblock.getX(), stoneblock.getY(), stoneblock.getZ()),
                            new Vector(1099, stoneblock.getY() + 1, stoneblock.getZ() + 1),
                            new Vector(stoneblock.getX() + 4, stoneblock.getY(), stoneblock.getZ()));

                        stoneblock.setType(Material.PISTON_BASE);
                        stoneblock.setData((byte) 5);
                        stoneblock.getLocation().add(1, 0, 0).getBlock().setType(Material.AIR);

                        Block endpiston = stoneblock.getLocation().add(2, 0, 0).getBlock();
                        if (endpiston.getType() == Material.AIR || endpiston.getType() == Material.STAINED_GLASS) {
                            endpiston.setType(Material.PISTON_BASE);
                            endpiston.setData((byte) 4);
                        }
                    }
                } else {
                    Block signblock = location.clone().add(0, 0, 1).getBlock();
                    signblock.setType(Material.WALL_SIGN);
                    signblock.setData((byte) 3);
                    Sign sign = (Sign) signblock.getState();
                    sign.setLine(0, "§lЕсли игрок");
                    sign.setLine(1, "§o*Кликни блоком*");
                    sign.update();
                    stoneblock.setType(Material.PISTON_BASE);
                    stoneblock.setData((byte) 5);

                    Block endpiston = stoneblock.getLocation().add(2, 0, 0).getBlock();
                    if (endpiston.getType() == Material.AIR) {
                        endpiston.setType(Material.PISTON_BASE);
                        endpiston.setData((byte) 4);
                    } else {
                        moveBlocks(faweworld,
                            new Vector(endpiston.getX(), endpiston.getY(), endpiston.getZ()),
                            new Vector(1100, endpiston.getY() + 1, endpiston.getZ() + 1),
                            new Vector(endpiston.getX() + 2, endpiston.getY(), endpiston.getZ()));
                        server.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            endpiston.setType(Material.PISTON_BASE);
                            endpiston.setData((byte) 4);
                        });
                    }
                }

                break;
            }

            case ENDER_STONE: {
                server.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.getInventory().addItem(kit_else));

                Block stoneblock = location.clone().add(1, 0, 0).getBlock();
                if (stoneblock.getType() != Material.AIR) {
                    Block lastblock = new Location(stoneblock.getWorld(), 1099, stoneblock.getY(), stoneblock.getZ()).getBlock();
                    if (lastblock.getType() != Material.AIR) {
                        event.setCancelled(true);
                        player.sendMessage("Строка полностью забита");
                    } else {
                        Block signblock = location.clone().add(0, 0, 1).getBlock();
                        signblock.setType(Material.WALL_SIGN);
                        signblock.setData((byte) 3);
                        Sign sign = (Sign) signblock.getState();
                        sign.setLine(0, "§lИначе");
                        sign.update();

                        moveBlocks(faweworld,
                            new Vector(stoneblock.getX(), stoneblock.getY(), stoneblock.getZ()),
                            new Vector(1099, stoneblock.getY() + 1, stoneblock.getZ() + 1),
                            new Vector(stoneblock.getX() + 4, stoneblock.getY(), stoneblock.getZ()));

                        stoneblock.setType(Material.PISTON_BASE);
                        stoneblock.setData((byte) 5);
                        stoneblock.getLocation().add(1, 0, 0).getBlock().setType(Material.AIR);

                        Block endpiston = stoneblock.getLocation().add(2, 0, 0).getBlock();
                        if (endpiston.getType() == Material.AIR || endpiston.getType() == Material.STAINED_GLASS) {
                            endpiston.setType(Material.PISTON_BASE);
                            endpiston.setData((byte) 4);
                        }
                    }
                } else {
                    Block signblock = location.clone().add(0, 0, 1).getBlock();
                    signblock.setType(Material.WALL_SIGN);
                    signblock.setData((byte) 3);
                    Sign sign = (Sign) signblock.getState();
                    sign.setLine(0, "§lИначе");
                    sign.update();
                    stoneblock.setType(Material.PISTON_BASE);
                    stoneblock.setData((byte) 5);

                    Block endpiston = stoneblock.getLocation().add(2, 0, 0).getBlock();
                    if (endpiston.getType() == Material.AIR) {
                        endpiston.setType(Material.PISTON_BASE);
                        endpiston.setData((byte) 4);
                    } else {
                        moveBlocks(faweworld,
                            new Vector(endpiston.getX(), endpiston.getY(), endpiston.getZ()),
                            new Vector(1100, endpiston.getY() + 1, endpiston.getZ() + 1),
                            new Vector(endpiston.getX() + 2, endpiston.getY(), endpiston.getZ()));
                        server.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            endpiston.setType(Material.PISTON_BASE);
                            endpiston.setData((byte) 4);
                        });
                    }
                }
                break;
            }

            case NETHER_BRICK: {
                server.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.getInventory().addItem(kit_gameaction));


                Block stoneblock = location.clone().add(1, 0, 0).getBlock();
                if (stoneblock.getType() != Material.AIR) {
                    Block lastblock = new Location(stoneblock.getWorld(), 1099, stoneblock.getY(), stoneblock.getZ()).getBlock();
                    if (lastblock.getType() != Material.AIR) {
                        event.setCancelled(true);
                        player.sendMessage("Строка полностью забита");
                    } else {
                        Block signblock = location.clone().add(0, 0, 1).getBlock();
                        signblock.setType(Material.WALL_SIGN);
                        signblock.setData((byte) 3);
                        Sign sign = (Sign) signblock.getState();
                        sign.setLine(0, "§lПараметры игры");
                        sign.setLine(1, "§o*Кликни блоком*");
                        sign.update();
                        try {
                            EditSession copy = new EditSessionBuilder(faweworld).fastmode(true).build();
                            EditSession set = new EditSessionBuilder(faweworld).fastmode(true).build();
                            EditSession paste = new EditSessionBuilder(faweworld).fastmode(true).build();

                            com.sk89q.worldedit.Vector pos1 = new Vector(stoneblock.getX(), stoneblock.getY(), stoneblock.getZ());
                            com.sk89q.worldedit.Vector pos2 = new Vector(1099, stoneblock.getY() + 1, stoneblock.getZ() + 1);
                            CuboidRegion region = new CuboidRegion(pos1, pos2);
                            BlockArrayClipboard lazyCopy = copy.lazyCopy(region);
                            Schematic schem = new Schematic(lazyCopy);

                            Vector to = new Vector(stoneblock.getX() + 2, stoneblock.getY(), stoneblock.getZ());
                            schem.paste(paste, to, true);
                            paste.flushQueue();

                            stoneblock.setType(Material.NETHERRACK);
                            stoneblock.getLocation().add(1, 0, 0).getBlock().setType(Material.AIR);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Block signblock = location.clone().add(0, 0, 1).getBlock();
                    signblock.setType(Material.WALL_SIGN);
                    signblock.setData((byte) 3);
                    Sign sign = (Sign) signblock.getState();
                    sign.setLine(0, "§lПараметры игры");
                    sign.setLine(1, "§o*Кликни блоком*");
                    sign.update();
                    stoneblock.setType(Material.NETHERRACK);
                }
                break;
            }

            case BRICK: {
                server.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.getInventory().addItem(kit_ifentity));

                Block stoneblock = location.clone().add(1, 0, 0).getBlock();
                if (stoneblock.getType() != Material.AIR) {
                    Block lastblock = new Location(stoneblock.getWorld(), 1099, stoneblock.getY(), stoneblock.getZ()).getBlock();
                    if (lastblock.getType() != Material.AIR) {
                        event.setCancelled(true);
                        player.sendMessage("Строка полностью забита");
                    } else {
                        Block signblock = location.clone().add(0, 0, 1).getBlock();
                        signblock.setType(Material.WALL_SIGN);
                        signblock.setData((byte) 3);
                        Sign sign = (Sign) signblock.getState();
                        sign.setLine(0, "§lЕсли моб");
                        sign.setLine(1, "§o*Кликни блоком*");
                        sign.update();

                        moveBlocks(faweworld,
                            new Vector(stoneblock.getX(), stoneblock.getY(), stoneblock.getZ()),
                            new Vector(1099, stoneblock.getY() + 1, stoneblock.getZ() + 1),
                            new Vector(stoneblock.getX() + 4, stoneblock.getY(), stoneblock.getZ()));

                        stoneblock.setType(Material.PISTON_BASE);
                        stoneblock.setData((byte) 5);
                        stoneblock.getLocation().add(1, 0, 0).getBlock().setType(Material.AIR);

                        Block endpiston = stoneblock.getLocation().add(2, 0, 0).getBlock();
                        if (endpiston.getType() == Material.AIR || endpiston.getType() == Material.STAINED_GLASS) {
                            endpiston.setType(Material.PISTON_BASE);
                            endpiston.setData((byte) 4);
                        }
                    }
                } else {
                    Block signblock = location.clone().add(0, 0, 1).getBlock();
                    signblock.setType(Material.WALL_SIGN);
                    signblock.setData((byte) 3);
                    Sign sign = (Sign) signblock.getState();
                    sign.setLine(0, "§lЕсли моб");
                    sign.setLine(1, "§o*Кликни блоком*");
                    sign.update();
                    stoneblock.setType(Material.PISTON_BASE);
                    stoneblock.setData((byte) 5);

                    Block endpiston = stoneblock.getLocation().add(2, 0, 0).getBlock();
                    if (endpiston.getType() == Material.AIR) {
                        endpiston.setType(Material.PISTON_BASE);
                        endpiston.setData((byte) 4);
                    } else {
                        moveBlocks(faweworld,
                            new Vector(endpiston.getX(), endpiston.getY(), endpiston.getZ()),
                            new Vector(1100, endpiston.getY() + 1, endpiston.getZ() + 1),
                            new Vector(endpiston.getX() + 2, endpiston.getY(), endpiston.getZ()));
                        server.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            endpiston.setType(Material.PISTON_BASE);
                            endpiston.setData((byte) 4);
                        });
                    }
                }
                break;
            }

            case STAINED_GLASS: {
                server.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.getInventory().addItem(kit_glass));
                break;
            }
        }
    }

    public static void codingOnClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

    }

    public static void codingOnBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();
        World world = location.getWorld();
        com.sk89q.worldedit.world.World faweworld = FaweAPI.getWorld(block.getWorld().getName());

        switch (block.getType()) {
            case WALL_SIGN: {
                Block mainblock = location.clone().add(0, 0, -1).getBlock();
                switch (mainblock.getType()) {
                    case EMERALD_BLOCK:
                    case ENDER_STONE:
                    case WOOD: {
                        Location pistonbegin = mainblock.getLocation().clone().add(1, 0, 0);
                        Location pistonend = getPistonEndFrom(pistonbegin) != null ? getPistonEndFrom(pistonbegin) : pistonbegin.clone().add(100, 0, 0);
                        for (int x = pistonbegin.getBlockX(); x < pistonend.getBlockX(); x++) {
                            world.getBlockAt(x, 2, pistonbegin.getBlockZ()).setType(Material.AIR);
                        }
                        setBlocks(faweworld, new Vector(pistonbegin.getX() - 1, 1, pistonbegin.getZ()), new Vector(pistonend.getX(), 1, pistonend.getBlockZ() + 1), Material.AIR);
                        server.getScheduler().scheduleSyncDelayedTask(plugin, () -> moveBlocks(faweworld,
                            new Vector(pistonend.getX() + 1, 1, pistonend.getZ()),
                            new Vector(1100, 2, pistonend.getZ() + 1),
                            new Vector(new Vector(pistonbegin.getX() - 1, 1, pistonbegin.getZ()))));
                        break;
                    }
                    case BRICK: {
                        Location pistonbegin = mainblock.getLocation().clone().add(1, 0, 0);
                        Location pistonend = getPistonEndFrom(pistonbegin) != null ? getPistonEndFrom(pistonbegin) : pistonbegin.clone().add(100, 0, 0);
                        for (int x = pistonbegin.getBlockX(); x < pistonend.getBlockX(); x++) {
                            world.getBlockAt(x, 2, pistonbegin.getBlockZ()).setType(Material.AIR);
                        }
                        setBlocks(faweworld, new Vector(pistonbegin.getX() - 1, 1, pistonbegin.getZ()), new Vector(pistonend.getX(), 1, pistonend.getBlockZ() + 1), Material.AIR);
                        server.getScheduler().scheduleSyncDelayedTask(plugin, () -> moveBlocks(faweworld,
                            new Vector(pistonend.getX() + 1, 1, pistonend.getZ()),
                            new Vector(1100, 2, pistonend.getZ() + 1),
                            new Vector(new Vector(pistonbegin.getX() - 1, 1, pistonbegin.getZ()))));
                        break;
                    }
                    case DIAMOND_BLOCK: {
                        for (int i = 0; i < 100; i++) {
                            Block chestblock = location.clone().add(i, 1, -1).getBlock();
                            chestblock.setType(Material.AIR);
                        }
                        server.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            EditSession session = new EditSessionBuilder(faweworld).fastmode(true).build();
                            com.sk89q.worldedit.Vector pos1 = new Vector(block.getX(), block.getY(), block.getZ() - 1);
                            com.sk89q.worldedit.Vector pos2 = new Vector(1099, block.getY(), block.getZ());
                            CuboidRegion region = new CuboidRegion(pos1, pos2);
                            try {
                                session.setBlocks(region, new BaseBlock(0));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            session.flushQueue();
                        });
                        break;
                    }
                    default: {
                        server.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            EditSession copy = new EditSessionBuilder(faweworld).fastmode(true).build();
                            EditSession paste = new EditSessionBuilder(faweworld).fastmode(true).build();

                            com.sk89q.worldedit.Vector pos1 = new Vector(block.getX() + 2, block.getY(), block.getZ() - 1);
                            com.sk89q.worldedit.Vector pos2 = new Vector(1099, block.getY() + 1, block.getZ());
                            CuboidRegion region = new CuboidRegion(pos1, pos2);
                            BlockArrayClipboard lazyCopy = copy.lazyCopy(region);
                            Schematic schem = new Schematic(lazyCopy);

                            Vector to = new Vector(block.getX(), block.getY(), block.getZ() - 1);
                            schem.paste(paste, to, true);
                            paste.flushQueue();
                        });
                    }
                }
                break;
            }
        }
    }

    public static void playingOnDeath(PlayerDeathEvent event) {
        event.setKeepInventory(true);
        Player player = event.getEntity();
        World world = player.getWorld();
        UUID eventid = UUID.randomUUID();
        CodeEventHandler codeEventHandler = codeMap.get(world);
        if (codeEventHandler == null) return;
        Location location = player.getLocation();
        codeEventHandler.runCode("СобытиеИгрока_Смерть", eventid, player, null);
        if (!player.getScoreboardTags().contains("keepInventory")) {
            List<ItemStack> items = Arrays.asList(player.getInventory().getContents());
            player.getInventory().clear();
            items.forEach(itemStack -> {
                if (itemStack != null) world.dropItemNaturally(location, itemStack);
            });
            event.setKeepInventory(false);
        }
    }

    public static boolean cancelEvent(Cancellable event, UUID eventid) {
        if (canceledEvents.contains(eventid)) {
            event.setCancelled(true);
            canceledEvents.remove(eventid);
            return true;
        }
        return false;
    }

    public static void moveBlocks(com.sk89q.worldedit.world.World world, Vector pos1, Vector pos2, Vector pasteto) {
        EditSession copy = new EditSessionBuilder(world).fastmode(true).build();
        EditSession paste = new EditSessionBuilder(world).fastmode(true).build();

        CuboidRegion region = new CuboidRegion(pos1, pos2);
        BlockArrayClipboard lazyCopy = copy.lazyCopy(region);
        Schematic schem = new Schematic(lazyCopy);

        schem.paste(paste, pasteto, true);
        paste.flushQueue();
    }

    public static void setBlocks(com.sk89q.worldedit.world.World world, Vector pos1, Vector pos2, Material material) {
        EditSession session = new EditSessionBuilder(world).fastmode(true).build();
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        try {
            session.setBlocks(region, new BaseBlock(material.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        session.flushQueue();
    }

    public static Location getPistonEndFrom(Location location) {
        Location loc = location.clone();
        int ignoreblock = 0;
        for (int checkx = loc.getBlockX() + 1; checkx < 1150; checkx++) {
            loc.setX(checkx);
            Block checkblock = loc.getBlock();
            if (checkblock.getType() == Material.PISTON_BASE) {
                if (checkblock.getData() == (byte) 5) {
                    ignoreblock++;
                }
                if (checkblock.getData() == (byte) 4) {
                    if (ignoreblock == 0) {
                        return checkblock.getLocation();
                    } else ignoreblock--;
                }
            }
        }
        return null;
    }

    public static int getCmdNumberByX(int x) {
        return (x - 1003) / 2;
    }

    /*
    int igonreelse = 0;
        for (int i = number; i < commands.size(); i++) {
            String cmd = commands.get(i);
            // server.broadcastMessage("§3Обрабатываем: "+cmd);
            if (i != number && cmd.startsWith("Если")) igonreelse++;
            if (cmd.startsWith("Иначе")) {
                if (igonreelse == 0) {
                    boolean canrun = true;
                    //server.broadcastMessage("§bМожем выполнить?: "+canrun);
                    for (int x = i + 1; x < commands.size(); x++) {
                        if (canrun) canrun = runCommand(line, x, commands.get(x).split(";"), eventid, player, entity);
                        else break;
                    }
                    break;
                } else igonreelse--;
            }
        }
     */

    public static int getXbyCmdNumber(int number) {
        return number * 2 + 1003;
    }

    public static int getZbyLine(int line) {
        return line * 3 + 1001;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void codingOnChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage().replace("&", "§").replace("§§", "&");
        if (!WorldManager.getWorldType(player.getWorld()).equals("coding") && player.getGameMode() != GameMode.CREATIVE)
            return;
        PlayerInventory inv = player.getInventory();
        ItemStack item = inv.getItemInMainHand();
        if (item == null) return;
        switch (item.getType()) {
            case BOOK: {
                event.setCancelled(true);
                inv.setItemInMainHand(new ItemBuilder(item).name(msg).build());
                player.sendTitle("§aУстановлен текст:", msg, 0, 20 * 3, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, Integer.MAX_VALUE, 1);
                break;
            }
            case SLIME_BALL: {
                event.setCancelled(true);
                try {
                    double number = Double.parseDouble(msg);
                    if (number > 65535) {
                        player.sendTitle("§cОШИБКА!", "Слишком большое число", 0, 20 * 3, 10);
                        player.sendMessage("Если вы используете время, попробуйте задавать число в формате 10s или 20m (10 секунд или 20 минут)");
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, Integer.MAX_VALUE, 0.5f);

                    }
                } catch (NumberFormatException ex) {
                    if (msg.endsWith("s") || msg.endsWith("m") || msg.endsWith("h") || msg.endsWith("d")) {
                        player.sendTitle("§aУстановлено число:", msg, 0, 20 * 3, 10);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, Integer.MAX_VALUE, 1);
                        inv.setItemInMainHand(new ItemBuilder(item).name(msg).build());
                        return;
                    }
                    player.sendTitle("§cОШИБКА!", "В сообщении содержаться буквы", 0, 20 * 3, 10);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, Integer.MAX_VALUE, 0.5f);
                    return;
                }
                player.sendTitle("§aУстановлено число:", msg, 0, 20 * 3, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, Integer.MAX_VALUE, 1);
                inv.setItemInMainHand(new ItemBuilder(item).name(msg).build());
                break;
            }
        }
    }

}

