package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据dishId在setmeal_dish表里查询
     * @param dishIds
     * @return
     */
    List<Long> getByDishIds(List<Long> dishIds);
}
