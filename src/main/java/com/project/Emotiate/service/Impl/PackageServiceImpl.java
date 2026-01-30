package com.project.Emotiate.service.Impl;

import com.project.Emotiate.dto.packages.PackageAddRequestDto;
import com.project.Emotiate.dto.packages.PackageResponseDto;
import com.project.Emotiate.dto.packages.PackageUpdateRequestDto;
import com.project.Emotiate.entity.HotelPackage;
import com.project.Emotiate.entity.Room;
import com.project.Emotiate.enums.PackageAddOn;
import com.project.Emotiate.enums.RoomType;
import com.project.Emotiate.exception.CustomException;
import com.project.Emotiate.repository.HotelPackageRepository;
import com.project.Emotiate.repository.RoomRepository;
import com.project.Emotiate.service.PackageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class PackageServiceImpl implements PackageService {

    private final HotelPackageRepository packageRepository;
    private final RoomRepository roomRepository;

    @Override
    // Retrieve packages
    public List<PackageResponseDto> getPackages(Boolean isActive, String roomType) {

        RoomType type = roomType != null
                ? RoomType.valueOf(roomType.toUpperCase(Locale.ROOT))
                : null;

        List<HotelPackage> packages;

        if (isActive != null && type != null) {
            packages = packageRepository.findByIsActiveAndRoomType(isActive, type);

        } else if (isActive != null) {
            packages = packageRepository.findByIsActive(isActive);

        } else if (type != null) {
            packages = packageRepository.findByRoomType(type);

        } else {
            packages = packageRepository.findAll();
        }

        return packages.stream()
                .map(this::toResponseDto)
                .toList();
    }


    @Override
    // Fetch a single package by id
    public PackageResponseDto getPackageById(Long id) {

        // Find the package or throw if it does not exist
        HotelPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Package with Id " + id + " not found",
                        HttpStatus.NOT_FOUND.value()));

        return toResponseDto(pkg);
    }


    @Override
    // Add a new package
    public PackageResponseDto addPackage(PackageAddRequestDto request) {

        // Validate price bounds
        if (request.getUpperBoundPrice().compareTo(request.getLowerBoundPrice()) < 0) {
            throw new CustomException(
                    "Upper bound price must be greater than or equal to lower bound price",
                    HttpStatus.BAD_REQUEST.value());
        }

        // Fetch the room this package is built around
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new CustomException(
                        "Room with Id " + request.getRoomId() + " not found",
                        HttpStatus.NOT_FOUND.value()));

        // Validate room status
        if (!room.getIsActive()) {
            throw new CustomException(
                    "Cannot create a package for an inactive room",
                    HttpStatus.BAD_REQUEST.value());
        }

        // Build the package entity, and persist
        HotelPackage pkg = HotelPackage.builder()
                .name(request.getName())
                .description(request.getDescription())
                .room(room)
                .lowerBoundPrice(request.getLowerBoundPrice())
                .upperBoundPrice(request.getUpperBoundPrice())
                .maxOccupancy(request.getMaxOccupancy() != null
                        ? request.getMaxOccupancy()
                        : room.getMaxOccupancy())
                .addOns(resolveAddOns(request.getAddOns()))
                .imageUrl(request.getImageUrl())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        packageRepository.save(pkg);
        log.info("Package added  name={} room={} lower={} upper={}",
                pkg.getName(), room.getRoomNumber(),
                pkg.getLowerBoundPrice(), pkg.getUpperBoundPrice());

        return toResponseDto(pkg);
    }


    @Override
    // Edit an existing package
    public PackageResponseDto editPackage(Long id, PackageUpdateRequestDto request) {

        // Find the package
        HotelPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Package with Id " + id + " not found",
                        HttpStatus.NOT_FOUND.value()));

        // Validate bounds if either price is being updated
        if (request.getLowerBoundPrice() != null || request.getUpperBoundPrice() != null) {

            // Use incoming value if provided
            var lower = request.getLowerBoundPrice() != null
                    ? request.getLowerBoundPrice() : pkg.getLowerBoundPrice();
            var upper = request.getUpperBoundPrice() != null
                    ? request.getUpperBoundPrice() : pkg.getUpperBoundPrice();

            if (upper.compareTo(lower) < 0) {
                throw new CustomException(
                        "Upper bound price must be greater than or equal to lower bound price",
                        HttpStatus.BAD_REQUEST.value());
            }
        }

        // Update room if a new roomId was provided
        if (request.getRoomId() != null && !request.getRoomId().equals(pkg.getRoom().getId())) {
            Room newRoom = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new CustomException(
                            "Room with Id " + request.getRoomId() + " not found",
                            HttpStatus.NOT_FOUND.value()));

            if (!newRoom.getIsActive()) {
                throw new CustomException(
                        "Cannot assign an inactive room to a package",
                        HttpStatus.BAD_REQUEST.value());
                }
            pkg.setRoom(newRoom);
        }

        // Apply only the fields that were provided
        if (request.getName()            != null) pkg.setName(request.getName());
        if (request.getDescription()     != null) pkg.setDescription(request.getDescription());
        if (request.getLowerBoundPrice() != null) pkg.setLowerBoundPrice(request.getLowerBoundPrice());
        if (request.getUpperBoundPrice() != null) pkg.setUpperBoundPrice(request.getUpperBoundPrice());
        if (request.getMaxOccupancy()    != null) pkg.setMaxOccupancy(request.getMaxOccupancy());
        if (request.getImageUrl()        != null) pkg.setImageUrl(request.getImageUrl());
        if (request.getIsActive()        != null) pkg.setIsActive(request.getIsActive());

        if (request.getAddOns() != null) {
            pkg.setAddOns(resolveAddOns(request.getAddOns()));
        }

        pkg.setUpdatedAt(LocalDateTime.now());
        packageRepository.save(pkg);
        log.info("Package updated  id={}", id);

        return toResponseDto(pkg);
    }


    @Override
    // Soft delete a package
    public Void removePackage(Long id) {

        // Find the package
        HotelPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Package with Id " + id + " not found",
                        HttpStatus.NOT_FOUND.value()));

        if (!pkg.getIsActive()) {
            throw new CustomException(
                    "Package with Id " + id + " is already inactive",
                    HttpStatus.BAD_REQUEST.value());
        }

        pkg.setIsActive(false);
        pkg.setUpdatedAt(LocalDateTime.now());
        packageRepository.save(pkg);
        log.info("Package soft-deleted  id={}", id);

        return null;
    }


    @Override
    // Retrieve active packages that satisfy requested add-ons and guest capacity
    public List<PackageResponseDto> getPackagesByRequirements(List<String> addOns, Integer guestCount) {

        // Resolve requested add-ons to enum values
        List<PackageAddOn> requiredAddOns = (addOns != null && !addOns.isEmpty())
                ? resolveAddOns(addOns)
                : List.of();

        List<HotelPackage> packages = packageRepository.findByIsActive(true);

        // Filter by guest capacity if provided
        if (guestCount != null) {
            packages = packages.stream()
                    .filter(p -> Objects.equals(p.getMaxOccupancy(), guestCount))
                    .toList();
        }

        // Filter to packages that contain every requested add-on
        if (!requiredAddOns.isEmpty()) {
            packages = packages.stream()
                    .filter(p -> new HashSet<>(p.getAddOns()).containsAll(requiredAddOns))
                    .toList();
        }

        log.info("Package requirement query  addOns={} guestCount={} matched={}",
                addOns, guestCount, packages.size());

        return packages.stream()
                .map(this::toResponseDto)
                .toList();
    }


    // Method to resolve AddOns options in package
    private List<PackageAddOn> resolveAddOns(List<String> addOnStrings) {
        if (addOnStrings == null) return new ArrayList<>();
        return addOnStrings.stream()
                .map(s -> {
                    try {
                        return PackageAddOn.valueOf(s.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        throw new CustomException(
                                "Invalid add-on: " + s, HttpStatus.BAD_REQUEST.value());
                    }
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }


    // Mapper to map Package entities to PackageResponseDto
    private PackageResponseDto toResponseDto(HotelPackage pkg) {
        return PackageResponseDto.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .roomId(pkg.getRoom().getId())
                .roomNumber(pkg.getRoom().getRoomNumber())
                .roomType(pkg.getRoom().getRoomType().name())
                .lowerBoundPrice(pkg.getLowerBoundPrice())
                .upperBoundPrice(pkg.getUpperBoundPrice())
                .maxOccupancy(pkg.getMaxOccupancy())
                .addOns(pkg.getAddOns().stream().map(Enum::name).toList())
                .imageUrl(pkg.getImageUrl())
                .isActive(pkg.getIsActive())
                .build();
    }
}