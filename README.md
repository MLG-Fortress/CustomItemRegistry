# CustomItemRecipes
Create your own custom items and recipes, all in-game (or via the files, if that's your fancy)! Also provides an API for plugins to recognize custom items and register their own!

[**Having problems or issues?❓ Click here for help!💡**](../../issues) | [Support me c:](https://r.robomwm.com/patreon) | [Source code](../../)

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
- Supports all rectangular sizes of shaped recipes! Recipes can 1x3, 2x2, 1x2, 2x3, etc.
- Compatible with all crafting-related plugins such as FastCraft+ since the recipe is actually registered in the server.
- Easily create recipes by placing items in the on-screen "GUI" prompt.
- Easily add name and lore to your custom items.
- API for plugins, they can
  - recognize registered custom items,
  - register their own custom items,
  - register their own recipes for custom items,
  - etc.

## API
Currently resides in the main class. Get the plugin's instance as per usual and call the public methods as you wish. Javadocs are present, will host them soon, but for now they're in the code.

[**Having problems or issues?❓ Click here for help!💡**](../../issues) | [Support me c:](https://r.robomwm.com/patreon) | [Source code](../../)
