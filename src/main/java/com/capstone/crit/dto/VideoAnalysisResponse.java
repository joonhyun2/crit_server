package com.capstone.crit.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoAnalysisResponse {
    private String videoId;
    private String title;
    private long viewCount;
    private long likeCount;
    private long commentCount;
    private long subscriberCount;
    private String publishedAt;

    // 세부 지표
    private double viewsPerSubscriber;
    private double engagement;
    private double viewsPerHour;
    private double likeRatio;
    private double commentRatio;

    // 총점
    private double totalScore;
}
