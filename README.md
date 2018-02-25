# CustomItemRecipes
Create your own custom items and recipes, all in-game (or via the configuration files, if that's your fancy)! Also includes an API so plugins can register their own custom items!

## Commands
- [Permissions](https://github.com/MLG-Fortress/CustomItemRecipes/blob/master/src/main/resources/plugin.yml)
- /citem - Name, add lore, or register a new custom item.
- /crecipe - Create a shaped or shapeless recipe for a custom item.
- /cremove - Removes a custom item and all its recipes.

![](https://i.imgur.com/UOXdfN6.png)

![](https://i.imgur.com/nfbhY0V.png)
![](https://i.imgur.com/hMEbsrL.png)

## Features
- Supports all rectangular sizes of shaped recipes! Recipes can 1x3, 2x2, etc.
- Compatible with all crafting-related plugins such as FastCraft+ since the recipe is actually registered in the server.
- Easily create recipes by placing items in the on-screen "GUI" prompt.
- Easily add name and lore to your custom items.
- API for plugins to register their own items and/or extend this plugin's features.

## API
Currently resides in the main class. Get the plugin's instance as per usual and call the public methods as you wish.