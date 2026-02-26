package com.sky.mapper;

import com.sky.entity.SetmealDish;
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

    /**
     * 向setmeal_dish表批量插入数据
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);
}
