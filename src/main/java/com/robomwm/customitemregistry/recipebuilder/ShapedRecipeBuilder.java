package com.robomwm.customitemregistry.recipebuilder;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author RoboMWM
 * Created 3/12/2019
 *
 * Only generates recipes with material-matched ingredients
 * To support ItemStack-matched ingredients, storing and loading recipes would have to be modified
 */
public class ShapedRecipeBuilder
{
    private List<String> shape;
    private Map<Material, Character> characterMap = new HashMap<>();
    private Set<Tag<Material>> tagChoices = new HashSet<>();


    //I could safely copy and clone objects, but don't think it's worth the overhead...
    public ShapedRecipeBuilder(ItemStack... matrix)
    {
        this.characterMap = generateCharacterMap(matrix);
        this.shape = getShape(characterMap, matrix);
    }

    public ShapedRecipe toShapedRecipe(ShapedRecipe shapedRecipe)
    {
        shapedRecipe.shape(shape.toArray(new String[0]));
        for (Material material : characterMap.keySet())
        {
            RecipeChoice choice = getChoice(material);
            shapedRecipe.setIngredient(characterMap.get(material), choice);
        }
        return shapedRecipe;
    }

    private Map<Material, Character> generateCharacterMap(ItemStack... matrix)
    {
        Map<Material, Character> characterMap = new HashMap<>();
        characterMap.put(null, 'a');
        char i = 'b';
        for (ItemStack item : matrix)
        {
            if (item == null || characterMap.containsKey(item.getType()))
                continue;
            characterMap.put(item.getType(), i++);
        }
        return characterMap;
    }

    //Ugly. Ugh. If you know a better way, please PR or at least tell me. BUT IT WORKS WOOOOOOOOOOOO YESSSSS
    //Spent hours trying to figure out how to do this. And um not the most elegant but _could be a lot_ worse
    //Why was I so hard on myself this isn't that bad actually (I did add comments, I guess I was tired trying to think of a way to do what I did
    private List<String> getShape(Map<Material, Character> characterMap, ItemStack... matrix)
    {
        //Mark occupied slots for each column/row.
        //We're effectively getting the part of the matrix that is "used" so we can "trim" out the unused portions.
        boolean[] rows = new boolean[3];
        boolean[] columns = new boolean[3];
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                if (matrix[3*i+j] != null)
                    rows[i] = true;
                if (matrix[3*j+i] != null)
                    columns[i] = true;
            }
        }

        //rows/columns must be contiguous (i.e. don't trim gaps such as a blank middle row with ingredients above and below)
        if (rows[0] && rows[2])
            rows[1] = true;
        if (columns[0] && columns[2])
            columns[1] = true;

        List<String> trimmedMatrix = new ArrayList<>();

        for (int i = 0; i < 3; i++) //for each row
        {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < 3; j++) //for each column
            {
                if (rows[i] && columns[j]) //if this slot should be used (otherwise the column and/or row its in is marked for trimming)
                {
                    Material material = null;
                    ItemStack item = matrix[3*i+j]; //get single slot of a 3 row matrix (3 * currentRow + column)
                    if (item != null)
                        material = item.getType();
                    row.append(characterMap.get(material));
                }
            }
            if (row.length() > 0) //omit (trim) row if nothing's in it
                trimmedMatrix.add(row.toString());
        }

        characterMap.remove(null); //shape generated, null 'a' entry no longer needed.

        return trimmedMatrix;
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

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Recipe shape: ").append(String.join(":", shape)).append("\n");
        for (Map.Entry<Material, Character> entry : characterMap.entrySet())
            builder.append(entry.getValue()).append(" = ").append(entry.getKey().name());
        return builder.toString();
    }
}