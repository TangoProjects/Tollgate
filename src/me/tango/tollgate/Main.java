package me.tango.tollgate;

import java.io.File;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.tango.tollgate.commands.ListTollgates;
import me.tango.tollgate.listeners.SignClick;
import me.tango.tollgate.utilities.ConfigManager;
import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin {

	
	public static Main plugin;
	public static Main getInstance(){
		return plugin;
	}
	
	public static ConfigManager conf;
	//public SignClick sc = new SignClick(this);
	
	@Override
	public void onEnable() {
		plugin = this;
		getServer().getPluginManager().registerEvents(new SignClick(this), this);
		getCommand("tollgate").setExecutor((CommandExecutor) new ListTollgates(this));
		if (!setupEconomy()) {
			getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		conf = new ConfigManager(this);
		conf.initCustomConfig();
		conf.getCustomConfig();
		loadConfig();
	}

	@Override
	public void onDisable() {

	}

	public static Economy economy = null;

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}
		return (economy != null);
	}

	private void loadConfig() {
		try {
			if (!getDataFolder().exists()) getDataFolder().mkdirs();
			File file = new File(getDataFolder(), "config.yml");
			if (!file.exists()) saveDefaultConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
