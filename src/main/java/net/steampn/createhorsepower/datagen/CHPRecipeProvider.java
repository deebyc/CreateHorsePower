package net.steampn.createhorsepower.datagen;

import com.simibubi.create.AllBlocks;
import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.steampn.createhorsepower.registry.BlockRegister;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.TrueCondition;

import static mezz.jei.api.ingredients.subtypes.UidContext.Ingredient;

public class CHPRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public CHPRecipeProvider(PackOutput packOutput){
        super(packOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegister.HORSE_CRANK.get())
                .pattern("AFA")
                .pattern("ACA")
                .pattern("SSS")
                .define('A', Blocks.AIR)
                .define('F', ItemTags.WOODEN_FENCES)
                .define('C', AllBlocks.COGWHEEL.get())
                .define('S', Blocks.STONE)
                .unlockedBy(getHasName(AllBlocks.COGWHEEL.get()), has(AllBlocks.COGWHEEL.get()))
                .save(pWriter);
    }
}
