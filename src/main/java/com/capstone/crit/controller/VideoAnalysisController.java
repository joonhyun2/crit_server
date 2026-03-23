package com.capstone.crit.controller;

import com.capstone.crit.dto.VideoAnalysisRequest;
import com.capstone.crit.dto.VideoAnalysisResponse;
import com.capstone.crit.service.YouTubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoAnalysisController {

    private final YouTubeService youTubeService;

    @PostMapping("/analyze")
    public ResponseEntity<VideoAnalysisResponse> analyze(@RequestBody VideoAnalysisRequest request) {
        return ResponseEntity.ok(youTubeService.analyze(request.getUrl()));
    }
}
