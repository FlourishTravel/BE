package com.flourishtravel.domain.flora.dto;

import lombok.Data;

import java.util.List;

@Data
public class FloraPreferencePreviewRequest {

    private List<String> selectedTagIds;
}
