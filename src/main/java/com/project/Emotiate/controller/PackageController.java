package com.project.Emotiate.controller;

import com.project.Emotiate.dto.packages.PackageAddRequestDto;
import com.project.Emotiate.dto.packages.PackageResponseDto;
import com.project.Emotiate.dto.packages.PackageUpdateRequestDto;
import com.project.Emotiate.generics.Response;
import com.project.Emotiate.service.PackageService;
import com.project.Emotiate.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/package")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping("/get")
    public ResponseEntity<Response<List<PackageResponseDto>>> getPackages(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String  roomType) {

        log.info("Retrieving packages — isActive={} roomType={}", isActive, roomType);
        return ResponseUtil.success(packageService.getPackages(isActive, roomType), "Packages retrieved successfully");
    }


    @GetMapping("/get/{id}")
    public ResponseEntity<Response<PackageResponseDto>> getPackageById(@PathVariable Long id) {

        log.info("Retrieving package — id={}", id);
        return ResponseUtil.success(packageService.getPackageById(id), "Package retrieved successfully");
    }


    @PostMapping("/add")
    public ResponseEntity<Response<PackageResponseDto>> addPackage(@RequestBody PackageAddRequestDto request) {

        log.info("Adding package — name={} roomId={}", request.getName(), request.getRoomId());
        return ResponseUtil.created(packageService.addPackage(request), "Package added successfully");
    }


    @PutMapping("/edit/{id}")
    public ResponseEntity<Response<PackageResponseDto>> editPackage(
            @PathVariable Long id,
            @RequestBody PackageUpdateRequestDto request) {

        log.info("Editing package — id={}", id);
        return ResponseUtil.success(packageService.editPackage(id, request), "Package updated successfully");
    }


    @DeleteMapping("/remove/{id}")
    public ResponseEntity<Response<Void>> removePackage(@PathVariable Long id) {

        log.info("Removing package — id={}", id);
        return ResponseUtil.success(packageService.removePackage(id), "Package removed successfully");
    }
}