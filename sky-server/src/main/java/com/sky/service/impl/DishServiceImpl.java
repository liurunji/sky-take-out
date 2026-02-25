package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        //拷贝属性
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //调用mapper向dish表插入数据
        dishMapper.insert(dish);
        //数据库执行完插入语句后将生成的主键值返回 在这里进行接收
        Long dishId = dish.getId();
        //取出口味集合
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            //遍历flavors，为dishId赋值
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }
            //调用mapper批量插入口味数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        //设置分页参数
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        //调用mapper
        List<DishVO> list = dishMapper.pageQuery(dishPageQueryDTO);
        Page p = (Page) list;
        PageResult pageResult = new PageResult(p.getTotal(),p.getResult());
        return pageResult;
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断能否删除菜品--菜品是否处于起售状态？
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                //不能删抛异常
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断能否删除菜品--菜品是否关联在某个套餐下？
        List<Long> setmealIds =setmealDishMapper.getByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0){
            //菜品关联在某个套餐下,不能删抛异常
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        /*
        //删除菜品信息
        for (Long id : ids) {
            dishMapper.deleteById(id);
            //删除口味信息
            dishFlavorMapper.deleteByDishId(id);
        }
         */
        //删除菜品信息
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);

    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //获取菜品信息封装到dish里
        Dish dish = dishMapper.getById(id);
        //拷贝给dishvo
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        //获取口味信息
        List<DishFlavor> dishFlavorList = dishFlavorMapper.getByDishId(id);
        dishVO.setFlavors(dishFlavorList);
        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        //创建dish对象并把dishdto里的属性值赋给他
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);
        //删除对应口味信息
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //新增口味信息
        //要先遍历并给dishId赋值
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            //遍历flavors，为dishId赋值
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishDTO.getId());
            }
            //调用mapper批量插入口味数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 菜品起售、停售
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);
        dishMapper.update(dish);
    }

    /**
     *根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> getByCategoryId(Integer categoryId) {
        return dishMapper.getByCategoryId(categoryId);
    }
}
