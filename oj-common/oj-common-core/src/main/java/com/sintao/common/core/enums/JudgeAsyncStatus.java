package com.sintao.common.core.enums;

import lombok.Getter;

@Getter
public enum JudgeAsyncStatus {

    WAITING(0),

    SUCCESS(1),

    DEAD_LETTER(2),

    DISPATCH_FAILED(3);

    private final Integer value;

    JudgeAsyncStatus(Integer value) {
        this.value = value;
    }
}
