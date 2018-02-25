package com.robomwm.customitemrecipes;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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
        getCommand("crecipe").setExecutor(customRecipes);
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

    public boolean removeItem(String name)
    {
        ItemStack itemStack = items.remove(name);
        if (itemStack == null)
            return false;
        List<Recipe> existingRecipes = new LinkedList<>();
        Iterator<Recipe> recipeIterator = getServer().recipeIterator();
        while (recipeIterator.hasNext())
        {
            Recipe recipe = recipeIterator.next();
            if (recipe.getResult().isSimilar(itemStack))
                continue;
            existingRecipes.add(recipe);
        }
        getServer().clearRecipes();
        for (Recipe recipe : existingRecipes)
            getServer().addRecipe(recipe);
        return true;
    }

    public void removeRecipe(Set<Material> materials)
    {
        List<Recipe> existingRecipes = new LinkedList<>();
        Iterator<Recipe> recipeIterator = getServer().recipeIterator();
        while (recipeIterator.hasNext())
        {
            Recipe recipe = recipeIterator.next();
            if (materials.contains(recipe.getResult().getType()))
                continue;
            existingRecipes.add(recipe);
        }
        getServer().clearRecipes();
        for (Recipe recipe : existingRecipes)
            getServer().addRecipe(recipe);
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

    //convenience methods

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

        char[] hexChars = new char[chars.length / 2];

        IntStream.range(0, chars.length)
                .filter(value -> value % 2 != 0)
                .forEach(value -> hexChars[value / 2] = chars[value]);

        try {
            return new String(Hex.decodeHex(hexChars), StandardCharsets.UTF_8);
        } catch (DecoderException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Couldn't decode text", e);
        }
    }
}
