package com.robomwm.customitemrecipes.recipebook;

import com.robomwm.customitemrecipes.LazyUtil;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.text.similarity.FuzzyScore;
import org.bukkit.Keyed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;
import pw.valaria.bookutil.BookUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class RecipeBookSearchCommand implements CommandExecutor, TabCompleter {

    private YamlConfiguration setting;
    private Plugin plugin;
    private FuzzyScore fuzzyScore = new FuzzyScore(Locale.ENGLISH);

    public RecipeBookSearchCommand(Plugin plugin, YamlConfiguration setting)
    {
        this.plugin = plugin;
        this.setting = setting;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player)sender;

        if (command.getName().equals("changerecipeview"))
            switchView(player);

        Queue<RecipeMatch> sortedRecipes = sortedSearch(String.join(" ", args));

        if (getView(player))
            return searchInventory(player, sortedRecipes);

        return searchBook(player, sortedRecipes);
    }

    public boolean getView(Player player)
    {
        return setting.getBoolean(player.getUniqueId().toString(), false);
    }

    public void switchView(Player player)
    {
        if (getView(player))
            setting.set(player.getUniqueId().toString(), null);
        else
            setting.set(player.getUniqueId().toString(), true);
    }

    public boolean searchInventory(Player player, Queue<RecipeMatch> recipes)
    {
        return false;
    }

    public boolean searchBook(Player player, Queue<RecipeMatch> recipes)
    {
        LazyUtil.Builder builder = new LazyUtil.Builder().add("Recipe search results:\n\n");

        int count = 2;

        while (!recipes.isEmpty())
        {
            String name = recipes.peek().getName();
            Recipe recipe = recipes.poll().getRecipe();
            builder.add(name).cmd("/showrecipe " + ((Keyed)recipe).getKey(), false).color(ChatColor.AQUA).add("\n");
            if (count++ > 10)
            {
                count = 0;
                builder.add("\\p");
            }
        }

        new BookUtil(plugin).openBook(player, builder.toBook());

        return true;
    }

    public Queue<RecipeMatch> sortedSearch(String search)
    {
        Iterator<Recipe> recipeIterator = plugin.getServer().recipeIterator();
        Queue<RecipeMatch> recipes = new PriorityQueue<>();

        while (recipeIterator.hasNext())
        {
            Recipe recipe = recipeIterator.next();
            if (!(recipe instanceof Keyed))
                continue;

            ItemStack result = recipe.getResult();
            String name;
            if (result.hasItemMeta() && result.getItemMeta().hasDisplayName())
                name = result.getItemMeta().getDisplayName();
            else
            {
                try
                {
                    name = result.getI18NDisplayName();
                }
                catch (Throwable rock)
                {
                    name = result.getType().name();
                }
            }

            recipes.add(new RecipeMatch(recipe, name, fuzzyScore.fuzzyScore(name, search)));
        }

        return recipes;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        String query = String.join(" ", args);
        plugin.getLogger().info("tab:'" + query + "'");
        Queue<RecipeMatch> sortedRecipes = sortedSearch(query);
        Set<String> names = new LinkedHashSet<>();
        while (!sortedRecipes.isEmpty())
            names.add(sortedRecipes.poll().getName());
        return new ArrayList<>(names);
    }
}

class RecipeMatch implements Comparable<RecipeMatch>
{
    private Recipe recipe;
    private String name;
    private int match;

    RecipeMatch(Recipe recipe, String name, int match)
    {
        this.recipe = recipe;
        this.name = name;
        this.match = match;
    }

    public Recipe getRecipe()
    {
        return recipe;
    }

    public String getName()
    {
        return name;
    }

    //Inverted so higher scores are at head of queue.
    @Override
    public int compareTo(RecipeMatch o)
    {
        return o.match - this.match;
    }
}
