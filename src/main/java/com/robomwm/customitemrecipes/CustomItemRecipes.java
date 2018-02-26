package com.robomwm.customitemrecipes;

import com.robomwm.customitemrecipes.event.ResetRecipeEvent;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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

    public void onEnable()
    {
        saveConfig();
        customItems = new CustomItems(this);
        customRecipes = new CustomRecipes(this);
        getCommand("citem").setExecutor(customItems);
        getCommand("cremove").setExecutor(customItems);
        new BukkitRunnable() //Allows for loading recipes of items registered via other plugins
        {
            @Override
            public void run()
            {
                getCommand("crecipe").setExecutor(customRecipes);
            }
        }.runTask(this);
    }

    public void onDisable()
    {
        customItems.save();
        customRecipes.save();
    }
    
    public boolean registerItem(ItemStack item, String name)
    {
        return registerItem(item, name, 1);
    }

    public boolean registerItem(ItemStack item, String name, int version)
    {
        if (items.containsKey(name))
            return false;
        ItemStack itemStack = item.clone();
        setItemVersion(itemStack, name, version);
        items.put(name, itemStack);
        return true;
    }

    public ItemStack getItem(String name)
    {
        if (!items.containsKey(name))
            return null;
        return items.get(name).clone();
    }

    //Does a straight "string id" comparison
    public boolean isItem(String name, ItemStack itemStack)
    {
        return getItemVersion(name, itemStack) > 0;
    }

    /**
     * Removes a registered item and its recipes
     * Since Server#clearRecipes causes issues, this will restore any removed vanilla recipes
     * @param name
     * @return
     */
    public boolean removeItem(String name)
    {
        ItemStack itemStack = items.remove(name);
        if (itemStack == null)
            return false;
        customItems.removeItem(name);
        customRecipes.removeAllRecipes(name);
        Set<Recipe> recipesToRemove = new HashSet<>();
        Iterator<Recipe> recipeIterator = getServer().recipeIterator();
        while (recipeIterator.hasNext())
        {
            Recipe recipe = recipeIterator.next();
            if (recipe.getResult().isSimilar(itemStack))
                recipesToRemove.add(recipe);
        }
        safelyRemoveRecipes(recipesToRemove);
        return true;
    }

    public int getItemVersion(String name, ItemStack itemStack)
    {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!itemMeta.hasLore())
            return 0;
        try
        {
            String[] version = revealText(itemMeta.getLore().get(itemMeta.getLore().size() - 1)).split(":");
            if (!version[0].equalsIgnoreCase(name))
                return 0;
            return Integer.valueOf(version[1]);
        }
        catch (Throwable rock)
        {
            return 0;
        }
    }

    /**
     * Checks if the item is a custom item, registered or not
     * @param itemMeta
     * @return
     */
    public boolean isCustomItem(ItemMeta itemMeta)
    {
        return extractCustomID(itemMeta) != null;
    }

    /**
     * Returns id hidden inside the lore of the item, if present.
     * @param itemMeta
     * @return name of custom item, null otherwise
     */
    public String extractCustomID(ItemMeta itemMeta)
    {
        if (!itemMeta.hasLore())
            return null;
        try
        {
            String[] version = revealText(itemMeta.getLore().get(itemMeta.getLore().size() - 1)).split(":");
            return version[0];
        }
        catch (Throwable rock)
        {
            return null;
        }
    }

    //convenience methods

    /**
     * Attempts to safely removes a recipe. Will not remove vanilla recipes unless nobody is on the server.
     * Will call ResetRecipesEvent if it calls Server#resetRecipe so plugins are aware that vanilla recipes have been restored
     * @see ResetRecipeEvent
     * @param recipesToRemove
     * @return
     */
    public void safelyRemoveRecipes(Collection<Recipe> recipesToRemove)
    {
        if (removeRecipes(recipesToRemove))
            return;
        List<Recipe> existingRecipes = new LinkedList<>();
        Iterator<Recipe> recipeIterator = getServer().recipeIterator();
        while (recipeIterator.hasNext())
        {
            Recipe recipe = recipeIterator.next();
            if (recipesToRemove.contains(recipe))
                continue;
            existingRecipes.add(recipe);
        }

        getServer().resetRecipes();
        getServer().getPluginManager().callEvent(new ResetRecipeEvent());

        /*Server#resetRecipes apparently "reinitializes" all vanilla recipes
        So none of the recipes will Object#equals the "old" ones.
        Thus, we use this nice try-catch to ignore the "duplicate recipe" exception
        (And no, not all recipes implement NamespacedKey)
         */
        for (Recipe recipe : existingRecipes)
        {
            try
            {
                getServer().addRecipe(recipe);
            }
            catch (IllegalStateException ignored){} //vanilla recipe
        }
        return;
    }

    /**
     * Removes vanilla recipes, only if no players are online
     * Attempting to call Server#clearRecipes while players are online will cause the server to have issues saving data for these players.
     * @param recipesToRemove
     * @return
     */
    public boolean removeRecipes(Collection<Recipe> recipesToRemove)
    {
        if (getServer().getOnlinePlayers().size() > 0)
            return false;
        List<Recipe> existingRecipes = new LinkedList<>();
        Iterator<Recipe> recipeIterator = getServer().recipeIterator();
        while (recipeIterator.hasNext())
        {
            Recipe recipe = recipeIterator.next();
            if (recipesToRemove.contains(recipe))
                continue;
            existingRecipes.add(recipe);
        }
        getServer().clearRecipes();
        for (Recipe recipe : existingRecipes)
            getServer().addRecipe(recipe);
        return true;
    }

    public ShapedRecipe getShapedRecipe(JavaPlugin plugin, String name)
    {
        if (!items.containsKey(name))
            return null;
        return new ShapedRecipe(new NamespacedKey(plugin, name + ":" + Integer.toString(ThreadLocalRandom.current().nextInt())), items.get(name));
    }

    public ShapelessRecipe getShapelessRecipe(JavaPlugin plugin, String name)
    {
        if (!items.containsKey(name))
            return null;
        return new ShapelessRecipe(new NamespacedKey(plugin, name + ":" + Integer.toString(ThreadLocalRandom.current().nextInt())), items.get(name));
    }

    public ItemStack setName(ItemStack itemStack, String name)
    {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(name);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack loreize(ItemStack itemStack, List<String> lore)
    {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    //internal

    private void setItemVersion(ItemStack itemStack, String name, int version)
    {
        ItemMeta itemMeta = itemStack.getItemMeta();
        int currentVersion = getItemVersion(name, itemStack);
        List<String> lore;
        if (!itemMeta.hasLore())
            lore = new ArrayList<>();
        else
            lore = itemMeta.getLore();
        if (currentVersion != 0)
            lore.remove(lore.size() - 1);
        lore.add(hideText(name + ":" + version));
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
