package net.steampn.createhorsepower.blocks.horse_crank;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import net.steampn.createhorsepower.config.Config;
import net.steampn.createhorsepower.registry.TileEntityRegister;
import net.steampn.createhorsepower.utils.CHPShapes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class HorseCrankBlock extends KineticBlock implements ICogWheel, IBE<HorseCrankTileEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BooleanProperty HAS_WORKER = BooleanProperty.create("has_worker");
    public static final BooleanProperty SMALL_WORKER_STATE = BooleanProperty.create("small_worker");
    public static final BooleanProperty MEDIUM_WORKER_STATE = BooleanProperty.create("medium_worker");
    public static final BooleanProperty LARGE_WORKER_STATE = BooleanProperty.create("large_worker");
    public HorseCrankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(HAS_WORKER, false)
                .setValue(SMALL_WORKER_STATE, false)
                .setValue(MEDIUM_WORKER_STATE, false)
                .setValue(LARGE_WORKER_STATE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(HAS_WORKER,SMALL_WORKER_STATE, MEDIUM_WORKER_STATE, LARGE_WORKER_STATE));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter getter, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return CHPShapes.HORSE_CRANK;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.hasBlockEntity() && pState.getBlock() != pNewState.getBlock()) pLevel.removeBlockEntity(pPos);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean isPathfindable(@NotNull BlockState state, @NotNull BlockGetter getter, @NotNull BlockPos pos, @NotNull PathComputationType type) {
        return false;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public Class<HorseCrankTileEntity> getBlockEntityClass() {
        return HorseCrankTileEntity.class;
    }

    @Override
    public BlockEntityType<? extends HorseCrankTileEntity> getBlockEntityType() {
        return TileEntityRegister.HORSE_CRANK.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityRegister.HORSE_CRANK.create(pos,state);
    }

    @Override
    public InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        // Client side show hand interacting only
        if (level.isClientSide()) return InteractionResult.PASS;
        // If hand interacting was not Main Hand, ignore
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack itemStack = player.getItemInHand(hand);

        List<Mob> mobsNearPlayer = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(7.0D));
        //Look for mobs leashed to player
        long leashedCount = mobsNearPlayer.stream()
                .filter(mob -> mob.isLeashed() && player.equals(mob.getLeashHolder())).count();

        // Attach mob if leading one mob
        if (leashedCount == 1){
            Mob mob = getMob(mobsNearPlayer, player);
            long leashKnots = level.getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB(pos).inflate(0.2D)).size();

            if (leashKnots > 0) {

                player.displayClientMessage(Component.translatable("tooltip.createhorsepower.horse_crank.alreadyHasWorker"), true);
                return InteractionResult.PASS;

            }
            if (!verifyMobIsInConfig(getMobType(mob), level, pos, state)) {
                player.displayClientMessage(Component.translatable("tooltip.createhorsepower.horse_crank.notValidWorker"), true);
                return InteractionResult.FAIL;
            }

            ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());

            LeadItem.bindPlayerMobs(player, level, pos);

            boolean small = Config.small_mobs.contains(mobId);
            boolean medium = Config.medium_mobs.contains(mobId);
            boolean large = Config.large_mobs.contains(mobId);

            BlockState updatedState = state.setValue(HAS_WORKER, true)
                    .setValue(SMALL_WORKER_STATE, small)
                    .setValue(MEDIUM_WORKER_STATE, medium)
                    .setValue(LARGE_WORKER_STATE, large);

            level.setBlock(pos, updatedState, 3);
            player.displayClientMessage(Component.translatable("tooltip.createhorsepower.horse_crank.attached"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);


        }

        if (itemStack.isEmpty() && leashedCount == 0) {
            level.setBlock(pos, state.setValue(HAS_WORKER, false)
                    .setValue(SMALL_WORKER_STATE, false)
                    .setValue(MEDIUM_WORKER_STATE, false)
                    .setValue(LARGE_WORKER_STATE, false), 3);
//            if (!player.getInventory().add(new ItemStack(Items.LEAD))) {
//                player.drop(new ItemStack(Items.LEAD), false);
//            }
            player.displayClientMessage(Component.translatable("tooltip.createhorsepower.horse_crank.detached"), true);
            return killLeashEntity(level, pos);

        }
        return InteractionResult.FAIL;
    }

    private InteractionResult killLeashEntity(Level level, BlockPos pos){
        level.getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB(pos).inflate(0.2D))
                .forEach(Entity::kill);
        return InteractionResult.SUCCESS;
    }

    private Mob getMob(List<Mob> mobsNearPlayer, Player player){
        return mobsNearPlayer.stream()
                .filter(Mob::isLeashed)
                .filter(mob -> player.equals(mob.getLeashHolder()))
                .findFirst()
                .orElse(null);
    }

    private ResourceLocation getMobType(Mob mob){
        return ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
    }

    private boolean verifyMobIsInConfig(ResourceLocation mobType, Level level, BlockPos pos, BlockState state){
        boolean valid = false, small = false, medium = false, large = false;

        if (Config.small_mobs.contains(mobType)){
            small = true;
            valid = true;
        }
        else if (Config.medium_mobs.contains(mobType)) {
            medium = true;
            valid = true;
        }
        else if (Config.large_mobs.contains(mobType)){
            large = true;
            valid = true;
        }
        level.setBlock(pos, state.setValue(HAS_WORKER, valid).setValue(SMALL_WORKER_STATE, small).setValue(MEDIUM_WORKER_STATE, medium).setValue(LARGE_WORKER_STATE, large), 3);
        return small || medium || large;
    }
}
