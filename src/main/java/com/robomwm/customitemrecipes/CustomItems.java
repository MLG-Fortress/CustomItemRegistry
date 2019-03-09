package com.robomwm.customitemrecipes;

import com.robomwm.usefulutil.UsefulUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
        UsefulUtil.saveStringToFile(customItemRecipes, itemsFile, itemsYaml.saveToString());
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
            try
            {
                customItemRecipes.registerItem((ItemStack)itemsYaml.get(itemString), itemString);
            }
            catch (Throwable rock)
            {
                customItemRecipes.getLogger().warning("Failed to load item " + itemString);
                customItemRecipes.getLogger().warning("Perhaps it's a 1.12 or earlier item? Consider manually fixing or removing the item from the config.");
                rock.printStackTrace();
            }
        }
    }

    public void removeItem(String name)
    {
        itemsYaml.set(name, null);
        save();
    }

    //This turned into a mess fast :/
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
        if (!(sender instanceof Player))
            return false;
        Player player = (Player)sender;

        if (args.length < 2)
        {
            switch(args[0].toLowerCase())
            {
                case "list":
                    for (String cid : customItemRecipes.getItemNames())
                        sender.sendMessage(cid);
                    break;
                case "lore":
                case "loreizer":
                    loreizerPrompt(player);
                    break;
                case "name":
                    sender.sendMessage("/" + cmd.getLabel() + "name <name...> - Sets display name of item. Color codes accepted.");
                    break;
                case "register":
                    sender.sendMessage("/" + cmd.getLabel() + "register <name> - Registers the held item with the given name as its \"ID,\" storing it and allowing recipes to be created for it and plugins to recognize it.");
                    break;
                case "get":
                    sender.sendMessage("/" + cmd.getLabel() + "<get> <customitem name> - Adds the custom item to your inventory.");
                    break;
                default:
                    return false;
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("get"))
        {
            ItemStack itemStack = customItemRecipes.getItem(args[1]);
            if (itemStack != null)
            {
                player.sendMessage("Attempted to give you the requested item");
                player.getInventory().addItem(itemStack);
            }
            else
                player.sendMessage("That item name has not been registered. See /citem list");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR)
            return false;

        ItemMeta itemMeta = item.getItemMeta();
        if (customItemRecipes.extractCustomID(itemMeta) != null)
        {
            List<String> lore = itemMeta.getLore();
            lore.remove(itemMeta.getLore().size() - 1);
            itemMeta.setLore(lore);
            item.setItemMeta(itemMeta);
        }

        int line;

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
                    case "clear":
                        itemMeta.setLore(null);
                        sender.sendMessage("Lore cleared");
                        break;
                    case "remove":
                    case "delete":
                        line = Integer.parseInt(args[2]);
                        lore.remove(line);
                        itemMeta.setLore(lore);
                        sender.sendMessage("Deleted " + line);
                        break;
                    case "set":
                        try
                        {
                            line = Integer.parseInt(args[2]);
                            lore.remove(line);
                            args[0] = null;
                            args[1] = null;
                            args[2] = null;
                            lore.add(line, ChatColor.translateAlternateColorCodes('&', StringUtils.join(args, " ").substring(3)));
                            sender.sendMessage("Set " + line);
                            itemMeta.setLore(lore);
                            break;
                        }
                        catch (Throwable ignored){}
                    case "add":
                        args[0] = null;
                        args[1] = null;
                        lore.add(ChatColor.translateAlternateColorCodes('&', StringUtils.join(args, " ").substring(2)));
                        itemMeta.setLore(lore);
                        sender.sendMessage("Added lore");
                        break;
                    case "insert":
                        try
                        {
                            line = Integer.parseInt(args[2]);
                            args[0] = null;
                            args[1] = null;
                            args[2] = null;
                            lore.add(line, ChatColor.translateAlternateColorCodes('&', StringUtils.join(args, " ").substring(3)));
                            sender.sendMessage("Inserted " + line);
                            itemMeta.setLore(lore);
                        }
                        catch (Throwable ignored){}
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
                    sender.sendMessage("Already registered, use /cremove or /citem reregister.");
                    sender.sendMessage("Note: /citem reregister does not remove old recipes.");
                    return false;
                }
                itemsYaml.set(args[1], item.clone());
                sender.sendMessage("Registered " + args[1]);
                sender.sendMessage("Use /citem get " + args[1] + " to obtain the registered item.");
                save();
                return true;
            case "reregister":
                customItemRecipes.registerItem(item, args[1], true);
                itemsYaml.set(args[1], item.clone());
                sender.sendMessage("Registered " + args[1] + ". Note that existing recipes for the previously-registered item will still exist until next server restart.");
                sender.sendMessage("Use /citem get " + args[1] + " to obtain the registered item.");
                save();
                return true;
            default:
                return false;
        }

        item.setItemMeta(itemMeta);
        player.getInventory().setItemInMainHand(item);
        loreizerPrompt(player);
        return true;
    }

    private void loreizerPrompt(Player player)
    {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR)
        {
            player.sendMessage("You must be holding an item to use the loreizer. " +
                    "The loreizer allows you to \"customize\" an item via editing its lore.");
            return;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (customItemRecipes.extractCustomID(itemMeta) != null)
        {
            List<String> lore = itemMeta.getLore();
            lore.remove(itemMeta.getLore().size() - 1);
            itemMeta.setLore(lore);
            item.setItemMeta(itemMeta);
        }
        try
        {
            player.sendMessage("Display name:");
            if (!itemMeta.hasDisplayName())
                player.spigot().sendMessage(LazyUtil.suggest("[Set display name]", "/citem name ", "Set display name"));
            else
                player.spigot().sendMessage(LazyUtil.suggest(ChatColor.RESET + itemMeta.getDisplayName(), "/citem name " + itemMeta.getDisplayName().replaceAll("\u00A7", "&"), "Change display name"));
            if (itemMeta.hasLore())
            {
                player.sendMessage("Lore:");
                for (int i = 0; i < itemMeta.getLore().size(); i++)
                {
                    player.spigot().sendMessage(LazyUtil.suggest("[+]", "/citem lore insert " + i + " ", "Insert line above"),
                            LazyUtil.command("[-] ", "/citem lore remove " + i, "Remove line"),
                            LazyUtil.suggest(ChatColor.DARK_PURPLE.toString() + ChatColor.ITALIC + itemMeta.getLore().get(i), "/citem lore set " + i + " " + itemMeta.getLore().get(i).replaceAll("\u00A7", "&"), "Modify line"));
                }
            }
            player.spigot().sendMessage(LazyUtil.suggest("[+] Append lore", "/citem lore add ", "Append lore"));
        }
        catch (Throwable rock)
        {
            player.sendMessage("Your server does not support usage of the loreizer prompts. " +
                    "\nIf you dare, you can try manually using commands to add lore. " +
                    "/citem name, /citem lore add, /citem remove/set/insert [index]" +
                    "\nUpgrade to Paper at https://papermc.io and you'll get a pretty clickable \"UI\" to use instead");
        }
    }
}
