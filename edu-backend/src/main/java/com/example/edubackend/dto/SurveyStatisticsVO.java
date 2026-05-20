package com.example.edubackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SurveyStatisticsVO {
    
    private Long surveyId;
    private String title;
    private Integer totalCount;
    private NpsVO nps;
    private List<RadarDataVO> radarData;
    private List<QuestionStatisticsVO> questions;
    
    @Data
    public static class NpsVO {
        private BigDecimal score;
        private BigDecimal promoterRate;
        private BigDecimal detractorRate;
    }
    
    @Data
    public static class RadarDataVO {
        private String questionTitle;
        private BigDecimal avgScore;
    }
    
    @Data
    public static class QuestionStatisticsVO {
        private Long questionId;
        private String title;
        private String type;
        private BigDecimal avgScore;
        private List<DistributionVO> distribution;
        private List<String> textAnswers;
    }
    
    @Data
    public static class DistributionVO {
        private Integer value;
        private Integer count;
        private BigDecimal rate;
    }
}