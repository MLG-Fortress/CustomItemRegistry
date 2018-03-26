package com.robomwm.customitemrecipes;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Created on 2/24/2018.
 *
 * @author RoboMWM
 */
public class CustomItemRecipes extends JavaPlugin
{
    private Map<String, ItemStack> items = new HashMap<>();

    private CustomItems customItems;
    private CustomRecipes customRecipes;
    private RecipeBlocker recipeBlocker;
    private boolean useHiddenID;
    private String visibleIDPrefix;

    public void onEnable()
    {
        visibleIDPrefix = ChatColor.DARK_GRAY + "CID:";
        saveConfig();
        getConfig().addDefault("useHiddenID", true);
        getConfig().options().copyDefaults(true);
        saveConfig();
        useHiddenID = getConfig().getBoolean("useHiddenID");
        customItems = new CustomItems(this);
        customRecipes = new CustomRecipes(this);
        recipeBlocker = new RecipeBlocker(this);
        getCommand("citem").setExecutor(customItems);
        getCommand("cremove").setExecutor(customItems);
        getServer().getPluginManager().registerEvents(recipeBlocker, this);
        getCommand("crecipe").setExecutor(customRecipes);
    }

    public RecipeBlocker getRecipeBlocker()
    {
        return recipeBlocker;
    }

    public void onDisable()
    {
        customItems.save();
        customRecipes.save();
    }

    /**
     * Register an item with the given name as its string ID
     * @param item the ItemStack to register
     * @param name the string ID of this item
     * @return true if successfully registered, false if the specified string ID is already registered.
     */
    public boolean registerItem(ItemStack item, String name)
    {
        return registerItem(item, name, false);
    }

    /**
     * Register an item with the given name as its string ID
     * @param item the ItemStack to register
     * @param name the string ID of this item
     * @param force Whether we should overwrite an existing custom item, if present
     * @return true if successfully registered, false if the specified string ID is already registered. Always succeeds if force is true.
     */
    public boolean registerItem(@Nonnull ItemStack item, String name, boolean force)
    {
        Validate.isTrue(item.getType() != Material.AIR, "Cannot register an item with type AIR");
        if (items.containsKey(name) && !force)
            return false;
        ItemStack itemStack = item.clone();
        brandCustomItemID(itemStack, name);
        items.put(name, itemStack);
        return true;
    }

    public ItemStack getItem(String name)
    {
        if (!items.containsKey(name))
            return null;
        return items.get(name).clone();
    }

    /**
     * Removes a registered item and its recipes.
     *
     * Recipes will only be removed if no players are online.
     * Otherwise it will wait until all players are offline before removing, and block crafting of the item until then.
     * @see RecipeBlocker
     *
     * @param name String ID of custom item to remove.
     * @return Whether the item was previously registered.
     */
    public boolean removeItem(String name)
    {
        ItemStack itemStack = items.remove(name);
        if (itemStack == null)
            return false;
        customItems.removeItem(name);
        customRecipes.removeAllRecipes(name);
        List<Recipe> recipesToRemove = new LinkedList<>();
        Iterator<Recipe> recipeIterator = getServer().recipeIterator();
        while (recipeIterator.hasNext())
        {
            Recipe recipe = recipeIterator.next();
            if (recipe.getResult().isSimilar(itemStack))
                recipesToRemove.add(recipe);
        }
        recipeBlocker.addRecipes(recipesToRemove);
        return true;
    }

    /**
     * Checks if the specified ItemStack matches the specified registered custom ItemStack
     *
     * @implNote This compares types in addition to the branded ID.
     *
     * @param name The string ID of the registered item.
     * @param itemStack The ItemStack to check. Item must not be null or of type AIR.
     * @return true if custom item matches registered item name
     */
    public boolean isItem(String name, @Nonnull ItemStack itemStack)
    {
        Validate.isTrue(itemStack.getType() != Material.AIR, "Cannot check AIR (contains no ItemMetadata).");
        ItemStack customStack = items.get(name);
        if (customStack == null || customStack.getType() != itemStack.getType())
            return false;
        String extractedName = extractCustomID(itemStack.getItemMeta());
        return extractedName != null && extractedName.equals(name);
    }

    /**
     * Checks if the item is a registered, custom item
     * @param itemMeta
     * @return
     */
    public boolean isCustomItem(ItemMeta itemMeta)
    {
        String name = extractCustomID(itemMeta);
        return name != null && items.containsKey(name);
    }

    /**
     * Returns string ID hidden inside the lore of the item, if present.
     * @param itemMeta
     * @return name of custom item, null otherwise
     */
    public String extractCustomID(ItemMeta itemMeta)
    {
        if (!itemMeta.hasLore())
            return null;

        if (!useHiddenID)
        {
            String line = itemMeta.getLore().get(itemMeta.getLore().size() - 1);
            if (!line.startsWith(visibleIDPrefix))
                return null;
            return line.substring(6);
        }

        try
        {
            return revealText(itemMeta.getLore().get(itemMeta.getLore().size() - 1));
        }
        catch (Throwable rock)
        {
            return null;
        }
    }

    //convenience methods

    //Reduce "failed to load recipe" warning on join after a server restart via maintaining a consistent order to keys.
    //Not guaranteed if player removes recipes in-game, but will only occur once after a restart rather than after every restart.
    private int i = 0;

    /**
     * Get a new shapedRecipe object for the given customItem string
     * @param plugin
     * @param name
     * @return a new ShapedRecipe, or null if the custom item string id is not registered.
     */
    public ShapedRecipe getShapedRecipe(JavaPlugin plugin, String name)
    {
        if (!items.containsKey(name))
            return null;
        return new ShapedRecipe(new NamespacedKey(plugin, name + ":" + Integer.toString(i++)), items.get(name));
    }

    /**
     * Get a new shapelessRecipe object for the given customItem string
     * @param plugin
     * @param name
     * @return a new ShapelessRecipe, or null if the custom item string id is not registered.
     */
    public ShapelessRecipe getShapelessRecipe(JavaPlugin plugin, String name)
    {
        if (!items.containsKey(name))
            return null;
        return new ShapelessRecipe(new NamespacedKey(plugin, name + ":" + Integer.toString(i++)), items.get(name));
    }

    //internal

    //Version is no longer used - include version in name if you'd like to "version" your custom items.
    private void brandCustomItemID(ItemStack itemStack, String name)
    {
        ItemMeta itemMeta = itemStack.getItemMeta();

        List<String> lore;
        if (!itemMeta.hasLore())
            lore = new ArrayList<>();
        else
            lore = itemMeta.getLore();

        if (extractCustomID(itemMeta) != null)
            lore.remove(itemMeta.getLore().size() - 1);

        if (useHiddenID)
            lore.add(hideText(name));
        else
            lore.add(visibleIDPrefix + name);

        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
    }

    //hex lore hider thingy from https://www.spigotmc.org/threads/how-to-hide-item-lore-how-to-bind-data-to-itemstack.196008/#post-2043170
    //wowsohacky

    /**
     * Hides text in color codes
     *
     * @param text The text to hide
     * @return The hidden text
     */
    public static String hideText(String text) {
        Objects.requireNonNull(text, "text can not be null!");

        StringBuilder output = new StringBuilder();

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        String hex = Hex.encodeHexString(bytes);

        for (char c : hex.toCharArray()) {
            output.append(ChatColor.COLOR_CHAR).append(c);
        }

        return output.toString();
    }

    /**
     * Reveals the text hidden in color codes
     *
     * @param text The hidden text
     * @throws IllegalArgumentException if an error occurred while decoding.
     * @return The revealed text
     */
    public static String revealText(String text) {
        Objects.requireNonNull(text, "text can not be null!");

        if (text.isEmpty()) {
            return text;
        }

        char[] chars = text.toCharArray();

        try
        {
        char[] hexChars = new char[chars.length / 2];

        IntStream.range(0, chars.length)
                .filter(value -> value % 2 != 0)
                .forEach(value -> hexChars[value / 2] = chars[value]);
            return new String(Hex.decodeHex(hexChars), StandardCharsets.UTF_8);
        } catch (DecoderException e) {
            //e.printStackTrace();
            //throw new IllegalArgumentException("Couldn't decode text", e);
            return null;
        }
    }
}
