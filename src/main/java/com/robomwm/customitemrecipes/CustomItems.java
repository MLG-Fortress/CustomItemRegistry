package com.robomwm.customitemrecipes;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2/24/2018.
 *
 * @author RoboMWM
 */
class CustomItems implements CommandExecutor
{
    private CustomItemRecipes customItemRecipes;
    private YamlConfiguration itemsYaml;
    private File itemsFile;

    public void save()
    {
        try
        {
            itemsYaml.save(itemsFile);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    CustomItems(CustomItemRecipes customItemRecipes)
    {
        this.customItemRecipes = customItemRecipes;
        itemsFile = new File(customItemRecipes.getDataFolder(), "items.yml");
        if (!itemsFile.exists())
        {
            try
            {
                itemsFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return;
            }
        }
        itemsYaml = YamlConfiguration.loadConfiguration(itemsFile);

        for (String itemString : itemsYaml.getKeys(false))
        {
            customItemRecipes.registerItem((ItemStack)itemsYaml.get(itemString), itemString);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (args.length == 0)
            return false;
        if (args.length < 2)
        {
            switch(args[0].toLowerCase())
            {
                case "lore":
                    sender.sendMessage("/" + cmd.getLabel() + " lore <clear/add> <lore...>");
                    break;
                case "name":
                    sender.sendMessage("/" + cmd.getLabel() + "name <name...> - Sets display name of item. Color codes accepted.");
                    break;
                case "register":
                    sender.sendMessage("/" + cmd.getLabel() + "register <name> - Adds custom item to plugin with the given name as its ID, storing it and allowing recipes to be created for it. Color codes not accepted.");
                    break;
                case "get":
                    sender.sendMessage("/" + cmd.getLabel() + "<get> <customitem name> - Adds custom item to your inventory.");
                    break;
            }
            return true;
        }
        if (!(sender instanceof Player))
            return false;
        Player player = (Player)sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!args[0].equalsIgnoreCase("get") && (item == null || item.getType() == Material.AIR))
            return false;

        ItemMeta itemMeta = item.getItemMeta();

        switch (args[0].toLowerCase())
        {
            case "lore":
                List<String> lore;
                switch(args[1].toLowerCase())
                {
                    case "remove":
                    case "clear":
                        itemMeta.setLore(null);
                        break;
                    case "add":
                        if (itemMeta.hasLore())
                            lore = itemMeta.getLore();
                        else
                            lore = new ArrayList<>();
                        args[0] = null;
                        args[1] = null;
                        lore.add(ChatColor.translateAlternateColorCodes('&', StringUtils.join(args, " ").substring(2)));
                        itemMeta.setLore(lore);
                        break;
                    default:
                        sender.sendMessage("/" + cmd.getLabel() + " lore <clear/add> <lore...>");
                }
                break;
            case "name":
                args[0] = null;
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', StringUtils.join(args, " ").substring(1)));
                break;
            case "register":
                if (!customItemRecipes.registerItem(item, args[1]))
                {
                    sender.sendMessage("Already registered");
                    return false;
                }
                itemsYaml.set(args[1], item);
                sender.sendMessage("Registered " + args[1]);
                return true;
            case "get":
                ItemStack itemStack = customItemRecipes.getItem(args[1]);
                if (itemStack != null)
                {
                    player.sendMessage("Attempted to give you item");
                    player.getInventory().addItem(itemStack);
                }
                else
                    player.sendMessage("Item id not registered.");
                return true;
        }

        item.setItemMeta(itemMeta);
        player.getInventory().setItemInMainHand(item);
        return true;
    }
}
