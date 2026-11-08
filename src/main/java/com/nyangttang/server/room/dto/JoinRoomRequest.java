package com.nyangttang.server.room.dto;

import com.nyangttang.server.room.entity.CatColor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record JoinRoomRequest(
        @NotBlank(message = "초대 코드를 입력해주세요.")
        @Pattern(
                regexp = "[A-Z0-9]{6}",
                message = "초대 코드는 영문·숫자 6자리입니다."
        )
        String inviteCode,

        @NotBlank(message = "고양이 이름을 입력해주세요.")
        @Size(max = 12, message = "고양이 이름은 12자 이하여야 합니다.")
        String catName,

        @NotNull(message = "고양이 색을 선택해주세요.")
        CatColor catColor,

        @NotNull(message = "목표 시간을 입력해주세요.")
        @Min(value = 30, message = "목표 시간은 최소 30초입니다.")
        @Max(value = 43200, message = "목표 시간은 최대 12시간입니다.")
        Integer dailyGoalSeconds
) {
    public JoinRoomRequest {
        if (inviteCode != null) {
            inviteCode = inviteCode.strip().toUpperCase(Locale.ROOT);
        }

        if (catName != null) {
            catName = catName.strip();
        }
    }
}