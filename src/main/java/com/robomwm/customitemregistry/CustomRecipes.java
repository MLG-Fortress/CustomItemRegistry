package com.robomwm.customitemregistry;

import com.robomwm.usefulutil.UsefulUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created on 2/24/2018.
 *
 * @author RoboMWM
 */
class CustomRecipes implements CommandExecutor, Listener
{
    private CustomItemRegistry customItemRegistry;
    private YamlConfiguration recipesYaml;
    private File recipesFile;

    public void save()
    {
        UsefulUtil.saveStringToFile(customItemRegistry, recipesFile, recipesYaml.saveToString());
    }

    CustomRecipes(CustomItemRegistry customItemRegistry)
    {
        this.customItemRegistry = customItemRegistry;
        customItemRegistry.getServer().getPluginManager().registerEvents(this, customItemRegistry);
        recipesFile = new File(customItemRegistry.getDataFolder(), "recipes.yml");
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
        customItemRegistry.getLogger().info("Starting to load recipes");

        for (String itemString : recipesYaml.getKeys(false)) //for each item
        {
            customItemRegistry.getLogger().info("Loading recipes for " + itemString);
            ConfigurationSection shapedSection = recipesYaml.getConfigurationSection(itemString).getConfigurationSection("shaped");
            if (shapedSection != null) //for each shapedRecipe
            {
                customItemRegistry.getLogger().info("Loading shaped recipes");
                for (String recipes : shapedSection.getKeys(false)) //for each recipe
                {
                    customItemRegistry.getLogger().info("Loading " + recipes);
                    ShapedRecipe shapedRecipe = customItemRegistry.getShapedRecipe(customItemRegistry, itemString);
                    if (shapedRecipe == null)
                    {
                        customItemRegistry.getLogger().warning(itemString +
                                " is not a registered custom item, skipping...");
                        continue;
                    }

                    ConfigurationSection section = shapedSection.getConfigurationSection(recipes); //get recipe
                    shapedRecipe.shape(section.getString("shape").split(":"));
                    boolean failedToAddIngredient = false;
                    for (String key : section.getKeys(false))
                    {
                        if (key.equalsIgnoreCase("shape"))
                            continue;
                        char keyChar = key.toCharArray()[0];
                        try
                        {
                            if (section.isString(key)) //compat with older versions
                                shapedRecipe.setIngredient(keyChar, Material.valueOf(section.getString(key)));
                            else
                                shapedRecipe.setIngredient(keyChar, new RecipeChoice.MaterialChoice(stringToMaterialsList(section.getStringList(key))));

                        }
                        catch (Throwable rock)
                        {
                            customItemRegistry.getLogger().severe("invalid ingredient/choices " + section.get(key) + " specified for " + itemString);
                            rock.printStackTrace();
                            failedToAddIngredient = true;
                            break;
                        }
                    }
                    if (failedToAddIngredient)
                        continue;

                    customItemRegistry.getLogger().info("Loaded recipe " + recipes);
                    customItemRegistry.getServer().addRecipe(shapedRecipe);
                }
            }

            ConfigurationSection shapelessSection = recipesYaml.getConfigurationSection(itemString).getConfigurationSection("shapeless");

            if (shapelessSection != null)
            {
                customItemRegistry.getLogger().info("Loading shapeless recipes");
                for (String recipes : shapelessSection.getKeys(false)) //for each shapeless recipe
                {
                    ShapelessRecipe shapelessRecipe = customItemRegistry.getShapelessRecipe(customItemRegistry, itemString);
                    if (shapelessRecipe == null)
                    {
                        customItemRegistry.getLogger().warning(itemString +
                                " is not a registered custom item, skipping...");
                        continue;
                    }

                    boolean failedToAddIngredient = false;
                    for (String key : shapelessSection.getStringList(recipes)) //get ingredients list
                    {
                        try
                        {
                            shapelessRecipe.addIngredient(new RecipeChoice.MaterialChoice(stringToMaterialsList(Arrays.asList(key.split(",")))));
                        }
                        catch (Throwable rock)
                        {
                            customItemRegistry.getLogger().severe("invalid ingredient/choices " + key + " specified for " + itemString);
                            rock.printStackTrace();
                            failedToAddIngredient = true;
                            break; //go PR error handling here if u wanna
                        }
                    }
                    if (failedToAddIngredient)
                        continue;

                    customItemRegistry.getLogger().info("Loaded recipe " + recipes);
                    customItemRegistry.getServer().addRecipe(shapelessRecipe);
                }
            }
        }
        customItemRegistry.getLogger().info("Finished loading recipes");
    }

    public void removeAllRecipes(String name)
    {
        recipesYaml.set(name, null);
        save();
    }

    public void saveShapedRecipe(String name, ShapedRecipe shapedRecipe)
    {
        ConfigurationSection itemSection = recipesYaml.getConfigurationSection(name);
        if (itemSection == null)
            itemSection = recipesYaml.createSection(name);
        ConfigurationSection shapedSection = itemSection.getConfigurationSection("shaped");
        if (shapedSection == null)
            shapedSection = itemSection.createSection("shaped");
        ConfigurationSection recipeSection = shapedSection.createSection(String.valueOf(System.currentTimeMillis()));
        recipeSection.set("shape", StringUtils.join(shapedRecipe.getShape(), ":"));
        for (Map.Entry<Character, RecipeChoice> entry : shapedRecipe.getChoiceMap().entrySet())
        {
            RecipeChoice.MaterialChoice choice = (RecipeChoice.MaterialChoice)entry.getValue();
            recipeSection.set(Character.toString(entry.getKey()), materialsToStringList(choice.getChoices()));
        }

        save();
    }

    public void saveShapelessRecipe(String name, ShapelessRecipe shapelessRecipe)
    {
        ConfigurationSection itemSection = recipesYaml.getConfigurationSection(name);
        if (itemSection == null)
            itemSection = recipesYaml.createSection(name);
        ConfigurationSection shapelessSection = itemSection.getConfigurationSection("shapeless");
        if (shapelessSection == null)
            shapelessSection = itemSection.createSection("shapeless");
        List<String> materials = new LinkedList<>();
        for (RecipeChoice choice : shapelessRecipe.getChoiceList())
        {
            RecipeChoice.MaterialChoice materialChoice = (RecipeChoice.MaterialChoice)choice;
            StringBuilder builder = new StringBuilder();

            for (Material material : materialChoice.getChoices())
                builder.append(material).append(",");

            builder.setLength(builder.length() - 1);
            materials.add(builder.toString());
        }
        shapelessSection.set(String.valueOf(System.currentTimeMillis()), materials);
        save();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (args.length < 2)
            return false;
        if (!(sender instanceof Player))
            return false;
        Player player = (Player)sender;
        ItemStack item = customItemRegistry.getItem(args[1]);
        if (item == null)
        {
            sender.sendMessage(args[1] + " is not a registered custom item name. Use /citem to register an item.");
            return false;
        }

        switch (args[0].toLowerCase())
        {
            case "shapeless":
                recipeMaker.put(player, new RecipeCreateMode(ShapeMode.SHAPELESS, item, args[1]));
                Bukkit.broadcastMessage("create recipe: " + player.toString() + " " + player.hashCode());
                player.openInventory(customItemRegistry.getServer().createInventory(new CIRHolder(), InventoryType.DISPENSER, "Input shapeless recipe"));
                break;
            case "shaped":
                recipeMaker.put(player, new RecipeCreateMode(ShapeMode.SHAPED, item, args[1]));
                Bukkit.broadcastMessage("create recipe: " + player.toString() + " " + player.hashCode());
                player.openInventory(customItemRegistry.getServer().createInventory(new CIRHolder(), InventoryType.DISPENSER, "Input shaped recipe"));
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
        if (createMode == null) // || !(event.getInventory().getHolder() instanceof CIRHolder)) //Works in Paper, but not spigot. I guess it being here isn't that necessary...
            return;

        Inventory inventory = event.getInventory();

        switch(createMode.getShapeMode())
        {
            case SHAPED:
                ShapedRecipe shapedRecipe = customItemRegistry.getShapedRecipe(customItemRegistry, createMode.getName());

                //Generate ingredients character map.
                Map<Material, Character> ingredients = new HashMap<>();
                ingredients.put(null, 'a');
                char i = 'b';
                for (ItemStack item : inventory.getContents())
                {
                    if (item == null || ingredients.containsKey(item.getType()))
                        continue;
                    ingredients.put(item.getType(), i++);
                }

                //Generate shape
                String[] shapedMatrix = getShapedMatrix(ingredients, inventory.getContents()).toArray(new String[0]);

                if (shapedMatrix.length == 0) //empty
                {
                    player.sendMessage(ChatColor.RED + "Recipe creation canceled (empty recipe)");
                    return;
                }
                shapedRecipe.shape(shapedMatrix);

                //setIngredients doesn't accept ingredients that aren't present in the shape so yea...
                boolean noAir = true;
                for (String string : shapedMatrix)
                {
                    if (string.contains("a"))
                        noAir = false;
                }
                if (noAir)
                {
                    ingredients.remove(null);
                    ingredients.remove(Material.AIR);
                }

                player.sendMessage("Recipe shape: " + String.join(":", shapedMatrix));
                //Set ingredients
                for (Material material : ingredients.keySet())
                {
                    if (material == null)
                        continue;
                    shapedRecipe.setIngredient(ingredients.get(material), material);
                    player.sendMessage(ingredients.get(material).toString() + " = " + material.toString());
                }

                //Add and save to file
                if (!customItemRegistry.getServer().addRecipe(shapedRecipe))
                {
                    player.sendMessage("Couldn't add recipe for some reason...");
                    return;
                }
                saveShapedRecipe(createMode.getName(), shapedRecipe);
                break;
            case SHAPELESS: //Way easier than shaped lol
                ShapelessRecipe shapelessRecipe = customItemRegistry.getShapelessRecipe(customItemRegistry, createMode.getName());
                for (ItemStack itemStack : inventory.getContents())
                {
                    if (itemStack == null)
                        continue;
                    shapelessRecipe.addIngredient(1, itemStack.getType());
                }

                if (shapelessRecipe.getIngredientList().isEmpty())
                {
                    player.sendMessage(ChatColor.RED + "Recipe creation canceled (empty recipe)");
                    return;
                }

                //Add and save to file
                if (!customItemRegistry.getServer().addRecipe(shapelessRecipe))
                {
                    player.sendMessage("Couldn't add recipe for some reason...");
                    return;
                }
                for (ItemStack itemStack : shapelessRecipe.getIngredientList())
                    player.sendMessage(itemStack.getType().toString());
                saveShapelessRecipe(createMode.getName(), shapelessRecipe);
                break;
        }
        player.sendMessage("Recipe added");
        List<ItemStack> itemsToReturn = new ArrayList<>();
        for (ItemStack itemStack : inventory.getContents())
            if (itemStack != null)
                itemsToReturn.add(itemStack);
        player.getInventory().addItem(itemsToReturn.toArray(new ItemStack[0]));
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

    public List<String> materialsToStringList(List<Material> materials)
    {
        List<String> names = new ArrayList<>(materials.size());
        for (Material material : materials)
            names.add(material.name());
        return names;
    }

    public List<Material> stringToMaterialsList(List<String> materialNames)
    {
        List<Material> materials = new ArrayList<>(materialNames.size());
        for (String name : materialNames)
            materials.add(Material.valueOf(name));
        return materials;
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