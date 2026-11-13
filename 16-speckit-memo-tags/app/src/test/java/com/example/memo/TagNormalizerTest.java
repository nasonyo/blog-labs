package com.example.memo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TagNormalizerTest {

    @Test
    void trimsLowercasesAndRemovesDuplicates() {
        assertThat(TagNormalizer.normalize(List.of(" Meeting ", "MEETING", "회의")))
                .containsExactly("meeting", "회의");
    }

    @Test
    void nullOrEmptyMeansNoTags() {
        assertThat(TagNormalizer.normalize(null)).isEmpty();
        assertThat(TagNormalizer.normalize(List.of())).isEmpty();
    }

    @Test
    void blankTagIsRejectedWithItsIndex() {
        assertThatThrownBy(() -> TagNormalizer.normalize(List.of("ok", "   ")))
                .isInstanceOf(InvalidTagException.class)
                .extracting("field").isEqualTo("tags[1]");
    }

    @Test
    void emptyStringIsRejected() {
        assertThatThrownBy(() -> TagNormalizer.normalize(List.of("")))
                .isInstanceOf(InvalidTagException.class)
                .extracting("field").isEqualTo("tags[0]");
    }

    @Test
    void thirtyCharactersAreAllowedButThirtyOneAreNot() {
        assertThat(TagNormalizer.normalize(List.of("a".repeat(30)))).hasSize(1);
        assertThatThrownBy(() -> TagNormalizer.normalize(List.of("a".repeat(31))))
                .isInstanceOf(InvalidTagException.class)
                .extracting("field").isEqualTo("tags[0]");
    }

    @Test
    void tenDistinctTagsAreAllowedButElevenAreNot() {
        List<String> ten = IntStream.rangeClosed(1, 10).mapToObj(i -> "t" + i).toList();
        List<String> eleven = IntStream.rangeClosed(1, 11).mapToObj(i -> "t" + i).toList();
        assertThat(TagNormalizer.normalize(ten)).hasSize(10);
        assertThatThrownBy(() -> TagNormalizer.normalize(eleven))
                .isInstanceOf(InvalidTagException.class)
                .extracting("field").isEqualTo("tags");
    }

    @Test
    void limitIsCountedAfterRemovingDuplicates() {
        List<String> elevenWithDuplicates =
                List.of("a", "A", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        assertThat(TagNormalizer.normalize(elevenWithDuplicates)).hasSize(10);
    }

    @Test
    void filterValueUsesTheSameRules() {
        assertThat(TagNormalizer.normalizeOne(" Meeting ")).isEqualTo("meeting");
        assertThatThrownBy(() -> TagNormalizer.normalizeOne("  "))
                .isInstanceOf(InvalidTagException.class)
                .extracting("field").isEqualTo("tag");
    }
}
