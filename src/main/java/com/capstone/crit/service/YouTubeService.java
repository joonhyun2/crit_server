package com.capstone.crit.service;

import com.capstone.crit.dto.VideoAnalysisResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class YouTubeService {

    private final RestTemplate restTemplate;
    private final VideoScoreService videoScoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${youtube.api.key}")
    private String apiKey;

    private static final String VIDEO_API_URL = "https://www.googleapis.com/youtube/v3/videos";
    private static final String CHANNEL_API_URL = "https://www.googleapis.com/youtube/v3/channels";

    public VideoAnalysisResponse analyze(String url) {
        String videoId = extractVideoId(url);

        // 영상 정보 조회
        String videoUrl = UriComponentsBuilder.fromUriString(VIDEO_API_URL)
                .queryParam("id", videoId)
                .queryParam("part", "snippet,statistics")
                .queryParam("key", apiKey)
                .toUriString();

        String videoResponse = restTemplate.getForObject(videoUrl, String.class);

        try {
            JsonNode item = objectMapper.readTree(videoResponse).path("items").get(0);
            JsonNode snippet = item.path("snippet");
            JsonNode stats = item.path("statistics");

            String channelId = snippet.path("channelId").asText();
            long views = stats.path("viewCount").asLong(0);
            long likes = stats.path("likeCount").asLong(0);
            long comments = stats.path("commentCount").asLong(0);
            String publishedAt = snippet.path("publishedAt").asText();

            long subscribers = getSubscriberCount(channelId);

            return videoScoreService.calculate(VideoAnalysisResponse.builder()
                    .videoId(videoId)
                    .title(snippet.path("title").asText())
                    .viewCount(views)
                    .likeCount(likes)
                    .commentCount(comments)
                    .subscriberCount(subscribers)
                    .publishedAt(publishedAt)
                    .build());

        } catch (Exception e) {
            throw new RuntimeException("YouTube API 파싱 실패: " + e.getMessage(), e);
        }
    }

    private long getSubscriberCount(String channelId) {
        try {
            String channelUrl = UriComponentsBuilder.fromUriString(CHANNEL_API_URL)
                    .queryParam("id", channelId)
                    .queryParam("part", "statistics")
                    .queryParam("key", apiKey)
                    .toUriString();

            String response = restTemplate.getForObject(channelUrl, String.class);
            JsonNode item = objectMapper.readTree(response).path("items").get(0);
            return item.path("statistics").path("subscriberCount").asLong(1);
        } catch (Exception e) {
            return 1; // 0 나누기 방지
        }
    }

    private String extractVideoId(String url) {
        if (url.contains("youtu.be/")) {
            return url.substring(url.lastIndexOf("/") + 1).split("\\?")[0];
        }
        if (url.contains("v=")) {
            return url.split("v=")[1].split("&")[0];
        }
        throw new IllegalArgumentException("유효하지 않은 YouTube URL: " + url);
    }
}
