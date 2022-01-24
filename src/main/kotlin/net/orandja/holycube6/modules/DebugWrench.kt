package net.orandja.holycube6.modules

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import net.minecraft.block.*
import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.block.enums.RailShape
import net.minecraft.block.enums.SlabType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.state.property.Property
import net.minecraft.tag.BlockTags
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.orandja.holycube6.modules.WrenchBlockState.Companion.EMPTY_LIST
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.*
import java.util.stream.Collectors

fun ItemStack.isWrench(): Boolean {
    return isOf(Items.DEBUG_STICK) && nbt?.getBoolean("holywrench") ?: false
}

private fun ItemStack.asWrench(callback: (stack: ItemStack) -> Unit) {
    if (this.isWrench()) {
        callback(this)
    }
}

private fun ItemStack.getWrenchProperty(state: BlockState): Property<*>? {
    if (isWrench()) {
        if (nbt?.contains("DebugProperty") == true) {
            val propertyName = getSubNbt("DebugProperty")!!.getString(Registry.BLOCK.getId(state.block).toString())
            if (!propertyName.equals("")) {
                return state.block.stateManager.getProperty(propertyName)
            }
        }
        return WrenchBlockState.getProperties(state.block)[0]
    }

    return null
}

private fun <T> Array<T>.toCollection(): Collection<T> {
    return Arrays.stream(this).collect(Collectors.toList()) as Collection<T>
}

private fun BlockState.isWrencheable(): Boolean {
    return WrenchBlockState.WrenchBlockStates.containsKey(block)
}

class WrenchBlockState(
    block: Block,
    values: Map<Property<*>, Collection<*>>,
    val validate: (blockState: BlockState, property: Property<*>) -> Boolean = alwaysTrue
) {

    companion object {
        val EMPTY_LIST: ImmutableList<Property<*>> = ImmutableList.copyOf(emptyList())
        val alwaysTrue: (blockState: BlockState, property: Property<*>) -> Boolean = { _, _ -> true }
        val WrenchBlockStates: MutableMap<Block, WrenchBlockState> = mutableMapOf()

        fun getProperties(block: Block): ImmutableList<Property<*>> {
            return WrenchBlockStates[block]?.getProperties() ?: EMPTY_LIST
        }

        fun getValues(block: Block, property: Property<*>): Collection<*> {
            return WrenchBlockStates[block]!!.getValues(property)
        }

        fun isAllowed(state: BlockState, property: Property<*>): Boolean {
            return WrenchBlockStates[state.block]?.validate?.invoke(state, property) ?: false
        }
    }


    private val allowed = ImmutableMap.copyOf(values)

    init {
        WrenchBlockStates[block] = this
    }

    fun getProperties(): ImmutableList<Property<*>> {
        return ImmutableList.copyOf(allowed.keys)
    }

    fun getValues(property: Property<*>): Collection<*> {
        return allowed[property]!!
    }

}

class DebugWrench {

    companion object {

        private fun Property<*>.toPair(): Pair<Property<*>, Collection<*>> {
            return this to this.values
        }

        private fun Property<*>.toMap(): Map<Property<*>, Collection<*>> {
            return mapOf(this.toPair())
        }

        private fun Property<*>.ofValues(vararg values: Any): Pair<Property<*>, Collection<*>> {
            return this to values.toCollection()
        }

        private fun Property<*>.with(vararg props: Property<*>): Map<Property<*>, Collection<*>> {
            return mapOf(this.toPair(), *props.map { it.toPair() }.toTypedArray())
        }

        private fun Property<*>.forBlocks(
            vararg blocks: Block,
            validate: (blockState: BlockState, property: Property<*>) -> Boolean = WrenchBlockState.alwaysTrue
        ) {
            blocks.forEach {
                WrenchBlockState(it, this.toMap(), validate)
            }
        }

        private fun Map<Property<*>, Collection<*>>.forBlocks(
            vararg blocks: Block,
            validate: (blockState: BlockState, property: Property<*>) -> Boolean = WrenchBlockState.alwaysTrue
        ) {
            blocks.forEach {
                WrenchBlockState(it, this, validate)
            }
        }

        private fun Pair<Property<*>, Collection<*>>.forBlocks(
            vararg blocks: Block,
            validate: (blockState: BlockState, property: Property<*>) -> Boolean = WrenchBlockState.alwaysTrue
        ) {
            blocks.forEach {
                WrenchBlockState(it, mapOf(this), validate)
            }
        }

        init {
            MushroomBlock.NORTH.with(
                MushroomBlock.EAST,
                MushroomBlock.WEST,
                MushroomBlock.SOUTH,
                MushroomBlock.UP,
                MushroomBlock.DOWN
            ).forBlocks(Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM)
            BigDripleafBlock.FACING.forBlocks(Blocks.BIG_DRIPLEAF, Blocks.BIG_DRIPLEAF_STEM)
            SmallDripleafBlock.FACING.forBlocks(Blocks.SMALL_DRIPLEAF)
            LightningRodBlock.FACING.forBlocks(Blocks.LIGHTNING_ROD)
            EndRodBlock.FACING.forBlocks(Blocks.END_ROD)
            ChainBlock.AXIS.forBlocks(Blocks.CHAIN)
            DoorBlock.FACING.with(DoorBlock.OPEN, DoorBlock.HINGE).forBlocks(
                Blocks.OAK_DOOR,
                Blocks.SPRUCE_DOOR,
                Blocks.BIRCH_DOOR,
                Blocks.JUNGLE_DOOR,
                Blocks.ACACIA_DOOR,
                Blocks.DARK_OAK_DOOR,
                Blocks.CRIMSON_DOOR,
                Blocks.WARPED_DOOR,
                Blocks.IRON_DOOR
            )
            TrapdoorBlock.FACING.with(TrapdoorBlock.OPEN, TrapdoorBlock.HALF).forBlocks(
                Blocks.OAK_TRAPDOOR,
                Blocks.SPRUCE_TRAPDOOR,
                Blocks.BIRCH_TRAPDOOR,
                Blocks.JUNGLE_TRAPDOOR,
                Blocks.ACACIA_TRAPDOOR,
                Blocks.DARK_OAK_TRAPDOOR,
                Blocks.IRON_TRAPDOOR,
                Blocks.CRIMSON_TRAPDOOR,
                Blocks.WARPED_TRAPDOOR
            )
            PillarBlock.AXIS.forBlocks(
                Blocks.OAK_LOG,
                Blocks.SPRUCE_LOG,
                Blocks.BIRCH_LOG,
                Blocks.JUNGLE_LOG,
                Blocks.ACACIA_LOG,
                Blocks.DARK_OAK_LOG,
                Blocks.STRIPPED_SPRUCE_LOG,
                Blocks.STRIPPED_BIRCH_LOG,
                Blocks.STRIPPED_JUNGLE_LOG,
                Blocks.STRIPPED_ACACIA_LOG,
                Blocks.STRIPPED_DARK_OAK_LOG,
                Blocks.STRIPPED_OAK_LOG,
                Blocks.QUARTZ_PILLAR,
                Blocks.PURPUR_PILLAR,
                Blocks.DEEPSLATE
            )
            FenceBlock.NORTH.with(FenceBlock.EAST, FenceBlock.WEST, FenceBlock.SOUTH).forBlocks(
                Blocks.OAK_FENCE,
                Blocks.NETHER_BRICK_FENCE,
                Blocks.SPRUCE_FENCE,
                Blocks.BIRCH_FENCE,
                Blocks.JUNGLE_FENCE,
                Blocks.ACACIA_FENCE,
                Blocks.DARK_OAK_FENCE,
                Blocks.CRIMSON_FENCE,
                Blocks.WARPED_FENCE
            )
            PaneBlock.NORTH.with(PaneBlock.EAST, PaneBlock.WEST, PaneBlock.SOUTH).forBlocks(
                Blocks.IRON_BARS,
                Blocks.GLASS_PANE,
                Blocks.WHITE_STAINED_GLASS_PANE,
                Blocks.ORANGE_STAINED_GLASS_PANE,
                Blocks.MAGENTA_STAINED_GLASS_PANE,
                Blocks.LIGHT_BLUE_STAINED_GLASS_PANE,
                Blocks.YELLOW_STAINED_GLASS_PANE,
                Blocks.LIME_STAINED_GLASS_PANE,
                Blocks.PINK_STAINED_GLASS_PANE,
                Blocks.GRAY_STAINED_GLASS_PANE,
                Blocks.LIGHT_GRAY_STAINED_GLASS_PANE,
                Blocks.CYAN_STAINED_GLASS_PANE,
                Blocks.PURPLE_STAINED_GLASS_PANE,
                Blocks.BLUE_STAINED_GLASS_PANE,
                Blocks.BROWN_STAINED_GLASS_PANE,
                Blocks.GREEN_STAINED_GLASS_PANE,
                Blocks.RED_STAINED_GLASS_PANE,
                Blocks.BLACK_STAINED_GLASS_PANE
            )
            WallBlock.UP.with(WallBlock.NORTH_SHAPE, WallBlock.EAST_SHAPE, WallBlock.WEST_SHAPE, WallBlock.SOUTH_SHAPE)
                .forBlocks(
                    Blocks.COBBLESTONE_WALL,
                    Blocks.MOSSY_COBBLESTONE_WALL,
                    Blocks.BRICK_WALL,
                    Blocks.PRISMARINE_WALL,
                    Blocks.RED_SANDSTONE_WALL,
                    Blocks.MOSSY_STONE_BRICK_WALL,
                    Blocks.GRANITE_WALL,
                    Blocks.STONE_BRICK_WALL,
                    Blocks.NETHER_BRICK_WALL,
                    Blocks.ANDESITE_WALL,
                    Blocks.RED_NETHER_BRICK_WALL,
                    Blocks.SANDSTONE_WALL,
                    Blocks.END_STONE_BRICK_WALL,
                    Blocks.DIORITE_WALL,
                    Blocks.BLACKSTONE_WALL,
                    Blocks.POLISHED_BLACKSTONE_BRICK_WALL,
                    Blocks.POLISHED_BLACKSTONE_WALL,
                    Blocks.COBBLED_DEEPSLATE_WALL,
                    Blocks.POLISHED_DEEPSLATE_WALL,
                    Blocks.DEEPSLATE_TILE_WALL,
                    Blocks.DEEPSLATE_BRICK_WALL
                )
            SkullBlock.ROTATION.forBlocks(
                Blocks.SKELETON_SKULL,
                Blocks.WITHER_SKELETON_SKULL,
                Blocks.PLAYER_HEAD,
                Blocks.ZOMBIE_HEAD,
                Blocks.CREEPER_HEAD,
                Blocks.DRAGON_HEAD
            )
            WallSkullBlock.FACING.forBlocks(
                Blocks.SKELETON_WALL_SKULL,
                Blocks.WITHER_SKELETON_WALL_SKULL,
                Blocks.PLAYER_WALL_HEAD,
                Blocks.ZOMBIE_WALL_HEAD,
                Blocks.CREEPER_WALL_HEAD,
                Blocks.DRAGON_WALL_HEAD
            )
            AnvilBlock.FACING.forBlocks(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL)
            RedstoneLampBlock.LIT.forBlocks(Blocks.REDSTONE_LAMP)
            HayBlock.AXIS.forBlocks(Blocks.HAY_BLOCK)
            LightBlock.LEVEL_15.forBlocks(Blocks.LIGHT)
            RepeaterBlock.FACING.with(RepeaterBlock.DELAY).forBlocks(Blocks.REPEATER)
            ComparatorBlock.FACING.with(ComparatorBlock.MODE).forBlocks(Blocks.COMPARATOR)
            HopperBlock.FACING.forBlocks(Blocks.HOPPER)
            DropperBlock.FACING.forBlocks(Blocks.DROPPER)
            DispenserBlock.FACING.forBlocks(Blocks.DISPENSER)
            ObserverBlock.FACING.forBlocks(Blocks.OBSERVER)
            StairsBlock.FACING.with(StairsBlock.HALF, StairsBlock.SHAPE).forBlocks(
                Blocks.OAK_STAIRS,
                Blocks.COBBLESTONE_STAIRS,
                Blocks.BRICK_STAIRS,
                Blocks.STONE_BRICK_STAIRS,
                Blocks.NETHER_BRICK_STAIRS,
                Blocks.SANDSTONE_STAIRS,
                Blocks.SPRUCE_STAIRS,
                Blocks.BIRCH_STAIRS,
                Blocks.JUNGLE_STAIRS,
                Blocks.QUARTZ_STAIRS,
                Blocks.ACACIA_STAIRS,
                Blocks.DARK_OAK_STAIRS,
                Blocks.PRISMARINE_STAIRS,
                Blocks.PRISMARINE_BRICK_STAIRS,
                Blocks.DARK_PRISMARINE_STAIRS,
                Blocks.RED_SANDSTONE_STAIRS,
                Blocks.PURPUR_STAIRS,
                Blocks.POLISHED_GRANITE_STAIRS,
                Blocks.SMOOTH_RED_SANDSTONE_STAIRS,
                Blocks.MOSSY_STONE_BRICK_STAIRS,
                Blocks.POLISHED_DIORITE_STAIRS,
                Blocks.MOSSY_COBBLESTONE_STAIRS,
                Blocks.END_STONE_BRICK_STAIRS,
                Blocks.STONE_STAIRS,
                Blocks.SMOOTH_SANDSTONE_STAIRS,
                Blocks.SMOOTH_QUARTZ_STAIRS,
                Blocks.GRANITE_STAIRS,
                Blocks.ANDESITE_STAIRS,
                Blocks.RED_NETHER_BRICK_STAIRS,
                Blocks.POLISHED_ANDESITE_STAIRS,
                Blocks.DIORITE_STAIRS,
                Blocks.CRIMSON_STAIRS,
                Blocks.WARPED_STAIRS,
                Blocks.BLACKSTONE_STAIRS,
                Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
                Blocks.POLISHED_BLACKSTONE_STAIRS,
                Blocks.OXIDIZED_CUT_COPPER_STAIRS,
                Blocks.WEATHERED_CUT_COPPER_STAIRS,
                Blocks.EXPOSED_CUT_COPPER_STAIRS,
                Blocks.CUT_COPPER_STAIRS,
                Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS,
                Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS,
                Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS,
                Blocks.WAXED_CUT_COPPER_STAIRS,
                Blocks.COBBLED_DEEPSLATE_STAIRS,
                Blocks.POLISHED_DEEPSLATE_STAIRS,
                Blocks.DEEPSLATE_TILE_STAIRS,
                Blocks.DEEPSLATE_BRICK_STAIRS
            )
            SlabBlock.TYPE.ofValues(SlabType.BOTTOM, SlabType.TOP).forBlocks(
                Blocks.PRISMARINE_SLAB,
                Blocks.PRISMARINE_BRICK_SLAB,
                Blocks.DARK_PRISMARINE_SLAB,
                Blocks.OAK_SLAB,
                Blocks.SPRUCE_SLAB,
                Blocks.BIRCH_SLAB,
                Blocks.JUNGLE_SLAB,
                Blocks.ACACIA_SLAB,
                Blocks.DARK_OAK_SLAB,
                Blocks.STONE_SLAB,
                Blocks.SMOOTH_STONE_SLAB,
                Blocks.SANDSTONE_SLAB,
                Blocks.CUT_SANDSTONE_SLAB,
                Blocks.PETRIFIED_OAK_SLAB,
                Blocks.COBBLESTONE_SLAB,
                Blocks.BRICK_SLAB,
                Blocks.STONE_BRICK_SLAB,
                Blocks.NETHER_BRICK_SLAB,
                Blocks.QUARTZ_SLAB,
                Blocks.RED_SANDSTONE_SLAB,
                Blocks.CUT_RED_SANDSTONE_SLAB,
                Blocks.PURPUR_SLAB,
                Blocks.POLISHED_GRANITE_SLAB,
                Blocks.SMOOTH_RED_SANDSTONE_SLAB,
                Blocks.MOSSY_STONE_BRICK_SLAB,
                Blocks.POLISHED_DIORITE_SLAB,
                Blocks.MOSSY_COBBLESTONE_SLAB,
                Blocks.END_STONE_BRICK_SLAB,
                Blocks.SMOOTH_SANDSTONE_SLAB,
                Blocks.SMOOTH_QUARTZ_SLAB,
                Blocks.GRANITE_SLAB,
                Blocks.ANDESITE_SLAB,
                Blocks.RED_NETHER_BRICK_SLAB,
                Blocks.POLISHED_ANDESITE_SLAB,
                Blocks.DIORITE_SLAB,
                Blocks.CRIMSON_SLAB,
                Blocks.WARPED_SLAB,
                Blocks.BLACKSTONE_SLAB,
                Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
                Blocks.POLISHED_BLACKSTONE_SLAB,
                Blocks.OXIDIZED_CUT_COPPER_SLAB,
                Blocks.WEATHERED_CUT_COPPER_SLAB,
                Blocks.EXPOSED_CUT_COPPER_SLAB,
                Blocks.CUT_COPPER_SLAB,
                Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB,
                Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB,
                Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB,
                Blocks.WAXED_CUT_COPPER_SLAB,
                Blocks.COBBLED_DEEPSLATE_SLAB,
                Blocks.POLISHED_DEEPSLATE_SLAB,
                Blocks.DEEPSLATE_TILE_SLAB,
                Blocks.DEEPSLATE_BRICK_SLAB
            ) { state, property -> property == SlabBlock.TYPE && !state.get(property).equals(SlabType.DOUBLE) }
            GlazedTerracottaBlock.FACING.forBlocks(
                Blocks.WHITE_GLAZED_TERRACOTTA,
                Blocks.ORANGE_GLAZED_TERRACOTTA,
                Blocks.MAGENTA_GLAZED_TERRACOTTA,
                Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
                Blocks.YELLOW_GLAZED_TERRACOTTA,
                Blocks.LIME_GLAZED_TERRACOTTA,
                Blocks.PINK_GLAZED_TERRACOTTA,
                Blocks.GRAY_GLAZED_TERRACOTTA,
                Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA,
                Blocks.CYAN_GLAZED_TERRACOTTA,
                Blocks.PURPLE_GLAZED_TERRACOTTA,
                Blocks.BLUE_GLAZED_TERRACOTTA,
                Blocks.BROWN_GLAZED_TERRACOTTA,
                Blocks.GREEN_GLAZED_TERRACOTTA,
                Blocks.RED_GLAZED_TERRACOTTA,
                Blocks.BLACK_GLAZED_TERRACOTTA
            )
            PistonBlock.FACING.forBlocks(
                Blocks.PISTON,
                Blocks.STICKY_PISTON
            ) { state, _ -> !state.get(PistonBlock.EXTENDED) }
            RailBlock.SHAPE.ofValues(
                RailShape.NORTH_SOUTH,
                RailShape.ASCENDING_NORTH,
                RailShape.ASCENDING_SOUTH,
                RailShape.NORTH_EAST,
                RailShape.EAST_WEST,
                RailShape.ASCENDING_EAST,
                RailShape.ASCENDING_WEST,
                RailShape.SOUTH_EAST,
                RailShape.SOUTH_WEST,
                RailShape.NORTH_WEST
            ).forBlocks(Blocks.RAIL)
            PoweredRailBlock.SHAPE.ofValues(
                RailShape.NORTH_SOUTH,
                RailShape.ASCENDING_NORTH,
                RailShape.ASCENDING_SOUTH,
                RailShape.EAST_WEST,
                RailShape.ASCENDING_EAST,
                RailShape.ASCENDING_WEST
            ).forBlocks(Blocks.POWERED_RAIL, Blocks.ACTIVATOR_RAIL)
            DetectorRailBlock.SHAPE.ofValues(
                RailShape.NORTH_SOUTH,
                RailShape.ASCENDING_NORTH,
                RailShape.ASCENDING_SOUTH,
                RailShape.EAST_WEST,
                RailShape.ASCENDING_EAST,
                RailShape.ASCENDING_WEST
            ).forBlocks(Blocks.DETECTOR_RAIL)
            StonecutterBlock.FACING.forBlocks(Blocks.STONECUTTER)
        }

        fun hackShapedRecipe(identifier: Identifier, group: String, width: Int, height: Int, input: DefaultedList<Ingredient>, output: ItemStack): ShapedRecipe {
            if (identifier.namespace == "holywrench" && identifier.path == "holywrench") {
                output.orCreateNbt.apply tag@{
                    putBoolean("holywrench", true)
                    put("display", NbtCompound().apply display@{
                        this@display.put("Lore", NbtList().apply lore@{
                            add(0, NbtString.of("[{\"text\":\"La plus simple des façons de modifier vos blockStates.\",\"italic\":false}]"))
                        })
                        this@display.putString("Name", "[{\"text\":\"HolyWrench\",\"italic\":true}]")
                    })
                }
            }

            return ShapedRecipe(identifier, group, width, height, input, output)
        }

        fun processBlockBreakingAction(player: ServerPlayerEntity, pos: BlockPos, world: ServerWorld, info: CallbackInfo) {
            player.mainHandStack.asWrench {
                it.item.canMine(world.getBlockState(pos), world, pos, player)
                info.cancel()
            }
        }

        fun allowWrench(player: PlayerEntity, stack: ItemStack): Boolean {
            return player.isCreativeLevelTwoOp || stack.isWrench()
        }

        fun getProperties(player: PlayerEntity, stateManager: StateManager<*, *>, state: BlockState, stack: ItemStack): ImmutableList<Property<*>> {
            if(player.isCreativeLevelTwoOp) {
                return stateManager.properties as ImmutableList<Property<*>>
            }

            if (stack.isWrench() && state.isWrencheable()) {
                val property = stack.getWrenchProperty(state) ?: return EMPTY_LIST
                if (WrenchBlockState.isAllowed(state, property)) {
                    return WrenchBlockState.getProperties(state.block)
                }
            }

            return EMPTY_LIST
        }

        fun <T> getValues(state: BlockState, property: Property<*>): Collection<T> {
            @Suppress("UNCHECKED_CAST")
            return WrenchBlockState.getValues(state.block, property) as Collection<T>
        }

        fun useOnSign(context: ItemUsageContext, info: CallbackInfoReturnable<ActionResult>) {
            if(context.stack.isWrench()) {
                if(context.world.getBlockState(context.blockPos).isIn(BlockTags.SIGNS)) {
                    if (!context.world.isClient && context.player != null) {
                        context.player!!.openEditSignScreen(context.world.getBlockEntity(context.blockPos) as SignBlockEntity)
                    }
                }
            }
        }
    }
}
