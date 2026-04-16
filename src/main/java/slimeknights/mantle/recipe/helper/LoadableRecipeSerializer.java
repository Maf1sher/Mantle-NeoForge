package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import java.util.Map;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Recipe serializer instance using loadables.
 * @param <T>  Recipe type
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class LoadableRecipeSerializer<T extends Recipe<?>> implements LoggingRecipeSerializer<T> {
  /** Context key to use if you want the recipe serializer passed into your recipe */
  public static final ContextKey<RecipeSerializer<?>> SERIALIZER = new ContextKey<>("serializer");
  /** Context key to use if you want a type aware serializer in the recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<TypeAwareRecipeSerializer<?>> TYPED_SERIALIZER = new ContextKey<>("typed_serializer");
  /** Context key to use if you want the recipe type passed into your recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<RecipeType<?>> TYPE = new ContextKey<>("type");
  /** Field for a group key in a recipe (common requirement) */
  public static final LoadableField<String,Recipe<?>> RECIPE_GROUP = StringLoadable.DEFAULT.defaultField("group", "", Recipe::getGroup);


  protected final RecordLoadable<T> loadable;

  /** Creates a standard serializer from a loadable */
  public static <T extends Recipe<?>> RecipeSerializer<T> of(RecordLoadable<T> loadable) {
    return new LoadableRecipeSerializer<>(loadable);
  }

  /** Creates a type aware serializer from a loadable */
  public static <T extends R, R extends Recipe<?>> TypeAwareRecipeSerializer<T> of(RecordLoadable<T> loadable, Supplier<? extends RecipeType<R>> type) {
    return new TypeAware<>(loadable, type);
  }

  /** Creates a serializer that is deprecated, logging a warning when used */
  public static <T extends Recipe<?>> RecipeSerializer<T> deprecated(RecordLoadable<T> loadable, String replacement) {
    return new Deprecated<>(loadable, replacement);
  }

  /** Builds a context for recipe deserialization */
  protected TypedMapBuilder buildContext() {
    return TypedMapBuilder.builder().put(SERIALIZER, this);
  }

  /** Builds a context for recipe deserialization including the recipe ID when available */
  protected TypedMapBuilder buildContext(ResourceLocation id) {
    TypedMapBuilder builder = buildContext();
    if (id != null) {
      builder.put(ContextKey.ID, id);
    }
    return builder;
  }

  /** Extracts the recipe ID for network sync. Custom recipes using this serializer are expected to expose getId(). */
  protected ResourceLocation getRecipeId(T recipe) {
    try {
      Method method = recipe.getClass().getMethod("getId");
      Object result = method.invoke(recipe);
      if (result instanceof ResourceLocation id) {
        return id;
      }
      throw new IllegalStateException("Recipe getId() did not return a ResourceLocation: " + recipe.getClass().getName());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Recipe using LoadableRecipeSerializer must expose getId(): " + recipe.getClass().getName(), e);
    }
  }

  /** Reads an optional ID field from codec input for external parsers that include it inline. */
  protected <O> ResourceLocation getContextId(DynamicOps<O> ops, MapLike<O> input) {
    O idValue = input.get("id");
    if (idValue == null) {
      return null;
    }
    return ops.getStringValue(idValue).result().map(ResourceLocation::tryParse).orElse(null);
  }

  @Override
  public MapCodec<T> codec() {
    return new MapCodec<>() {
      @Override
      public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
        try {
          // Convert DynamicOps MapLike input to a JsonObject
          JsonObject json = new JsonObject();
          input.entries().forEach(pair -> {
            String key = ops.getStringValue(pair.getFirst()).getOrThrow();
            O value = pair.getSecond();
            json.add(key, ops.convertTo(com.mojang.serialization.JsonOps.INSTANCE, value));
          });
          T result = loadable.deserialize(json, buildContext(getContextId(ops, input)).build());
          return DataResult.success(result);
        } catch (Exception e) {
          return DataResult.error(e::getMessage);
        }
      }

      @Override
      public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        try {
          JsonObject json = new JsonObject();
          loadable.serialize(input, json);
          for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
            prefix.add(entry.getKey(), com.mojang.serialization.JsonOps.INSTANCE.convertTo(ops, entry.getValue()));
          }
          return prefix;
        } catch (Exception e) {
          return prefix.withErrorsFrom(DataResult.error(e::getMessage));
        }
      }

      @Override
      public <O> Stream<O> keys(DynamicOps<O> ops) {
        return Stream.empty();
      }
    };
  }

  @Override
  public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
    return StreamCodec.of(
      (buffer, recipe) -> {
        try {
          buffer.writeResourceLocation(getRecipeId(recipe));
          loadable.encode(buffer, recipe);
        } catch (RuntimeException e) {
          Mantle.logger.error("{}: Error writing recipe to packet using loadable {}", LoadableRecipeSerializer.this.getClass().getSimpleName(), loadable, e);
          throw e;
        }
      },
      buffer -> {
        try {
          ResourceLocation id = buffer.readResourceLocation();
          return loadable.decode(buffer, buildContext(id).build());
        } catch (RuntimeException e) {
          Mantle.logger.error("{}: Error reading recipe from packet using loadable {}", LoadableRecipeSerializer.this.getClass().getSimpleName(), loadable, e);
          throw e;
        }
      }
    );
  }

  public static class TypeAware<T extends Recipe<?>> extends LoadableRecipeSerializer<T> implements TypeAwareRecipeSerializer<T> {
    private final Supplier<? extends RecipeType<?>> type;
    protected TypeAware(RecordLoadable<T> loadable, Supplier<? extends RecipeType<?>> type) {
      super(loadable);
      this.type = type;
    }

    @Override
    protected TypedMapBuilder buildContext() {
      return super.buildContext().put(TYPE, getType()).put(TYPED_SERIALIZER, this);
    }

    @Override
    public RecipeType<?> getType() {
      return type.get();
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
      return StreamCodec.of(
        (buffer, recipe) -> {
          try {
            buffer.writeResourceLocation(getRecipeId(recipe));
            loadable.encode(buffer, recipe);
          } catch (RuntimeException e) {
            Mantle.logger.error("{}: Error writing recipe of type {} to packet using loadable {}", TypeAware.this.getClass().getSimpleName(), getType(), loadable, e);
            throw e;
          }
        },
        buffer -> {
          try {
            ResourceLocation id = buffer.readResourceLocation();
            return loadable.decode(buffer, buildContext(id).build());
          } catch (RuntimeException e) {
            Mantle.logger.error("{}: Error reading recipe of type {} from packet using loadable {}", TypeAware.this.getClass().getSimpleName(), getType(), loadable, e);
            throw e;
          }
        }
      );
    }
  }

  /** Helper class that logs a warning on recipe parse about planned removal */
  private static class Deprecated<T extends Recipe<?>> extends LoadableRecipeSerializer<T> {
    private final String replacement;
    protected Deprecated(RecordLoadable<T> loadable, String replacement) {
      super(loadable);
      this.replacement = replacement;
    }

    @Override
    public MapCodec<T> codec() {
      MapCodec<T> original = super.codec();
      return original.xmap(
        recipe -> {
          Mantle.logger.warn("Using deprecated recipe serializer {}, {}", BuiltInRegistries.RECIPE_SERIALIZER.getKey(this), replacement);
          return recipe;
        },
        recipe -> recipe
      );
    }
  }
}
