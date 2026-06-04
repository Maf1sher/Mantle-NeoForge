package slimeknights.mantle.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invoker mixin exposing private FlowingFluid methods needed by InvertedFluid.
 */
@Mixin(FlowingFluid.class)
public interface FlowingFluidInvoker {
  @Invoker("sourceNeighborCount")
  int mantle$sourceNeighborCount(LevelReader level, BlockPos pos);

  @Invoker("isWaterHole")
  boolean mantle$isWaterHole(BlockGetter level, Fluid fluid, BlockPos pos, BlockState block, BlockPos spreadPos, BlockState spreadBlock);

  @Invoker("canPassThrough")
  boolean mantle$canPassThrough(BlockGetter level, Fluid fluid, BlockPos pos, BlockState state, Direction direction, BlockPos sidePos, BlockState sideState, FluidState sideFluidState);

  @Invoker("canPassThroughWall")
  boolean mantle$canPassThroughWall(Direction direction, BlockGetter level, BlockPos pos, BlockState state, BlockPos sidePos, BlockState sideState);

  @Invoker("canHoldFluid")
  boolean mantle$canHoldFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid);

  @Invoker("isSourceBlockOfThisType")
  boolean mantle$isSourceBlockOfThisType(FluidState state);

  @Invoker("affectsFlow")
  boolean mantle$affectsFlow(FluidState state);

  @Invoker("getCacheKey")
  short mantle$getCacheKey(BlockPos pos, BlockPos side);

  @Invoker("spreadToSides")
  void mantle$spreadToSides(Level level, BlockPos pos, FluidState fluid, BlockState block);
}
