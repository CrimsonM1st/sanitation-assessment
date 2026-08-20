package com.example.sanitationassessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.sanitationassessment.entity.SystemUserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemUserMapper
        extends BaseMapper<SystemUserEntity> {
}
