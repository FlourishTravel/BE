package com.flourishtravel.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReactionSummaryDto {

    private String type;
    private long count;
    @JsonProperty("reactedByMe")
    private boolean reactedByMe;
}
