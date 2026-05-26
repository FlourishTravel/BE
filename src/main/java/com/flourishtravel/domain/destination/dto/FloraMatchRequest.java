package com.flourishtravel.domain.destination.dto;

import lombok.Data;

import java.util.List;

@Data
public class FloraMatchRequest {
    private List<String> preferences;
    private String destinationSlug;
}
