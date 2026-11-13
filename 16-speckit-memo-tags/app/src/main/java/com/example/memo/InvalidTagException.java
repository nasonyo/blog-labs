package com.example.memo;

/** 태그 입력이 규칙에 맞지 않을 때 던진다. field 는 문제가 된 입력의 위치(예: tags[1], tags, tag)다. */
public class InvalidTagException extends RuntimeException {

    private final String field;

    public InvalidTagException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
