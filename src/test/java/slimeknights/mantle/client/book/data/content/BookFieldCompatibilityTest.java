package slimeknights.mantle.client.book.data.content;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookFieldCompatibilityTest {
  private static final ExclusionStrategy UNUSED_RENDER_FIELDS = new ExclusionStrategy() {
    @Override
    public boolean shouldSkipField(FieldAttributes field) {
      return field.getName().equals("image") || field.getName().equals("tooltip");
    }

    @Override
    public boolean shouldSkipClass(Class<?> type) {
      return false;
    }
  };
  private static final Gson GSON = new GsonBuilder()
    .addDeserializationExclusionStrategy(UNUSED_RENDER_FIELDS)
    .addSerializationExclusionStrategy(UNUSED_RENDER_FIELDS)
    .create();

  @Test
  void readsAndWritesLegacyTextLeftImageFields() {
    ContentTextLeftImage content = GSON.fromJson("""
      {"text1":[{"text":"right"}],"text2":[{"text":"bottom"}]}
      """, ContentTextLeftImage.class);

    assertEquals("right", content.right_text[0].getText());
    assertEquals("bottom", content.bottom_text[0].getText());
    String serialized = GSON.toJson(content);
    assertTrue(serialized.contains("right_text"));
    assertTrue(serialized.contains("bottom_text"));
  }

  @Test
  void readsAndWritesLegacyTextRightImageFields() {
    ContentTextRightImage content = GSON.fromJson("""
      {"text1":[{"text":"left"}],"text2":[{"text":"bottom"}]}
      """, ContentTextRightImage.class);

    assertEquals("left", content.left_text[0].getText());
    assertEquals("bottom", content.bottom_text[0].getText());
    String serialized = GSON.toJson(content);
    assertTrue(serialized.contains("left_text"));
    assertTrue(serialized.contains("bottom_text"));
  }
}
