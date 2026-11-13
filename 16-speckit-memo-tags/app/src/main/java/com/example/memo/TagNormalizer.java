package com.example.memo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 태그 정규화와 검증 규칙 (spec FR-004, 008, 009, 010). */
public final class TagNormalizer {

    public static final int MAX_TAGS = 10;
    public static final int MAX_LENGTH = 30;

    private TagNormalizer() {}

    /** 요청의 tags 를 정규화한다. null 은 태그 없음이다. 중복은 처음 나온 것만 남긴다. */
    public static List<String> normalize(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i < raw.size(); i++) {
            result.add(normalizeValue(raw.get(i), "tags[" + i + "]"));
        }
        if (result.size() > MAX_TAGS) {
            throw new InvalidTagException("tags", "태그는 최대 " + MAX_TAGS + "개까지 붙일 수 있습니다");
        }
        return new ArrayList<>(result);
    }

    /** 조회용 tag 파라미터 하나를 같은 규칙으로 정규화한다. */
    public static String normalizeOne(String raw) {
        return normalizeValue(raw, "tag");
    }

    private static String normalizeValue(String raw, String field) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new InvalidTagException(field, "태그는 비어 있을 수 없습니다");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidTagException(field, "태그는 최대 " + MAX_LENGTH + "자까지 가능합니다");
        }
        return value;
    }
}
