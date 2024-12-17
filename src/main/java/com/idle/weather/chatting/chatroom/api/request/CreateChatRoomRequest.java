package com.idle.weather.chatting.chatroom.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateChatRoomRequest(
        @Schema(description = "기상특보 지역 코드", example = "S1250000")
        String parentRegionCode,

        @Schema(description = "기상특보 지역 이름", example = "서해중부전해상")
        String parentRegionName
) {
}
