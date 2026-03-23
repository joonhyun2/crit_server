package com.capstone.crit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;
import java.util.Map;

@Service
public class BedrockService {

    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.bedrock.model-id:anthropic.claude-3-5-sonnet-20241022-v2:0}")
    private String modelId;

    public BedrockService(@Value("${aws.region:us-east-1}") String region) {
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String analyzeTrend(String category, String keywords) {
        String prompt = buildTrendPrompt(category, keywords);
        return invokeModel(prompt);
    }

    public String analyzeVideo(VideoAnalysisData data) {
        String prompt = String.format(
            "당신은 YouTube 알고리즘 전문가입니다. 아래 영상 데이터를 분석해주세요.\n\n" +
            "제목: %s\n" +
            "조회수: %,d\n" +
            "좋아요: %,d\n" +
            "댓글: %,d\n" +
            "구독자: %,d\n" +
            "알고리즘 총점: %.1f / 100\n\n" +
            "지표별 수치:\n" +
            "- 구독자 대비 조회율(views_per_subscriber): %.3f (가중치 30%%)\n" +
            "- 참여율(engagement): %.3f (가중치 25%%)\n" +
            "- 시간당 조회수(views_per_hour): %.1f (가중치 20%%)\n" +
            "- 좋아요 비율(like_ratio): %.3f (가중치 15%%)\n" +
            "- 댓글 비율(comment_ratio): %.4f (가중치 10%%)\n\n" +
            "다음 항목을 한국어로 분석해주세요:\n" +
            "1. 각 지표별 평가 (강점/약점)\n" +
            "2. 알고리즘 점수 총평\n" +
            "3. 개선을 위한 구체적인 제안 3가지",
            data.title(), data.viewCount(), data.likeCount(), data.commentCount(), data.subscriberCount(),
            data.totalScore(), data.viewsPerSubscriber(), data.engagement(),
            data.viewsPerHour(), data.likeRatio(), data.commentRatio()
        );
        return invokeModel(prompt);
    }

    public record VideoAnalysisData(
        String title, long viewCount, long likeCount, long commentCount, long subscriberCount,
        double totalScore, double viewsPerSubscriber, double engagement,
        double viewsPerHour, double likeRatio, double commentRatio
    ) {}

    private String buildTrendPrompt(String category, String keywords) {
        return String.format(
            "당신은 YouTube 콘텐츠 전략 전문가입니다.\n" +
            "카테고리: %s\n" +
            "키워드: %s\n\n" +
            "위 정보를 바탕으로 다음을 제공해주세요:\n" +
            "1. 현재 트렌드 분석 (3가지)\n" +
            "2. 추천 콘텐츠 주제 (5가지, 제목 포함)\n" +
            "3. 각 주제의 예상 타겟 시청자\n" +
            "한국어로 답변해주세요.",
            category, keywords != null ? keywords : "없음"
        );
    }

    private String invokeModel(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 1024,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                )
            );

            String bodyJson = objectMapper.writeValueAsString(body);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(bodyJson))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            Map<?, ?> responseBody = objectMapper.readValue(
                response.body().asUtf8String(), Map.class
            );

            // Claude 응답 파싱
            List<?> content = (List<?>) responseBody.get("content");
            Map<?, ?> firstContent = (Map<?, ?>) content.get(0);
            return (String) firstContent.get("text");

        } catch (Exception e) {
            throw new RuntimeException("Bedrock 호출 실패: " + e.getMessage(), e);
        }
    }
}
