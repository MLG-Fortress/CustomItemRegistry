package com.robomwm.customitemrecipes.recipebuilder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by 3/12/2019
 * @author RoboMWM
 */
public class ShapelessRecipeBuilder
{
    private Set<Tag<Material>> tagChoices = new HashSet<>();

    public ShapelessRecipeBuilder(String... tags)
    {
        for (String key : tags)
        {
            Tag<Material> tag = Bukkit.getTag("blocks", NamespacedKey.minecraft(key), Material.class);
            if (tag != null)
                tagChoices.add(tag);
        }
    }

    //Wow so easy
    public ShapelessRecipe toShapelessRecipe(ShapelessRecipe shapelessRecipe, ItemStack... ingredients)
    {
        for (ItemStack itemStack : ingredients)
        {
            if (itemStack == null)
                continue;
            shapelessRecipe.addIngredient(getChoice(itemStack.getType()));
        }

        return shapelessRecipe;
    }

    private RecipeChoice getChoice(Material material)
    {
        return new RecipeChoice.MaterialChoice(getTaggedMaterials(material));
    }

    private List<Material> getTaggedMaterials(Material material)
    {
        for (Tag<Material> tag : tagChoices)
            if (tag.isTagged(material))
                return new ArrayList<>(tag.getValues());

        return Collections.singletonList(material);
    }
}