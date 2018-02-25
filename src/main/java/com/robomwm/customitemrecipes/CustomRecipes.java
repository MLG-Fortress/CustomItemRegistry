package com.robomwm.customitemrecipes;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 2/24/2018.
 *
 * @author RoboMWM
 */
class CustomRecipes implements CommandExecutor, Listener
{
    private CustomItemRecipes customItemRecipes;
    private YamlConfiguration recipesYaml;
    private File recipesFile;

    public void save()
    {
        try
        {
            recipesYaml.save(recipesFile);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    CustomRecipes(CustomItemRecipes customItemRecipes)
    {
        this.customItemRecipes = customItemRecipes;
        customItemRecipes.getServer().getPluginManager().registerEvents(this, customItemRecipes);
        recipesFile = new File(customItemRecipes.getDataFolder(), "recipes.yml");
        if (!recipesFile.exists())
        {
            try
            {
                recipesFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return;
            }
        }
        recipesYaml = YamlConfiguration.loadConfiguration(recipesFile);

        if (recipesYaml.getConfigurationSection("shaped") == null)
            recipesYaml.createSection("shaped");
        if (recipesYaml.getConfigurationSection("shapeless") == null)
            recipesYaml.createSection("shapeless");

        for (String itemString : recipesYaml.getConfigurationSection("shaped").getKeys(false))
        {
            ShapedRecipe shapedRecipe = customItemRecipes.getShapedRecipe(customItemRecipes, itemString);
            if (shapedRecipe == null)
            {
                customItemRecipes.getLogger().warning(itemString + " is not a custom item, skipping...");
                continue;
            }
            ConfigurationSection section = recipesYaml.getConfigurationSection("shaped").getConfigurationSection(itemString);
            shapedRecipe.shape(section.getString("shape").split(":"));
            for (String key : section.getKeys(false))
            {
                if (key.equalsIgnoreCase("shape"))
                    return;
                char keyChar = key.toCharArray()[0];
                try
                {
                    shapedRecipe.setIngredient(keyChar, Material.getMaterial(section.getString(key)));
                }
                catch (Throwable rock)
                {
                    customItemRecipes.getLogger().severe("invalid ingredient specified for " + itemString);
                    rock.printStackTrace();
                    return; //go PR error handling here if u wanna
                }
            }
            customItemRecipes.getServer().addRecipe(shapedRecipe);
        }

        for (String itemString : recipesYaml.getConfigurationSection("shapeless").getKeys(false))
        {
            ShapelessRecipe shapelessRecipe = customItemRecipes.getShapelessRecipe(customItemRecipes, itemString);
            if (shapelessRecipe == null)
            {
                customItemRecipes.getLogger().warning(itemString + " is not a custom item, skipping...");
                continue;
            }
            ConfigurationSection section = recipesYaml.getConfigurationSection("shapeless").getConfigurationSection(itemString);
            for (String key : section.getKeys(false))
            {
                try
                {
                    shapelessRecipe.addIngredient(section.getInt(key), Material.getMaterial(key));
                }
                catch (Throwable rock)
                {
                    customItemRecipes.getLogger().severe("invalid ingredient specified for " + itemString);
                    rock.printStackTrace();
                    return; //go PR error handling here if u wanna
                }
            }
            customItemRecipes.getServer().addRecipe(shapelessRecipe);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (args.length < 2)
            return false;
        if (!(sender instanceof Player))
            return false;
        Player player = (Player)sender;
        ItemStack item = customItemRecipes.getItem(ChatColor.translateAlternateColorCodes('&', args[0]));
        if (item == null)
            return false;

        switch (args[1].toLowerCase())
        {
            case "shapeless":
                recipeMaker.put(player, new RecipeCreateMode(ShapeMode.SHAPELESS, item, args[0]));
                player.openInventory(customItemRecipes.getServer().createInventory(new CIRHolder(), InventoryType.DISPENSER, "Input shapeless recipe."));
                break;
            case "shaped":
                recipeMaker.put(player, new RecipeCreateMode(ShapeMode.SHAPED, item, args[0]));
                player.openInventory(customItemRecipes.getServer().createInventory(new CIRHolder(), InventoryType.DISPENSER, "Input shaped recipe."));
                break;
            default:
                return false;
        }
        return true;
    }

    private Map<Player, RecipeCreateMode> recipeMaker = new HashMap<>();

    @EventHandler
    private void onClose(InventoryCloseEvent event)
    {
        if (event.getPlayer().getType() != EntityType.PLAYER)
            return;
        Player player = (Player)event.getPlayer();
        RecipeCreateMode createMode = recipeMaker.remove(player);

        if (createMode == null || !(event.getInventory().getHolder() instanceof CIRHolder))
            return;

        Inventory inventory = event.getInventory();

        switch(createMode.getShapeMode())
        {
            case SHAPED:
                ShapedRecipe shapedRecipe = customItemRecipes.getShapedRecipe(customItemRecipes, createMode.getName());

                //Generate ingredients character map.
                Map<Material, Character> ingredients = new HashMap<>();
                ingredients.put(null, 'a');
                ingredients.put(Material.AIR, 'a');
                char i = 'b';
                for (ItemStack item : inventory.getContents())
                {
                    if (item == null || ingredients.containsKey(item.getType()))
                        continue;
                    ingredients.put(item.getType(), i++);
                }

                //Generate shape
                String[] shapedMatrix = getShapedMatrix(ingredients, inventory.getContents()).toArray(new String[0]);
                if (shapedMatrix.length == 0)
                    return; //empty
                shapedRecipe.shape(shapedMatrix);

                //Set ingredients
                for (Material material : ingredients.keySet())
                {
                    if (material == null)
                        continue;
                    shapedRecipe.setIngredient(ingredients.get(material), material);
                }

                //Add and save to file
                if (!customItemRecipes.getServer().addRecipe(shapedRecipe))
                {
                    player.sendMessage("Couldn't add recipe for some reason...");
                    return;
                }
                ConfigurationSection shapedSection = recipesYaml.getConfigurationSection("shaped").createSection(createMode.getName(), shapedRecipe.getIngredientMap());
                shapedSection.set("shape", StringUtils.join(shapedMatrix, ":"));
                break;
            case SHAPELESS: //Way easier than shaped lol
                ShapelessRecipe shapelessRecipe = customItemRecipes.getShapelessRecipe(customItemRecipes, createMode.getName());
                Map<Material, Integer> ingredientCount = new HashMap<>();
                for (ItemStack itemStack : inventory.getContents())
                {
                    if (itemStack == null)
                        continue;
                    Integer count = ingredientCount.get(itemStack.getType());
                    if (count == null)
                        count = 0;
                    count += itemStack.getAmount();
                    ingredientCount.put(itemStack.getType(), count);
                }

                for (Material material : ingredientCount.keySet())
                {
                    shapelessRecipe.addIngredient(ingredientCount.get(material), material);
                }

                //Add and save to file
                if (!customItemRecipes.getServer().addRecipe(shapelessRecipe))
                {
                    player.sendMessage("Couldn't add recipe for some reason...");
                    return;
                }


                ConfigurationSection shapelessSection = recipesYaml.getConfigurationSection("shapeless").createSection(createMode.getName());
                for (Material material : ingredientCount.keySet())
                {
                    shapelessSection.set(material.name(), ingredientCount.get(material));
                }
                break;
        }
    }

    //Ugly. Ugh. If you know a better way, please PR or at least tell me. BUT IT WORKS WOOOOOOOOOOOO YESSSSS
    //Spent hours trying to figure out how to do this. And um not the most elegant but _could be a lot_ worse
    private List<String> getShapedMatrix(Map<Material, Character> charMap, ItemStack... matrix)
    {
        //Mark occupied slots for each column/row.
        //We're effectively getting the part of the matrix that is "used" so we can "trim" out the unused portions.
        int[] rows = new int[3];
        int[] columns = new int[3];
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                if (matrix[3*i+j] != null)
                    rows[i] = 1;
                if (matrix[3*j+i] != null)
                    columns[i] = 1;
            }
        }

        //rows/columns must be contiguous
        if (rows[0] == 1 && rows[2] == 1)
            rows[1] = 1;
        if (columns[0] == 1 && columns[2] == 1)
            columns[1] = 1;

        List<String> trimmedMatrix = new ArrayList<>();

        for (int i = 0; i < 3; i++)
        {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < 3; j++)
            {
                if (rows[i] + columns[j] > 1)
                {
                    Material material = null;
                    ItemStack item = matrix[3*i+j];
                    if (item != null)
                        material = item.getType();
                    row.append(charMap.get(material));
                }
            }
            if (row.length() > 0)
                trimmedMatrix.add(row.toString());
        }

        return trimmedMatrix;
    }
}

class CIRHolder implements InventoryHolder
{
    @Override
    public Inventory getInventory()
    {
        return null;
    }
}

enum ShapeMode
{
    SHAPED,
    SHAPELESS
}

class RecipeCreateMode
{
    private String name;
    private ShapeMode shapeMode;
    private ItemStack itemStack;

    RecipeCreateMode(ShapeMode shapeMode, ItemStack itemStack, String name)
    {
        this.shapeMode = shapeMode;
        this.itemStack = itemStack;
        this.name = name;
    }

    public ItemStack getItemStack()
    {
        return itemStack;
    }

    public ShapeMode getShapeMode()
    {
        return shapeMode;
    }

    public String getName()
    {
        return name;
    }
}