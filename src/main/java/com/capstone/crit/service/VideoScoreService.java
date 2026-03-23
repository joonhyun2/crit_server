package com.capstone.crit.service;

import com.capstone.crit.dto.VideoAnalysisResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class VideoScoreService {

    public VideoAnalysisResponse calculate(VideoAnalysisResponse data) {
        long views = data.getViewCount();
        long likes = data.getLikeCount();
        long comments = data.getCommentCount();
        long subscribers = data.getSubscriberCount();

        double viewsPerSubscriber = subscribers > 0 ? (double) views / subscribers : 0;
        double engagement = views > 0 ? (double) (likes + comments) / views : 0;
        double viewsPerHour = calcViewsPerHour(views, data.getPublishedAt());
        double likeRatio = views > 0 ? (double) likes / views : 0;
        double commentRatio = views > 0 ? (double) comments / views : 0;

        double totalScore = (normalize(viewsPerSubscriber, 0.5) * 0.3
                + normalize(engagement, 0.05) * 0.25
                + normalize(viewsPerHour, 5_000) * 0.2
                + normalize(likeRatio, 0.05) * 0.15
                + normalize(commentRatio, 0.005) * 0.1) * 100;

        return VideoAnalysisResponse.builder()
                .videoId(data.getVideoId())
                .title(data.getTitle())
                .viewCount(views)
                .likeCount(likes)
                .commentCount(comments)
                .subscriberCount(subscribers)
                .publishedAt(data.getPublishedAt())
                .viewsPerSubscriber(viewsPerSubscriber)
                .engagement(engagement)
                .viewsPerHour(viewsPerHour)
                .likeRatio(likeRatio)
                .commentRatio(commentRatio)
                .totalScore(totalScore)
                .build();
    }

    // 0~1로 정규화 (상한값 초과 시 1로 클램프)
    private double normalize(double value, double max) {
        return Math.min(value / max, 1.0);
    }

    private double calcViewsPerHour(long views, String publishedAt) {
        try {
            OffsetDateTime published = OffsetDateTime.parse(publishedAt);
            long hours = Duration.between(published, OffsetDateTime.now()).toHours();
            long effectiveHours = Math.min(hours, 720); // 최대 30일(720시간)로 고정
            return effectiveHours > 0 ? (double) views / effectiveHours : views;
        } catch (Exception e) {
            return 0;
        }
    }
}
