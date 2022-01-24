package net.orandja.holycube6.recipes

import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.ShapelessRecipe
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList

class CustomRemainedShapelessRecipe(id: Identifier, group: String, output: ItemStack, input: DefaultedList<Ingredient>, private val remains: Map<Item, Item>)
    : ShapelessRecipe(id, group, output, input) {

    override fun getRemainder(inventory: CraftingInventory): DefaultedList<ItemStack> {
        val defaultedList = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY)
        for (i in defaultedList.indices) {
            val item = inventory.getStack(i).item

            if (remains.containsKey(item))
                defaultedList[i] = ItemStack(remains[item])
            else if (item.hasRecipeRemainder())
                defaultedList[i] = ItemStack(item.recipeRemainder)
        }
        return defaultedList
    }
}