package slimeknights.mantle.client.book.data.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.lang.reflect.Type;

/**
 * Deserializer for {@link Component} that handles both string literals and full JSON component objects.
 * Strings are converted to literal text components, while objects are parsed using the component codec.
 */
public class ComponentDeserializer implements JsonDeserializer<Component> {
  @Override
  public Component deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    if (json == null || json.isJsonNull()) {
      return null;
    }
    // handle strings that contain malformed JSON by reparsing leniently
    if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
      String s = json.getAsString();
      if (!s.isEmpty()) {
        // try to parse as a component object if it looks like JSON
        if ((s.startsWith("{") || s.startsWith("[")) && s.endsWith(s.startsWith("{") ? "}" : "]")) {
          try {
            // lenient parse for malformed JSON (missing quotes around keys, etc)
            JsonElement reparsed = JsonParser.parseString(s);
            if (reparsed != null && !reparsed.equals(json)) {
              return ComponentSerialization.FLAT_CODEC.parse(JsonOps.INSTANCE, reparsed)
                .getOrThrow(msg -> new JsonParseException("Failed to parse Component: " + msg));
            }
          } catch (Exception ignored) {
            // fall through to literal
          }
        }
      }
      return Component.literal(s);
    }
    return ComponentSerialization.FLAT_CODEC.parse(JsonOps.INSTANCE, json)
      .getOrThrow(msg -> new JsonParseException("Failed to parse Component: " + msg));
  }
}
