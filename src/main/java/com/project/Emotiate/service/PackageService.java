package com.project.Emotiate.service;

import com.project.Emotiate.dto.packages.PackageAddRequestDto;
import com.project.Emotiate.dto.packages.PackageResponseDto;
import com.project.Emotiate.dto.packages.PackageUpdateRequestDto;

import java.util.List;

public interface PackageService {

    // Retrieve packages
    List<PackageResponseDto> getPackages(Boolean isActive, String roomType);

    // Fetch a single package by id
    PackageResponseDto getPackageById(Long id);

    // Add a new package
    PackageResponseDto addPackage(PackageAddRequestDto request);

    // Edit an existing package
    PackageResponseDto editPackage(Long id, PackageUpdateRequestDto request);

    // Soft delete a package
    Void removePackage(Long id);

    // Retrieve active packages matching required add-ons and guest count
    List<PackageResponseDto> getPackagesByRequirements(List<String> addOns, Integer guestCount);
}