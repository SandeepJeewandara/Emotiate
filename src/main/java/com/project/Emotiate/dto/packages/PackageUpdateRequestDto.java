package com.project.Emotiate.dto.packages;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackageUpdateRequestDto {

    private String     name;
    private String     description;
    private Long       roomId;
    private BigDecimal lowerBoundPrice;
    private BigDecimal upperBoundPrice;
    private Integer    maxOccupancy;
    private List<String> addOns;
    private String     imageUrl;
    private Boolean    isActive;
}