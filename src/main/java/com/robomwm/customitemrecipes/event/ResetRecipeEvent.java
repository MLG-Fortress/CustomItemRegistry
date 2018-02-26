package com.robomwm.customitemrecipes.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created on 2/26/2018.
 *
 * Really just a signal, could add stuff like e.g. what to add to server I guess but I see no reason for such.
 *
 * @author RoboMWM
 */
public class ResetRecipeEvent extends Event
{
    // Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
