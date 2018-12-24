package me.tango.tollgate.listeners;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Directional;
import org.bukkit.metadata.FixedMetadataValue;

import me.tango.tollgate.Main;
import me.tango.tollgate.utilities.ConfigManager;

public class SignClick implements Listener {

	static Main plugin;
	public SignClick(Main plugin) {
		this.plugin = plugin;
	}

	static ConfigManager conf = new ConfigManager(plugin);
	double signCost = plugin.getInstance().getConfig().getDouble("signCost");
	String signHeader = ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "Tollgate" + ChatColor.DARK_GRAY + "]";
	String signFacingError = plugin.getInstance().getConfig().getString("signFacingError");
	String invalidPriceError = plugin.getInstance().getConfig().getString("invalidPriceError");
	String incompleteSignError = plugin.getInstance().getConfig().getString("incompleteSignError");
	String notEnoughMoney = plugin.getInstance().getConfig().getString("notEnoughMoney");
	String successfullyMade = plugin.getInstance().getConfig().getString("successfullyMade");
	String removedTollgate = plugin.getInstance().getConfig().getString("removedTollgate");
	String unableToTeleportError = plugin.getInstance().getConfig().getString("unableToTeleportError");
	String usedOwnTollgate = plugin.getInstance().getConfig().getString("usedOwnTollgate");
	String usedOtherTollgate = plugin.getInstance().getConfig().getString("usedOtherTollgate");
	String noPermission = plugin.getInstance().getConfig().getString("noPermission");
	String doNotOwnTollgate = plugin.getInstance().getConfig().getString("doNotOwnTollgate");
	String symbol = getSymbol() + "";


	@EventHandler
	public void onSign(SignChangeEvent e) {
		Player p = e.getPlayer();
		float dir = (float) Math.toDegrees(Math.atan2(p.getLocation().getBlockX() - e.getBlock().getX(), e.getBlock().getZ() - p.getLocation().getBlockZ()));
		BlockFace face = getClosestFace(dir);
		String ID = "" + e.getBlock().getX() + e.getBlock().getY() + e.getBlock().getZ();

		if (e.getLine(0).equalsIgnoreCase("[tollgate]")){

			if(!p.hasPermission("tollgate.create")) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermission));
				e.getBlock().breakNaturally();
				return;
			}

			if(plugin.economy.getBalance(p) < signCost) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', notEnoughMoney));
				e.getBlock().breakNaturally();
				return;
			}

			if(e.getLine(1) == null) {
				e.getBlock().breakNaturally();
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', incompleteSignError));
				return;
			}

			if(!isNumeric(e.getLine(1)) || e.getLine(1).contains("-")) {
				e.getBlock().breakNaturally();
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidPriceError));
				return;
			}

			if(e.getLine(0).equals(signHeader) && !(face == BlockFace.NORTH  || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST)) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', signFacingError));
				e.getBlock().breakNaturally();
				return;
			}

			e.getBlock().setMetadata(ID, new FixedMetadataValue(plugin, e.getBlock()));
			e.setLine(0, signHeader);
			e.setLine(2, setCurrency(e.getLine(1)));
			e.setLine(1, p.getName());

			int x = e.getBlock().getX();
			int y = e.getBlock().getY();
			int z = e.getBlock().getZ();
			String world = e.getBlock().getWorld().getName();


			if(plugin.economy.getBalance(p) >= 	signCost) {
				plugin.economy.withdrawPlayer(p, signCost);
			}

			//int i = conf.getCustomConfig().getConfigurationSection("Tollgates." + p.getUniqueId()).getKeys(false).size(); i++;
			conf.getCustomConfig().set("Tollgates." + p.getUniqueId() + "." + ID + ".x" , x);
			conf.getCustomConfig().set("Tollgates." + p.getUniqueId() + "." + ID + ".y" , y);
			conf.getCustomConfig().set("Tollgates." + p.getUniqueId() + "." + ID + ".z" , z);
			conf.getCustomConfig().set("Tollgates." + p.getUniqueId() + "." + ID + ".world" , world);
			conf.saveCustomConfig();
			String msg = ChatColor.translateAlternateColorCodes('&', successfullyMade);
			p.sendMessage(msg.replace("%cost%","" + setCurrency(signCost + "")));
		}
	}




	@SuppressWarnings("deprecation")
	@EventHandler
	public void signClick(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if ((e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && ((e.getClickedBlock().getState() instanceof Sign))) {
			Sign sign = (Sign) e.getClickedBlock().getState();
			float dir = (float) Math.toDegrees(Math.atan2(p.getLocation().getBlockX() - sign.getX(), sign.getZ() - p.getLocation().getBlockZ()));
			BlockFace face = getClosestFace(dir);
			if(face != BlockFace.valueOf(getDirection(p))) {
				return;
			}

			if (sign.getLine(0).equals(signHeader)){

				if(!p.hasPermission("tollgate.use")) {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermission));
					return;
				}

				double cost = getPrice(sign.getLine(2));
				if(p.getName().equalsIgnoreCase(sign.getLine(1))) {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', usedOwnTollgate));
					teleport(p, sign);
				} else {
					if(!plugin.economy.has(p, cost)) {
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', notEnoughMoney));
						return;
					} 
					if(plugin.economy.has(p, cost)) {
						teleport(p, sign);
					}
				}
			}
		}
	}

	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		Player p = e.getPlayer();

		if(e.getBlock().getRelative(BlockFace.NORTH).getState() instanceof Sign && e.getBlock().getRelative(BlockFace.NORTH).getType() == Material.WALL_SIGN) {
			Sign s = (Sign) e.getBlock().getRelative(BlockFace.NORTH).getState();
			if(s.getLine(0).equals(signHeader) && p.hasPermission("tollgate.remove")) {
				String ID = "" + s.getX() + s.getY() + s.getZ();
				if(conf.getCustomConfig().contains("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID)) {
					conf.getCustomConfig().set("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID, null);
					conf.saveCustomConfig();
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
				}
				return;
			}
			if(s.getLine(0).equals(signHeader) && !s.getLine(1).equals(p.getName())) {
				e.setCancelled(true);
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', doNotOwnTollgate));
			} else {
				if(s.getLine(0).equals(signHeader) && s.getLine(1).equals(p.getName())) {
					String ID = "" + s.getX() + s.getY() + s.getZ();
					if(conf.getCustomConfig().contains("Tollgates." + p.getUniqueId() + "." + ID)) {
						conf.getCustomConfig().set("Tollgates." + p.getUniqueId() + "." + ID, null);
						conf.saveCustomConfig();
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
					}
				}
			}
		}

		if(e.getBlock().getRelative(BlockFace.EAST).getState() instanceof Sign && e.getBlock().getRelative(BlockFace.EAST).getType() == Material.WALL_SIGN) {
			Sign s = (Sign) e.getBlock().getRelative(BlockFace.EAST).getState();
			if(s.getLine(0).equals(signHeader) && p.hasPermission("tollgate.remove")) {
				String ID = "" + s.getX() + s.getY() + s.getZ();
				if(conf.getCustomConfig().contains("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID)) {
					conf.getCustomConfig().set("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID, null);
					conf.saveCustomConfig();
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
				}
				return;
			}
			if(s.getLine(0).equals(signHeader) && !s.getLine(1).equals(p.getName())) {
				e.setCancelled(true);
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', doNotOwnTollgate));
			} else {
				if(s.getLine(0).equals(signHeader) && s.getLine(1).equals(p.getName())) {
					String ID = "" + s.getX() + s.getY() + s.getZ();
					if(conf.getCustomConfig().contains("Tollgates." + p.getUniqueId() + "." + ID)) {
						conf.getCustomConfig().set("Tollgates." + p.getUniqueId() + "." + ID, null);
						conf.saveCustomConfig();
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
					}
				}
			}
		}

		if(e.getBlock().getRelative(BlockFace.SOUTH).getState() instanceof Sign && e.getBlock().getRelative(BlockFace.SOUTH).getType() == Material.WALL_SIGN) {
			Sign s = (Sign) e.getBlock().getRelative(BlockFace.SOUTH).getState();
			
			if(s.getLine(0).equals(signHeader) && p.hasPermission("tollgate.remove")) {
				String ID = "" + s.getX() + s.getY() + s.getZ();
				if(conf.getCustomConfig().contains("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID)) {
					conf.getCustomConfig().set("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID, null);
					conf.saveCustomConfig();
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
				}
				return;
			}
			if(s.getLine(0).equals(signHeader) && !s.getLine(1).equals(p.getName())) {
				e.setCancelled(true);
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', doNotOwnTollgate));
				
			} else {
				if(s.getLine(0).equals(signHeader) && s.getLine(1).equals(p.getName())) {
					String ID = "" + s.getX() + s.getY() + s.getZ();
					if(conf.getCustomConfig().contains("Tollgates." + p.getUniqueId() + "." + ID)) {
						conf.getCustomConfig().set("Tollgates." + p.getUniqueId() + "." + ID, null);
						conf.saveCustomConfig();
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
					}
				}
			}
		}

		if(e.getBlock().getRelative(BlockFace.WEST).getState() instanceof Sign && e.getBlock().getRelative(BlockFace.WEST).getType() == Material.WALL_SIGN) {
			Sign s = (Sign) e.getBlock().getRelative(BlockFace.WEST).getState();
			
			if(s.getLine(0).equals(signHeader) && p.hasPermission("tollgate.remove")) {
				String ID = "" + s.getX() + s.getY() + s.getZ();
				if(conf.getCustomConfig().contains("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID)) {
					conf.getCustomConfig().set("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID, null);
					conf.saveCustomConfig();
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
				}
				return;
			}
			
			if(s.getLine(0).equals(signHeader) && !s.getLine(1).equals(p.getName())) {
				e.setCancelled(true);
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', doNotOwnTollgate));
			} else {
				if(s.getLine(0).equals(signHeader) && s.getLine(1).equals(p.getName())) {
					String ID = "" + s.getX() + s.getY() + s.getZ();
					if(conf.getCustomConfig().contains("Tollgates." + p.getUniqueId() + "." + ID)) {
						conf.getCustomConfig().set("Tollgates." + p.getUniqueId() + "." + ID, null);
						conf.saveCustomConfig();
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
					}
				}
			}
		}

		if(e.getBlock().getRelative(BlockFace.UP).getState() instanceof Sign && e.getBlock().getRelative(BlockFace.UP).getType() == Material.SIGN) {
			Sign s = (Sign) e.getBlock().getRelative(BlockFace.UP).getState();
			
			if(s.getLine(0).equals(signHeader) && p.hasPermission("tollgate.remove")) {
				String ID = "" + s.getX() + s.getY() + s.getZ();
				if(conf.getCustomConfig().contains("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID)) {
					conf.getCustomConfig().set("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID, null);
					conf.saveCustomConfig();
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
				}
				return;
			}
			
			if(s.getLine(0).equals(signHeader) && !s.getLine(1).equals(p.getName())) {
				e.setCancelled(true);
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', doNotOwnTollgate));
			} else {
				if(s.getLine(0).equals(signHeader) && s.getLine(1).equals(p.getName())) {
					String ID = "" + s.getX() + s.getY() + s.getZ();
					if(conf.getCustomConfig().contains("Tollgates." + p.getUniqueId() + "." + ID)) {
						conf.getCustomConfig().set("Tollgates." + p.getUniqueId() + "." + ID, null);
						conf.saveCustomConfig();
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
					}
				}
			}
		}



		if(e.getBlock().getType().equals(Material.WALL_SIGN) || e.getBlock().getType().equals(Material.SIGN)) {
			Sign s = (Sign) e.getBlock().getState();
			if(s.getLine(0).equalsIgnoreCase(signHeader)) {

				String ID = "" + s.getX() + s.getY() + s.getZ();
				if(s.getLine(0).equals(signHeader) && p.hasPermission("tollgate.remove")) {
					if(conf.getCustomConfig().contains("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID)) {
						conf.getCustomConfig().set("Tollgates." + Bukkit.getOfflinePlayer(s.getLine(1)).getUniqueId() + "." + ID, null);
						conf.saveCustomConfig();
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
					}
					return;
				}
				
				if(!s.getLine(1).toString().equalsIgnoreCase(p.getName())) {
					e.setCancelled(true);
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', doNotOwnTollgate));
				} else {
					if(conf.getCustomConfig().getConfigurationSection("Tollgates." + p.getUniqueId()) == null) return;

					if(conf.getCustomConfig().contains("Tollgates." + p.getUniqueId() + "." + ID)) {
						conf.getCustomConfig().set("Tollgates." + p.getUniqueId() + "." + ID, null);
						conf.saveCustomConfig();
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', removedTollgate));
					}
				}
			}
		}
	}



	public void teleport(Player p, Sign sign) {
		float dir = (float) Math.toDegrees(Math.atan2(p.getLocation().getBlockX() - sign.getX(), sign.getZ() - p.getLocation().getBlockZ()));
		BlockFace face = getClosestFace(dir);
		if(getClosestFace(dir) == null) {
			return;
		}
		if(face == BlockFace.NORTH && face == BlockFace.valueOf(getDirection(p))) {
			int count = 0;
			for(int z = sign.getZ(); isSolid(new Location(p.getWorld(), sign.getX(),sign.getY(),z)); z--){
				count++;
				if(count == 20) {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', unableToTeleportError));
					break;
				}
				Location check = new Location(p.getWorld(), sign.getX(), sign.getY(), sign.getZ(), p.getLocation().getYaw(), p.getLocation().getPitch());
				check.setZ((sign.getZ() - count) + 0.5);
				check.setX(sign.getX() + 0.5);
				if(!isSolid(check)){
					p.teleport(check);
					p.playSound(p.getEyeLocation(), Sound.ENTITY_SHULKER_TELEPORT, 1, 1);
					if(p.getName().equals(sign.getLine(1))) break;
					takeMoney(p, getPrice(sign.getLine(2)), sign);
					break;
				}
			}
		}


		if(face == BlockFace.SOUTH && face == BlockFace.valueOf(getDirection(p))) {
			int count = 0;
			for(int z = sign.getZ(); isSolid(new Location(p.getWorld(), sign.getX(),sign.getY(),z)); z++){
				count++;
				if(count == 20) {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', unableToTeleportError));
					break;
				}
				Location check = new Location(p.getWorld(), sign.getX(), sign.getY(), sign.getZ(), p.getLocation().getYaw(), p.getLocation().getPitch());
				check.setZ((sign.getZ() + count) + 0.5);
				check.setX(sign.getX() + 0.5);
				if(!isSolid(check)){
					p.teleport(check);
					p.playSound(p.getEyeLocation(), Sound.ENTITY_SHULKER_TELEPORT, 1, 1);
					if(p.getName().equals(sign.getLine(1))) break;
					takeMoney(p, getPrice(sign.getLine(2)), sign);
					break;
				}
			}
		}

		if(face == BlockFace.WEST && face == BlockFace.valueOf(getDirection(p))) {
			int count = 0;
			for(int x = sign.getX(); isSolid(new Location(p.getWorld(), x,sign.getY(),sign.getZ())); x--){
				count++;
				if(count == 20) {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', unableToTeleportError));
					break;
				}
				Location check = new Location(p.getWorld(), sign.getX(), sign.getY(), sign.getZ(), p.getLocation().getYaw(), p.getLocation().getPitch());
				check.setZ((sign.getZ() + 0.5));
				check.setX((sign.getX() - count) + 0.5);
				if(!isSolid(check)){
					p.teleport(check);
					p.playSound(p.getEyeLocation(), Sound.ENTITY_SHULKER_TELEPORT, 1, 1);
					if(p.getName().equals(sign.getLine(1))) break;
					takeMoney(p, getPrice(sign.getLine(2)), sign);
					break;
				}
			}
		}


		if(face == BlockFace.EAST && face == BlockFace.valueOf(getDirection(p))) {
			int count = 0;
			for(int x = sign.getX(); isSolid(new Location(p.getWorld(), x,sign.getY(),sign.getZ())); x++){
				count++;
				if(count == 20) {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', unableToTeleportError));
					break;
				}
				Location check = new Location(p.getWorld(), sign.getX(), sign.getY(), sign.getZ(), p.getLocation().getYaw(), p.getLocation().getPitch());
				check.setZ((sign.getZ() + 0.5));
				check.setX((sign.getX() + (count + 0.5)));
				if(!isSolid(check)){
					p.teleport(check);
					p.playSound(p.getEyeLocation(), Sound.ENTITY_SHULKER_TELEPORT, 1, 1);
					if(p.getName().equals(sign.getLine(1))) break;
					takeMoney(p, getPrice(sign.getLine(2)), sign);
					break;
				}
			}
		}
	}

	public void takeMoney(Player p, double cost, Sign sign) {
		plugin.economy.withdrawPlayer(p, cost);
		plugin.economy.depositPlayer(sign.getLine(1), cost);

		if(p.getName().equals(sign.getLine(1))) return;
		String msg = usedOtherTollgate.replace("%player%", sign.getLine(1)).replace("%cost%", sign.getLine(2));
		p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
	}

	public boolean isSolid(Location l) {
		if(l.getBlock().isEmpty()) {
			return false;
		}
		return true;
	}

	public boolean isSafeLocation(Location location) {
		Block feet = location.getBlock();
		if (feet.getType().isSolid() && feet.getLocation().add(0, 1, 0).getBlock().getType().isSolid()) {
			return false;
		}
		Block head = feet.getRelative(BlockFace.UP);
		if (head.getType().isSolid()) {
			return false; 
		}
		Block ground = feet.getRelative(BlockFace.DOWN);
		if (!ground.getType().isSolid()) {
			return false; 
		}
		return true;
	}

	public String direction(Player p) {
		int yaw = (int) p.getLocation().getYaw();

		if(yaw >= 45 && yaw < 135) {
			return "SOUTH"; 
		} else if(yaw >= 135 && yaw < 180) {
			return "WEST";
		} else if (yaw >= 180 && yaw < 225) {
			return "NORTH";
		} else if(yaw >= 180 && yaw < 225) {
			return "EAST";
		}

		return null;

	}

	public char getSymboll() {
		String s = plugin.getInstance().getConfig().getString("currencySymbol");
		char[] s1 = s.toCharArray();
		return s1[0];
	}

	public char getSymbol(){  
		String s = plugin.getInstance().getConfig().getString("currencySymbol");  
		char c = s.charAt(0);  
		return c;  
	}


	public boolean isNumeric(String str) {  
		try  
		{  
			double d = Double.parseDouble(str.replace(getSymbol(), ' '));  
		}  
		catch(NumberFormatException nfe)  
		{  
			return false;  
		}  
		return true;  
	}

	public String setCurrency(String str) {  
		double d = Double.parseDouble(str.replace(getSymbol(), ' '));
		DecimalFormat nf = (DecimalFormat) DecimalFormat.getCurrencyInstance();
		DecimalFormatSymbols symbols = nf.getDecimalFormatSymbols();
		symbols.setCurrencySymbol("");
		nf.setDecimalFormatSymbols(symbols);
		String output = getSymbol() + nf.format(d);
		return output;  

	}

	public double getPrice(String str) {

		String s = str.replace(getSymbol(), ' ').replace(",", "");

		double d = Double.parseDouble(s);
		DecimalFormat nf = (DecimalFormat) DecimalFormat.getCurrencyInstance();
		DecimalFormatSymbols symbols = nf.getDecimalFormatSymbols();
		symbols.setCurrencySymbol("");
		nf.setDecimalFormatSymbols(symbols);

		return Double.parseDouble(nf.format(d).replace(",", ""));
	}

	public BlockFace getClosestFace(float direction) {

		direction = direction % 360;

		if (direction < 0)
			direction += 360;

		direction = Math.round(direction / 45);

		switch ((int) direction) {

		case 0:
			return BlockFace.SOUTH; 
		case 2:
			return BlockFace.WEST; 
		case 4:
			return BlockFace.NORTH; 
		case 6:
			return BlockFace.EAST; 
		default:
			return null; 

		}
	}

	public String getDirection(Player player) {
		float yaw = player.getLocation().getYaw();
		if (yaw < 0) {
			yaw += 360;
		}
		if (yaw >= 315 || yaw < 45) {
			return "SOUTH";
		} else if (yaw < 135) {
			return "WEST";
		} else if (yaw < 225) {
			return "NORTH";
		} else if (yaw < 315) {
			return "EAST";
		}
		return null;
	}

}
