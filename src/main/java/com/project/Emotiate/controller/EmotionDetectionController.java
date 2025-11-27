package com.project.Emotiate.controller;

import com.project.Emotiate.dto.emotion.EmotionResultDto;
import com.project.Emotiate.generics.Response;
import com.project.Emotiate.module.EmotionDetectionModule;
import com.project.Emotiate.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/emotion")
@RequiredArgsConstructor
public class EmotionDetectionController {

    private final EmotionDetectionModule emotionDetectionModule;

    @PostMapping("/detect")
    public ResponseEntity<Response<EmotionResultDto>> detectEmotion(@RequestBody Map<String,String> request) {

        log.info("Emotion detection request received: {}", request.get("userMessage"));
        EmotionResultDto result = emotionDetectionModule.detectEmotion(request.get("userMessage"));
        return ResponseUtil.success(result, "Emotion detected successfully");
    }
}