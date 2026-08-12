package com.example.sanitationassessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.sanitationassessment.entity.AssessmentTaskAuditLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AssessmentTaskAuditLogMapper
        extends BaseMapper<AssessmentTaskAuditLogEntity> {
}
