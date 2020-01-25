package io.github.thebusybiscuit.sensibletoolbox.blocks.machines;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapedRecipe;

import io.github.thebusybiscuit.sensibletoolbox.api.items.AutoFarmingMachine;
import io.github.thebusybiscuit.sensibletoolbox.items.GoldCombineHoe;
import io.github.thebusybiscuit.sensibletoolbox.items.components.MachineFrame;
import me.mrCookieSlime.CSCoreLibPlugin.CSCoreLib;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;

public class AutoFarm2 extends AutoFarmingMachine {
	
    private static final Map<Material, Material> crops = new HashMap<>();
    private static final int RADIUS = 5;
    
    static {
    	crops.put(Material.COCOA, Material.COCOA_BEANS);
    	crops.put(Material.SUGAR_CANE, Material.SUGAR_CANE);
    	crops.put(Material.CACTUS, Material.CACTUS);
    }
    
    private Set<Block> blocks;
    private Material buffer;

    public AutoFarm2() {
        blocks = new HashSet<>();
    }

    public AutoFarm2(ConfigurationSection conf) {
        super(conf);
        blocks = new HashSet<>();
    }
    
    @Override
    public Material getMaterial() {
        return Material.BROWN_TERRACOTTA;
    }

    @Override
    public String getItemName() {
        return "Auto Farm MkII";
    }

    @Override
    public String[] getLore() {
        return new String[] {
                "Automatically harvests and replants",
                "Cocoa Beans/Sugar Cane/Cactus",
                "in a " + RADIUS + "x" + RADIUS + " Radius 2 Blocks above the Machine"
        };
    }

    @Override
    public Recipe getRecipe() {
    	MachineFrame frame = new MachineFrame();
    	GoldCombineHoe hoe = new GoldCombineHoe();
    	registerCustomIngredients(frame, hoe);
        ShapedRecipe res = new ShapedRecipe(getKey(), toItemStack());
        res.shape("LHL", "IFI", "RGR");
        res.setIngredient('R', Material.REDSTONE);
        res.setIngredient('G', Material.GOLD_INGOT);
        res.setIngredient('I', Material.IRON_INGOT);
        res.setIngredient('L', new MaterialChoice(Tag.LOGS));
        res.setIngredient('H', hoe.getMaterial());
        res.setIngredient('F', frame.getMaterial());
        return res;
    }
    
    @Override
    public void onBlockRegistered(Location location, boolean isPlacing) {
    	int i = RADIUS / 2;
    	for (int x = -i; x <= i; x++) {
    		for (int z = -i; z <= i; z++) {
        		blocks.add(new Location(location.getWorld(), location.getBlockX() + x, location.getBlockY() + 2, location.getBlockZ() + z).getBlock());
        	}
    	}
    	
    	super.onBlockRegistered(location, isPlacing);
    }
    
	@Override
    public void onServerTick() {
    	if (!isJammed()) {
    		for (Block crop : blocks) {
        		if (crops.containsKey(crop.getType())) {
        			if (crop.getType() == Material.COCOA) {
        				Cocoa cocoa = (Cocoa) crop.getBlockData();
        				if (cocoa.getAge() >= cocoa.getMaximumAge()) {
        					if (getCharge() >= getScuPerCycle()) setCharge(getCharge() - getScuPerCycle());
                			else break;
        					
                			cocoa.setAge(0);
                			crop.getWorld().playEffect(crop.getLocation(), Effect.STEP_SOUND, crop.getType());
                    		setJammed(!output(crops.get(crop.getType())));
                			break;
        				}
        			}
        			else {
        				Block block = crop.getRelative(BlockFace.UP);
        				if (crops.containsKey(block.getType()) && block.getType() != Material.COCOA) {
        					if (getCharge() >= getScuPerCycle()) setCharge(getCharge() - getScuPerCycle());
                			else break;
        					block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
                    		setJammed(!output(crops.get(block.getType())));
        					block.setType(Material.AIR);
                			break;
        				}
        			}
        		}
        	}
    	}
    	else if (buffer != null) setJammed(!output(buffer));
    	
        super.onServerTick();
    }
    
	private boolean output(Material m) {
		for (int slot: getOutputSlots()) {
			ItemStack stack = getInventoryItem(slot);
			if (stack == null || (stack.getType() == m && stack.getAmount() < stack.getMaxStackSize())) {
				if (stack == null) stack = new ItemStack(m);
				int amount = (stack.getMaxStackSize() - stack.getAmount()) > 3 ? (CSCoreLib.randomizer().nextInt(2) + 1): (stack.getMaxStackSize() - stack.getAmount());
				setInventoryItem(slot, new CustomItem(stack, stack.getAmount() + amount));
				buffer = null;
				return true;
			}
		}
		return false;
	}
	
	@Override
    public double getScuPerCycle() {
        return 30.0;
    }
}
