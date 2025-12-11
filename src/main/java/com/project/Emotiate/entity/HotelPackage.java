package com.project.Emotiate.entity;

import com.project.Emotiate.enums.PackageAddOn;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "hotel_packages")
public class HotelPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // Extras included in the package
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "package_addons",
            joinColumns = @JoinColumn(name = "package_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "addon")
    @Builder.Default
    private List<PackageAddOn> addOns = new ArrayList<>();

    @Column(name = "upper_bound_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal upperBoundPrice;

    @Column(name = "lower_bound_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal lowerBoundPrice;

    @Column(name = "max_occupancy", nullable = false)
    private Integer maxOccupancy;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}