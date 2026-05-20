package com.example.edubackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.edubackend.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}
