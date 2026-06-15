package com.example.edubackend.mapper;

import com.example.edubackend.entity.QuestionBank;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 智能题库表 Mapper 接口
 * </p>
 *
 * @author auto-generator
 * @since 2026-03-19
 */
@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {

    QuestionBank selectByIdIncludingDeleted(@Param("id") Long id);

    List<QuestionBank> selectBatchIdsIncludingDeleted(@Param("ids") Collection<Long> ids);
}
