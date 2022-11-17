package me.noverita.miningworld;

import com.onarandombox.MultiverseCore.MVWorld;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.*;

public class MiningWorld extends JavaPlugin implements Listener {
    private MultiverseCore mvcore;
    private MultiverseWorld miningWorld;
    private MultiverseWorld baseWorld;

    private Map<Player, Location> entranceLocation;

    @Override
    public void onEnable() {
        Bukkit.getLogger().info("MiningWorld Enabled.");
        mvcore = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
        Bukkit.getPluginManager().registerEvents(this, this);

        FileConfiguration config = this.getConfig();
        config.addDefault("miningWorld","underground2");
        config.addDefault("baseWorld","ezruahfinal");
        String mWorld = config.getString("miningWorld");
        String bWorld = config.getString("baseWorld");

        miningWorld = mvcore.getMVWorldManager().getMVWorld(mWorld);
        if (miningWorld == null) {
            Bukkit.getLogger().info("Could not find mining world.");
            setEnabled(false);
        }

        baseWorld = mvcore.getMVWorldManager().getMVWorld(bWorld);
        if (baseWorld == null) {
            Bukkit.getLogger().info("Could not find base world.");
            setEnabled(false);
        }

        try {
            config.save(getDataFolder().getAbsolutePath() + "mining_config.yml");
        } catch (IOException e) {
            e.printStackTrace();
        }

        entranceLocation = new HashMap<>();
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("MiningWorld disabled.");
    }

    @EventHandler
    public void onPlaceSign(SignChangeEvent event) {
        String line = event.getLine(0);
        if (line != null && line.equalsIgnoreCase("[Mine]")) {
            event.setLine(0,"[Mine]");
        }
    }

    @EventHandler
    public void onBreakSign(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (b.getType().name().endsWith("_SIGN")) {
            Sign sign = (Sign) b.getState();
            if (sign.getLine(0).equalsIgnoreCase("[Mine]")) {
                if (b.getWorld().equals(baseWorld.getCBWorld())) {
                    Location loc = b.getLocation();
                    miningWorld.getCBWorld().getBlockAt(loc.getBlockX(), 319, loc.getBlockZ()).setType(Material.BEDROCK);
                }
            }
        }
    }

    @EventHandler
    public void onClickMineSign(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType().name().endsWith("_SIGN")) {
            Sign sign = (Sign) event.getClickedBlock().getState();
            if (sign.getLine(0).equalsIgnoreCase("[Mine]")) {

                Location loc = event.getClickedBlock().getLocation();
                loc.setWorld(miningWorld.getCBWorld());
                loc.setY(310);
                
                World cbWorld = miningWorld.getCBWorld();

                for (int x = -2; x < 3; ++x) {
                    for (int y = 0; y < 3; ++y) {
                        for (int z = -2; z < 3; ++z) {
                            Block b = cbWorld.getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                            if (!(b.getState() instanceof InventoryHolder)) {
                                b.setType(Material.AIR);
                            }
                        }
                    }
                }

                for (int x = -2; x < 3; ++x) {
                    for (int z = -2; z < 3; ++z) {
                        Block b = cbWorld.getBlockAt(loc.getBlockX() + x, loc.getBlockY() - 1, loc.getBlockZ() + z);
                        if (!(b.getState() instanceof InventoryHolder)) {
                            b.setType(Material.OAK_PLANKS);
                        }
                    }
                }

                for (int y = loc.getBlockY(); y < 319; ++y) {
                    cbWorld.getBlockAt(loc.getBlockX() + 1, y, loc.getBlockZ()).setType(Material.OAK_LOG);
                    Block ladder = cbWorld.getBlockAt(loc.getBlockX(), y, loc.getBlockZ());
                    ladder.setType(Material.LADDER);
                    Ladder ladderData = (Ladder) ladder.getBlockData();
                    ladderData.setFacing(BlockFace.WEST);
                    ladder.setBlockData(ladderData);
                }
                
                Block ladder = cbWorld.getBlockAt(loc.getBlockX(), 310, loc.getBlockZ());
                ladder.setType(Material.LADDER);
                Ladder ladderData = (Ladder) ladder.getBlockData();
                ladderData.setFacing(BlockFace.WEST);
                ladder.setBlockData(ladderData);

                mvcore.teleportPlayer(null, event.getPlayer(), loc);
                entranceLocation.put(event.getPlayer(), event.getClickedBlock().getLocation());
            }
        } else if (event.getItem() != null && event.getItem().getType() == Material.LEAD) {
            String name = event.getItem().getItemMeta().getDisplayName();
            if (name.equalsIgnoreCase("Escape Rope")) {
                Location b = entranceLocation.remove(event.getPlayer());
                if (b != null) {
                    event.getPlayer().teleport(b);
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location loc = event.getPlayer().getEyeLocation();
        if (loc.getY() > 318) {
            if (loc.getWorld().equals(miningWorld.getCBWorld())) {

                World mainWorld = baseWorld.getCBWorld();

                int x = loc.getBlockX();
                int z = loc.getBlockZ();

                int airY = mainWorld.getMinHeight();

                for (int y = mainWorld.getMinHeight(); y < mainWorld.getMaxHeight(); ++y) {
                    Block b = mainWorld.getBlockAt(x, y, z);
                    if (b.getType().name().endsWith("_SIGN")) {
                        Sign sign = (Sign) b.getState();
                        if (sign.getLine(0).equalsIgnoreCase("[mine]")) {
                            loc.setWorld(mainWorld);
                            loc.setY(airY + 1);
                            mvcore.teleportPlayer(null, event.getPlayer(), loc);
                            entranceLocation.remove(event.getPlayer());
                        }
                    } else if (!b.getType().isAir()) {
                        airY = y;
                    }
                }

            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        entranceLocation.remove(event.getPlayer());
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (event.getWorld().equals(miningWorld.getCBWorld())) {
            Collection<ItemStack> loot = new LinkedList<>();
            ItemStack is = new ItemStack(Material.GOLD_INGOT);
            is.setAmount(6);
            loot.add(is);
            event.setLoot(loot);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getCaught() instanceof Item) {
            Item item = (Item) event.getCaught();
            if (item.getItemStack().getType() == Material.ENCHANTED_BOOK) {
                ItemStack is = new ItemStack(Material.PAPER);
                ItemMeta meta = is.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + "Water Damaged Paper");
                List<String> lore = new LinkedList<>();
                lore.add(ChatColor.RESET + "" +ChatColor.GRAY + "You can't read this, it has been too damaged by the water.");
                meta.setLore(lore);
                for (Enchantment enchantment: meta.getEnchants().keySet()) {
                    meta.removeEnchant(enchantment);
                }
                is.setItemMeta(meta);
                item.setItemStack(is);
            }
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        event.setCancelled(true);
        event.getEnchanter().sendMessage(ChatColor.RED + "Sorry, enchantments are not currently available.");
        Bukkit.getLogger().info(String.format("%s has tried to enchant.",event.getEnchanter().getName()));
    }

    /*@EventHandler
    public void onBreakLog(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (b.getType().name().endsWith("_LOG") || b.getType().name().endsWith("_WOOD")) {
            for (BlockFace bf: BlockFace.values()) {
                Block b2 = b.getRelative(bf);
                if (b2.getBlockData() instanceof Leaves) {
                    Leaves leaves = (Leaves) b2.getBlockData();
                    leaves.setPersistent(false);
                }
            }
        }
    }

    @EventHandler
    public void onLeafDecay(LeavesDecayEvent event) {
        Block b = event.getBlock();
        for (BlockFace bf: BlockFace.values()) {
            Block b2 = b.getRelative(bf);
            if (b2.getBlockData() instanceof Leaves) {
                Leaves leaves = (Leaves) b2.getBlockData();
                leaves.setPersistent(false);
            }
        }
    }*/
}
