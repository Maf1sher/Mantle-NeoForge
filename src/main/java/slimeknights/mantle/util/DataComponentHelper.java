package slimeknights.mantle.util;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.List;

/** Helpers for working with data components on stacks */
public class DataComponentHelper {
  private DataComponentHelper() {}

  /** Creates an item stack with the given components */
  public static ItemStack makeStack(ItemLike item, int count, DataComponentPatch components) {
    if (item == Items.AIR || count <= 0) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = new ItemStack(item, count);
    if (!components.isEmpty()) {
      stack.applyComponents(components);
    }
    return stack;
  }

  /** Creates an item stack with the given components */
  public static ItemStack makeStack(ItemLike item, DataComponentPatch components) {
    return makeStack(item, 1, components);
  }

  /** Creates a fluid stack with the given components */
  public static FluidStack makeStack(Fluid fluid, int amount, DataComponentPatch components) {
    if (fluid == Fluids.EMPTY || amount <= 0) {
      return FluidStack.EMPTY;
    }
    FluidStack stack = new FluidStack(fluid, amount);
    if (!components.isEmpty()) {
      stack.applyComponents(components);
    }
    return stack;
  }

  /** Copies the given data component to the given stack */
  public static <T, S extends MutableDataComponentHolder> S copy(S to, DataComponentHolder from, DataComponentType<T> type) {
    T value = from.get(type);
    if (value != null) {
      to.set(type, value);
    }
    return to;
  }

  /** Copies the given data components to the given stack */
  @Contract("_,_,_->param1")
  public static <S extends MutableDataComponentHolder> S copy(S to, DataComponentHolder from, List<DataComponentType<?>> types) {
    for (DataComponentType<?> type : types) {
      copy(to, from, type);
    }
    return to;
  }

  /**
   * Gets the mutable custom data tag from the given stack, or null if absent.
   * Mirrors the removed {@code ItemStack#getTag()} and {@code FluidStack#getTag()}; keep the returned tag in sync by using
   * {@link #getOrCreateCustomData(MutableDataComponentHolder)} or {@link #setCustomData(MutableDataComponentHolder, CompoundTag)}.
   */
  @Nullable
  public static CompoundTag getCustomData(DataComponentHolder holder) {
    CustomData data = holder.get(DataComponents.CUSTOM_DATA);
    return data == null ? null : data.getUnsafe();
  }

  /** Checks if the given stack has custom data */
  public static boolean hasCustomData(DataComponentHolder holder) {
    return holder.has(DataComponents.CUSTOM_DATA);
  }

  /**
   * Gets the mutable custom data tag from the given stack, creating an empty one if absent.
   * Mirrors the removed {@code ItemStack#getOrCreateTag()}.
   * <p>
   * The returned tag is live and may be shared with other stacks: unlike 1.20.1, {@code ItemStack#copy()} in 1.21 keeps
   * the same {@link CustomData} component instance, so a copy still points at the original tag. Call
   * {@link #copyCustomData(MutableDataComponentHolder)} after copying a stack before mutating the returned tag.
   */
  public static CompoundTag getOrCreateCustomData(MutableDataComponentHolder holder) {
    CustomData data = holder.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      data = CustomData.of(new CompoundTag());
      holder.set(DataComponents.CUSTOM_DATA, data);
    }
    return data.getUnsafe();
  }

  /** Sets the custom data tag on the given stack, removing it when the tag is empty */
  public static void setCustomData(MutableDataComponentHolder holder, CompoundTag tag) {
    if (tag.isEmpty()) {
      holder.remove(DataComponents.CUSTOM_DATA);
    } else {
      holder.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
  }

  /** Copies the custom data from one stack to another, removing the target data when the source has none */
  @Contract("_,_->param1")
  public static <S extends MutableDataComponentHolder> S copyCustomData(S to, DataComponentHolder from) {
    CompoundTag tag = getCustomData(from);
    if (tag == null || tag.isEmpty()) {
      to.remove(DataComponents.CUSTOM_DATA);
    } else {
      to.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
    }
    return to;
  }

  /**
   * Copies the holder's custom data onto itself, giving it its own tag.
   * <p>
   * Use this on a stack created with {@code ItemStack#copy()} or {@code ItemStack#copyWithCount(int)} before mutating its
   * custom data: in 1.21 those methods keep the source's {@link CustomData} instance, so in-place edits would otherwise
   * modify the original stack as well.
   */
  @Contract("_->param1")
  public static <S extends MutableDataComponentHolder> S copyCustomData(S holder) {
    return copyCustomData(holder, holder);
  }
}
