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

    public void removeItem(String name)
    {
        itemsYaml.set(name, null);
        save();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (args.length == 0)
            return false;
        if (cmd.getName().equalsIgnoreCase("cremove"))
        {
            if (customItemRecipes.removeItem(args[0]))
                sender.sendMessage("Removed " + args[0] + " and all of its recipes.");
            else
                sender.sendMessage("Item is not registered");
            return true;
        }
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
                default:
                    return false;
            }
            return true;
        }
        if (!(sender instanceof Player))
            return false;
        Player player = (Player)sender;

        if (args[0].equalsIgnoreCase("get"))
        {
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
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR)
            return false;

        ItemMeta itemMeta = item.getItemMeta();
        if (customItemRecipes.isCustomItem(itemMeta))
            itemMeta.getLore().remove(itemMeta.getLore().size() - 1);


        switch (args[0].toLowerCase())
        {
            case "lore":
                List<String> lore;
                if (itemMeta.hasLore())
                    lore = itemMeta.getLore();
                else
                    lore = new ArrayList<>();
                switch(args[1].toLowerCase())
                {
                    case "remove":
                    case "clear":
                        itemMeta.setLore(null);
                        sender.sendMessage("Lore cleared");
                        break;
                    case "set":
                        int line;
                        try
                        {
                            line = Integer.parseInt(args[2]);
                            lore.remove(line);
                            args[0] = null;
                            args[1] = null;
                            args[2] = null;
                            lore.add(line, ChatColor.translateAlternateColorCodes('&', StringUtils.join(args, " ").substring(3)));
                            sender.sendMessage("Set " + line);
                            break;
                        }
                        catch (Throwable rock){}
                    case "add":
                        args[0] = null;
                        args[1] = null;
                        lore.add(ChatColor.translateAlternateColorCodes('&', StringUtils.join(args, " ").substring(2)));
                        itemMeta.setLore(lore);
                        sender.sendMessage("Added lore");
                        break;
                    default:
                        sender.sendMessage("/" + cmd.getLabel() + " lore <clear/add> <lore...> or /lore set <line> <lore...>");
                        return true;
                }
                break;
            case "name":
                args[0] = null;
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', StringUtils.join(args, " ").substring(1)));
                break;
            case "register":
                if (!customItemRecipes.registerItem(item, args[1]))
                {
                    sender.sendMessage("Already registered, use /reregister to force");
                    return false;
                }
                itemsYaml.set(args[1], item);
                sender.sendMessage("Registered " + args[1]);
                return true;
            case "reregister":
                customItemRecipes.registerItem(item, args[1], 1, true);
                itemsYaml.set(args[1], item);
                sender.sendMessage("Registered " + args[1]);
                return true;
            default:
                return false;
        }

        item.setItemMeta(itemMeta);
        player.getInventory().setItemInMainHand(item);
        return true;
    }
}
