package net.aufdemrand.denizen.scripts.commands.core;

import net.aufdemrand.denizen.exceptions.CommandExecutionException;
import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.scripts.commands.AbstractCommand;
import net.aufdemrand.denizen.utilities.arguments.Item;
import net.aufdemrand.denizen.utilities.arguments.aH;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizen.utilities.depends.Depends;
import net.aufdemrand.denizen.utilities.nbt.NBTItem;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/* GIVE [MONEY|#(:#)|MATERIAL_TYPE(:#)] (QTY:#) */

/* 
 * Arguments: [] - Required, () - Optional 
 * [MONEY|[#](:#)|[MATERIAL_TYPE](:#)] specifies what to give.
 *   [MONEY] gives money using your economy.
 *   [#](:#) gives the item with the specified item ID. Optional
 *     argument (:#) can specify a specific data value.
 *   [MATERIAL_TYPE](:#) gives the item with the specified
 *     bukkit MaterialType. Optional argument (:#) can specify
 *     a specific data value.
 * (QTY:#) specifies quantity. If not specified, assumed 'QTY:1'    
 *  
 */

public class GiveCommand  extends AbstractCommand {

    enum GiveType { ITEM, MONEY, EXP }

    @Override
    public void parseArgs(ScriptEntry scriptEntry)
            throws InvalidArgumentsException {

        GiveType type = null;
        int amt = 1;
        Item item = null;
        boolean engrave = false;

		/* Match arguments to expected variables */
        for (String thisArg : scriptEntry.getArguments()) {
            if (aH.matchesQuantity(thisArg))
                amt = aH.getIntegerFrom(thisArg);

            else if (aH.matchesArg("MONEY", thisArg))
                type = GiveType.MONEY;

            else if (aH.matchesArg("XP", thisArg)
                    || aH.matchesArg("EXP", thisArg))
                type = GiveType.EXP;

            else if (aH.matchesArg("ENGRAVE", thisArg))
                engrave = true;

            else if (aH.matchesItem(thisArg) || aH.matchesItem("item:" + thisArg)) {
                item = aH.getItemFrom (thisArg);
                type = GiveType.ITEM;
            }

            else throw new InvalidArgumentsException(dB.Messages.ERROR_UNKNOWN_ARGUMENT, thisArg);
        }

        if (type == null)
            throw new InvalidArgumentsException("Must specify a type! Valid: MONEY, XP, or ITEM:...");

        if (type == GiveType.ITEM && item == null)
            throw new InvalidArgumentsException("Item was returned as null.");

        scriptEntry.addObject("type", type)
                .addObject("amt", amt)
                .addObject("item", item)
                .addObject("engrave", engrave);
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        GiveType type = (GiveType) scriptEntry.getObject("type");
        Integer amt = (Integer) scriptEntry.getObject("amt");
        Item item = (Item) scriptEntry.getObject("item");
        Boolean engrave = (Boolean) scriptEntry.getObject("engrave");

        dB.report(getName(),
                aH.debugObj("Type", type.name())
                        + aH.debugObj("Amount", amt.toString())
                        + (item != null ? item.debug() : "")
                        + (engrave ? aH.debugObj("Engraved", "TRUE") : ""));

        switch (type) {

            case MONEY:
                if(Depends.economy != null)
                    Depends.economy.depositPlayer(scriptEntry.getPlayer().getName(), (double) amt);
                else dB.echoError("No economy loaded! Have you installed Vault and a compatible economy plugin?");
                break;

            case EXP:
                scriptEntry.getPlayer().giveExp(amt);
                break;

            case ITEM:
                ItemStack is = item.getItemStack();
                is.setAmount(amt);
                if(engrave) is = NBTItem.addCustomNBT(item.getItemStack(), "owner", scriptEntry.getPlayer().getName());

                HashMap<Integer, ItemStack> leftovers = scriptEntry.getPlayer().getInventory().addItem(is);

                if (!leftovers.isEmpty()) {
                    dB.echoDebug ("'" + scriptEntry.getPlayer().getName() + "' did not have enough space in their inventory," +
                            " the rest of the items have been placed on the floor.");
                    for (Map.Entry<Integer, ItemStack> leftoverItem : leftovers.entrySet())
                        scriptEntry.getPlayer().getWorld().dropItem(scriptEntry.getPlayer().getLocation(), leftoverItem.getValue());
                }
                break;
        }
    }
}