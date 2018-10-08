package com.robomwm.customitemrecipes;

import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created on 2/27/2018.
 *
 * Blocks crafting table recipes only.
 * Primarily used to "remove" recipes while players are in-game since it's not safe to do so otherwise.
 * Queues blocked recipes for removal when all players have left the server.
 *
 * @author RoboMWM
 */
public class RecipeBlocker implements Listener
{
    private JavaPlugin plugin;

    //The other, faster way is to compare the result - but this will fail if the user removes and re-adds the exact same item.
    //But all crafting table recipes are keyed so we can just use that instead of iterating instead. Yay?
    private Set<NamespacedKey> resultsToBlock = new HashSet<>();

    RecipeBlocker(CustomItemRecipes plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    private void onCraft(CraftItemEvent event)
    {
        if (!resultsToBlock.contains(((Keyed)event.getRecipe()).getKey()))
            return;
        event.setCancelled(true);
        event.getWhoClicked().sendMessage(ChatColor.RED + "Sorry, this recipe has been disabled and is pending removal.");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onBeforeCraft(PrepareItemCraftEvent event)
    {
        if (event.getRecipe() == null)
            return;
        if (resultsToBlock.contains(((Keyed)event.getRecipe()).getKey()))
            event.getInventory().setResult(null);
    }

    /**
     * Disables crafting of the specified recipes, <s>and queues them for removal</s>
     * @param recipes The recipes to block
     */
    public void addRecipes(Collection<Recipe> recipes)
    {
        for (Recipe recipe : recipes)
            resultsToBlock.add(((Keyed)recipe).getKey());
        //removeRecipesFromServer();
    }

    //Remove blocked recipes when the server is empty
//    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
//    private void onLastPlayerQuit(PlayerQuitEvent event)
//    {
//        if (resultsToBlock.isEmpty())
//            return;
//        new BukkitRunnable()
//        {
//            @Override
//            public void run()
//            {
//                removeRecipesFromServer();
//            }
//        }.runTaskLater(plugin, 1L);
//    }

//    private void removeRecipesFromServer()
//    {
//        if (plugin.getServer().getOnlinePlayers().size() == 0)
//        {
//            List<Recipe> recipesToKeep = new LinkedList<>();
//            Iterator<Recipe> recipeIterator = plugin.getServer().recipeIterator();
//            while (recipeIterator.hasNext())
//            {
//                Recipe recipe = recipeIterator.next();
//
//                if (!(recipe instanceof Keyed)) //Keep all non-keyed (non-crafting table) recipes
//                {
//                    recipesToKeep.add(recipe);
//                    continue;
//                }
//                if (!resultsToBlock.contains(((Keyed)recipe).getKey()))
//                    recipesToKeep.add(recipe);
//            }
//            removeAllRecipesExceptFor(recipesToKeep);
//            resultsToBlock.clear();
//        }
//    }

    /*
     * Removes all recipes, then re-adds the recipes specified in the Collection.
     *
     * Will fail and return false if players are present on the server
     *
     * Attempting to call Server#clearRecipes while players are online will cause the server to have issues saving data for these players.
     * Calling Server#resetRecipes will cause the issue above, but only for players who have crafted custom recipes.
     *
     * Thus, it is basically impossible to remove recipes safely while players are online,
     * unless we track crafted recipes - which an addon can take care of.
     *
     * @param recipesToKeep Collection of recipes to add after removing all recipes
     * @return
     */
//    private boolean removeAllRecipesExceptFor(Collection<Recipe> recipesToKeep)
//    {
//        if (plugin.getServer().getOnlinePlayers().size() > 0)
//            return false;
//        else
//            plugin.getServer().clearRecipes();
//
//        if (recipesToKeep == null)
//            return true;
//
//        /*Server#resetRecipes apparently "reinitializes" all vanilla recipes
//        So none of the recipes will Object#equals the "old" ones.
//        Thus, we use this nice try-catch to ignore the "duplicate recipe" exception
//        (And no, not all recipes implement NamespacedKey)
//         */
//        for (Recipe recipe : recipesToKeep)
//        {
//            try
//            {
//                plugin.getServer().addRecipe(recipe);
//            }
//            catch (IllegalStateException ignored){} //vanilla recipe
//        }
//        return true;
//    }
}
