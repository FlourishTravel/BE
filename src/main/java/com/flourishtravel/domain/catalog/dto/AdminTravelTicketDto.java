package com.flourishtravel.domain.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AdminTravelTicketDto {
    private UUID id;
    private String slug;
    private String name;
    private String category;
    private String destinationCity;
    private String description;
    private String shortDescription;
    private String imageUrl;
    private BigDecimal priceVnd;
    private String priceLabel;
    private BigDecimal rating;
    private String showTimeLabel;
    private String locationLabel;
    private String routeLabel;
    private Boolean eTicket;
    private Boolean featured;
    private Boolean published;
    private Integer sortOrder;
}
