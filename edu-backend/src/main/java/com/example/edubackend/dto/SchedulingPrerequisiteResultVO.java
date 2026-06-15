package com.example.edubackend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SchedulingPrerequisiteResultVO {
    private Boolean canSchedule;
    private List<String> blockedReasons = new ArrayList<>();

    public static SchedulingPrerequisiteResultVO passed() {
        SchedulingPrerequisiteResultVO vo = new SchedulingPrerequisiteResultVO();
        vo.setCanSchedule(true);
        return vo;
    }

    public static SchedulingPrerequisiteResultVO blocked(List<String> reasons) {
        SchedulingPrerequisiteResultVO vo = new SchedulingPrerequisiteResultVO();
        vo.setCanSchedule(false);
        vo.setBlockedReasons(reasons);
        return vo;
    }
}
