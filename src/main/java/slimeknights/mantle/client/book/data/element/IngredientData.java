package slimeknights.mantle.client.book.data.element;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.repository.BookRepository;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

public class IngredientData implements IDataElement {
  public SizedIngredient[] ingredients = new SizedIngredient[0];
  public String action;
  public String nbt;

  private transient String error;
  private transient NonNullList<ItemStack> items;
  private transient boolean customData;
  private transient net.minecraft.nbt.CompoundTag nbtTag;

  public NonNullList<ItemStack> getItems() {
    return this.items;
  }

  public static IngredientData getItemStackData(ItemStack stack) {
    IngredientData data = new IngredientData();
    data.items = NonNullList.withSize(1, stack);
    data.customData = true;

    return data;
  }

  public static IngredientData getItemStackData(NonNullList<ItemStack> items) {
    IngredientData data = new IngredientData();
    data.items = items;
    data.customData = true;

    return data;
  }

  @Override
  public void load(BookRepository source) {
    if (this.customData) {
      return;
    }

    ArrayList<ItemStack> stacks = new ArrayList<>();
    for(SizedIngredient ingredient : ingredients) {
      if(ingredient == null) {
        continue;
      }

      if (this.nbt != null || this.nbtTag != null) {
        for (ItemStack stack : ingredient.getMatchingStacks()) {
          ItemStack copy = stack.copy();
          try {
            net.minecraft.nbt.CompoundTag tag;
            if (this.nbtTag != null) {
              tag = this.nbtTag;
            } else {
              tag = net.minecraft.nbt.TagParser.parseTag(this.nbt);
            }
            copy.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
          } catch (Exception e) {
            Mantle.logger.error("Failed to parse NBT for ingredient display in book: " + this.nbt, e);
          }
          stacks.add(copy);
        }
      } else {
        stacks.addAll(ingredient.getMatchingStacks());
      }
    }

    if(ingredients == null || stacks.isEmpty() || !StringUtil.isNullOrEmpty(error)) {
      items = NonNullList.withSize(1, getMissingItem());
      return;
    }

    items = NonNullList.of(getMissingItem(), stacks.toArray(new ItemStack[0]));
  }

  private ItemStack getMissingItem() {
    return getMissingItem(this.error);
  }

  private ItemStack getMissingItem(String error) {
    ItemStack missingItem = new ItemStack(Items.BARRIER);

    missingItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Error Loading Item"));
    if(!StringUtil.isNullOrEmpty(error)) {
      missingItem.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
        net.minecraft.network.chat.Component.literal("Error:").withStyle(net.minecraft.ChatFormatting.YELLOW),
        net.minecraft.network.chat.Component.literal(error).withStyle(net.minecraft.ChatFormatting.YELLOW)
      )));
    }

    return missingItem;
  }

  public static class Deserializer implements JsonDeserializer<IngredientData> {
  /** Converts a JSON object to an NBT compound tag by iterating its primitive fields */
  private static net.minecraft.nbt.CompoundTag jsonToCompound(net.minecraft.nbt.CompoundTag target, JsonObject json) {
    for (Map.Entry<String,JsonElement> entry : json.entrySet()) {
      String key = entry.getKey();
      JsonElement val = entry.getValue();
      if (val.isJsonPrimitive()) {
        JsonPrimitive p = val.getAsJsonPrimitive();
        if (p.isNumber()) {
          target.putDouble(key, p.getAsDouble());
        } else if (p.isBoolean()) {
          target.putBoolean(key, p.getAsBoolean());
        } else if (p.isString()) {
          target.putString(key, p.getAsString());
        }
      }
    }
    return target;
  }

  @Override
    public IngredientData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      IngredientData data = new IngredientData();

      if(json.isJsonArray()) {
        JsonArray array = json.getAsJsonArray();
        data.ingredients = new SizedIngredient[array.size()];

        for(int i = 0; i < array.size(); i++) {
          try {
            data.ingredients[i] = readIngredient(array.get(i));
          } catch (Exception e) {
            data.ingredients[i] = SizedIngredient.of(Ingredient.of(data.getMissingItem(e.getMessage())));
          }
        }

        return data;
      }

      try {
        data.ingredients = new SizedIngredient[]{ readIngredient(json) };
      } catch (Exception e) {
        data.error = e.getMessage();
        return data;
      }

      if(json.isJsonObject()) {
        JsonObject object = json.getAsJsonObject();
        if (object.has("nbt")) {
          JsonElement nbtEl = object.get("nbt");
          if (nbtEl.isJsonObject()) {
            // direct JSON object — convert to CompoundTag immediately
            data.nbtTag = jsonToCompound(new net.minecraft.nbt.CompoundTag(), nbtEl.getAsJsonObject());
          } else if (nbtEl.isJsonPrimitive() && nbtEl.getAsJsonPrimitive().isString()) {
            data.nbt = nbtEl.getAsString();
          }
        }
        if (object.has("action")) {
          JsonElement action = object.get("action");
          if (action.isJsonPrimitive()) {
            JsonPrimitive primitive = action.getAsJsonPrimitive();
            if (primitive.isString()) {
              data.action = primitive.getAsString();
            }
          }
        }
      }

      return data;
    }

    private SizedIngredient readIngredient(JsonElement json) {
      if(json.isJsonPrimitive()) {
        JsonPrimitive primitive = json.getAsJsonPrimitive();

        if(primitive.isString()) {
          Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(primitive.getAsString()));
          return SizedIngredient.fromItems(item);
        }
      }

      if(!json.isJsonObject()) {
        throw new JsonParseException("Must be an array, string or JSON object");
      }

      JsonObject object = json.getAsJsonObject();
      return SizedIngredient.LOADABLE.deserialize(object);
    }
  }
}
