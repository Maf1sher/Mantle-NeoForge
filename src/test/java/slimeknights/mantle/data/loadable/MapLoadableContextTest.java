package slimeknights.mantle.data.loadable;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.mapping.MapLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regression test for map keys losing the parsing context. */
public class MapLoadableContextTest {
  private static final ContextKey<String> PREFIX = new ContextKey<>("prefix");

  /** Key loadable that prefixes the parsed value using the context, failing if the context is dropped */
  private static final StringLoadable<Integer> CONTEXT_KEY = new StringLoadable<>() {
    @Override
    public Integer parseString(String value, String key, TypedMap context) {
      return Integer.parseInt(context.getOrThrow(PREFIX, ErrorFactory.JSON_SYNTAX_ERROR) + value);
    }

    @Override
    public String getString(Integer object, TypedMap context) {
      return Integer.toString(object);
    }

    @Override
    public Integer decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      return buffer.readVarInt();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, Integer value, TypedMap context) {
      buffer.writeVarInt(value);
    }
  };

  @Test
  void mapKeysReceiveContext() {
    //noinspection unchecked
    MapLoadable<Integer,Integer> loadable = (MapLoadable<Integer,Integer>) CONTEXT_KEY.mapWithValues(IntLoadable.FROM_ZERO, 1);
    JsonObject json = new JsonObject();
    json.addProperty("2", 3);
    TypedMap context = TypedMapBuilder.builder().put(PREFIX, "1").build();

    Map<Integer,Integer> parsed = loadable.convert(json, "test", context);
    assertEquals(Map.of(12, 3), parsed);
  }
}
