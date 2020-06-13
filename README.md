# CustomItemRegistry
Create your own custom items and recipes, all in-game (or via the files, if that's your fancy)! All items are registered within this plugin, providing an API for plugins to recognize custom items and register their own!

[**Having problems or issues?❓ Click here for help!💡**](../../issues) | [Support me c:](https://r.robomwm.com/patreon) | [Source code](../../) | [Project page](https://dev.bukkit.org/projects/customitemrecipes)

## Purpose

This plugin provides an API for plugins to to register, recognize, and spawn custom items created by server administrators and other plugins (that hook into this plugin).

You can create and register your own items for plugins to use like AbsorptionShields, and spawn them with the exact same name, lore, durability, etc.

There is also auxilliary recipe creation support which is registered directly into the server's recipe store, but this is limited to the crafting table and limitations of the vanilla crafting system.

## Commands
- [Permissions](https://github.com/MLG-Fortress/CustomItemRecipes/blob/master/src/main/resources/plugin.yml)
- /citem - Name, add lore, or register a new custom item.
- /crecipe - Create a shaped or shapeless recipe for a custom item.
- /cremove - Removes a custom item and all its recipes.

![](https://i.imgur.com/5jjLqPf.png)
![](https://i.imgur.com/UOXdfN6.png)

![](https://i.imgur.com/nfbhY0V.png)
![](https://i.imgur.com/hMEbsrL.png)

## Config
```yaml
# Should spawned custom item "IDs" be invisible?
useInvisibleID: true
```

## Features

- Easily add name and lore to your custom items via the clickable chat prompts (requires Paper or Spigot for clickable prompts, otherwise fallbacks to command-based editing).
- API for plugins, they can
  - recognize registered custom items,
  - register their own custom items,
  - register their own recipes for custom items,
  - etc.
- Easily create recipes by placing items in the on-screen "GUI" prompt.
- Recipe creation supports all rectangular sizes of shaped recipes! Recipes can 1x3, 2x2, 1x2, 2x3, etc.
- Compatible with all crafting-related plugins such as FastCraft+ since the recipe is actually registered in the server.

## API
Currently resides in the main class. Get the plugin's instance as per usual and call the public methods as you wish. Javadocs are present, will host them soon, but for now they're in the code.

[**Having problems or issues?❓ Click here for help!💡**](../../issues) | [Support me c:](https://r.robomwm.com/patreon) | [Source code](../../)
