package me.tango.tollgate.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import me.tango.tollgate.Main;
import me.tango.tollgate.utilities.ConfigManager;

public class ListTollgates implements CommandExecutor {

	static Main plugin;
	public ListTollgates(Main plugin) {
		this.plugin = plugin;
	}

	static ConfigManager conf = new ConfigManager(plugin);

	String noPermission = plugin.getInstance().getConfig().getString("noPermission");
	String invalidCommandArgs = plugin.getInstance().getConfig().getString("invalidCommandArgs");
	String noTollgatesFound = plugin.getInstance().getConfig().getString("noTollgatesFound");

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String string, String[] args){
		if (sender instanceof Player) {
			Player p = (Player)sender;
			if(cmd.getName().equalsIgnoreCase("tollgate")) {
				if(args.length < 0){
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidCommandArgs));
					return true;
				}
				
				if(!p.hasPermission("tollgate.list")) {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermission));
					return true;
				}
				
				if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
					conf.reloadCustomConfig();
					ConfigurationSection locations = conf.getCustomConfig().getConfigurationSection("Tollgates." + p.getUniqueId());
					if(conf.getCustomConfig().getConfigurationSection("Tollgates." + p.getUniqueId()).getKeys(false).size() == 0) {
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', noTollgatesFound));
						return true;
					} else {
						int i = 1;
						while(i <= conf.getCustomConfig().getConfigurationSection("Tollgates." + p.getUniqueId()).getKeys(false).size()) {
							for (String key : locations.getKeys(false)) {
								int x = locations.getInt(key + ".x");
								int y = locations.getInt(key + ".y");
								int z = locations.getInt(key + ".z");
								String world = locations.getString(key + ".world");
								p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + i + ChatColor.GRAY + "]" + ChatColor.AQUA 
										+ " " + x + ", " + y + ", " + z + ChatColor.GRAY + " (" + ChatColor.BLUE + world + ChatColor.GRAY + ")");
								i++;
							}
						}
						return true;
					}
				} else {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidCommandArgs));
				}
			}
		}
		return false;
	}

}
