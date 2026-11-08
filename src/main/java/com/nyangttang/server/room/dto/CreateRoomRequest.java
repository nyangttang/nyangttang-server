package com.nyangttang.server.room.dto;

import com.nyangttang.server.room.entity.CatColor;
import com.nyangttang.server.room.entity.RoomMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "방 이름을 입력해주세요.")
        @Size(max = 30, message = "방 이름은 30자 이하여야 합니다.")
        String name,

        @NotNull(message = "게임 모드를 선택해주세요.")
        RoomMode mode,

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
    public CreateRoomRequest {
        if (name != null) {
            name = name.strip();
        }

        if (catName != null) {
            catName = catName.strip();
        }
    }
}