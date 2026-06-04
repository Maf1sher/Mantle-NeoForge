package slimeknights.mantle.recipe.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;

/**
 * Ingredient for a non-NBT sensitive item from another mod, should never be used outside datagen.
 * This is a pure JSON generation utility - it does not participate in runtime ingredient matching.
 *
 * In NeoForge 1.21.1, Ingredient is final and AbstractIngredient was removed,
 * so this class generates ingredient JSON directly without inheriting from Ingredient.
 */
public class ItemNameIngredient {
  private final List<ResourceLocation> names;

  protected ItemNameIngredient(List<ResourceLocation> names) {
    this.names = names;
  }

  /** Creates a new ingredient from a list of names */
  public static ItemNameIngredient from(List<ResourceLocation> names) {
    return new ItemNameIngredient(names);
  }

  /** Creates a new ingredient from a list of names */
  public static ItemNameIngredient from(ResourceLocation... names) {
    return from(Arrays.asList(names));
  }

  /** Creates a JSON object for a single item name */
  private static JsonObject forName(ResourceLocation name) {
    JsonObject json = new JsonObject();
    json.addProperty("item", name.toString());
    return json;
  }

  /** Serializes this ingredient to JSON for use in datagen */
  public JsonElement toJson() {
    if (names.size() == 1) {
      return forName(names.get(0));
    }
    JsonArray array = new JsonArray();
    for (ResourceLocation name : names) {
      array.add(forName(name));
    }
    return array;
  }

  /**
   * Converts this ingredient to a real {@link Ingredient} by parsing the generated JSON via the Ingredient codec.
   * This allows ItemNameIngredient to be used wherever an Ingredient parameter is required in recipe builders.
   * The resulting ingredient references items by name that may not exist at datagen time (cross-mod compat).
   */
  public Ingredient toIngredient() {
    try {
      return Ingredient.CODEC.parse(JsonOps.INSTANCE, toJson())
        .getOrThrow(msg -> new IllegalStateException("Failed to parse ItemNameIngredient to Ingredient: " + msg));
    } catch (RuntimeException e) {
      return Ingredient.EMPTY;
    }
  }
}
