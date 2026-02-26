package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐
     *
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        //拷贝到setmeal对象
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //调用mapper向setmeal表插入数据
        setmealMapper.insert(setmeal);
        //主键返回
        Long setmealId = setmeal.getId();
        //获取套餐菜品信息
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //遍历套餐菜品集合setmealId设置为当前返回的主键
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealId);
        }
        //调用mapper向setmeal_dish表批量插入数据
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        //设置分页参数
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        //调用mapper
        List<SetmealVO> list = setmealMapper.pageQuery(setmealPageQueryDTO);
        //强转成Page类型
        Page p = (Page) list;
        //封装成pageresult返回
        PageResult pageResult = new PageResult(p.getTotal(), p.getResult());
        return pageResult;
    }

    /**
     * 批量删除套餐
     *
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断能否删除套餐--处于起售状态的套餐无法直接删除
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                //起售状态的套餐无法直接删除,抛出异常
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //批量删除setmeal表的套餐信息
        setmealMapper.deleteByIds(ids);
        //还要删除对应的setmeal_dish表的信息
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 根据id查询套餐
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        //根据ID查询套餐信息封装到setmeal对象里
        Setmeal setmeal = setmealMapper.getById(id);
        //拷贝到setmealVO里
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        //调用mapper：根据setmealId查询套餐菜品关系表
        List<SetmealDish> list = setmealDishMapper.getBySetmealId(id);
        //把list赋值到setmealVO里
        setmealVO.setSetmealDishes(list);
        return setmealVO;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {
        //先更新setmeal里的数据
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);
        //删除setmeal_dish表数据
        List<Long> setmealId = Arrays.asList(setmealDTO.getId());
        System.out.println(setmealId);
        setmealDishMapper.deleteBySetmealIds(setmealId);
        //向setmeal_dish表插入新数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //传过来的setmealDish不为空再进行赋值和更新
        if (setmealDishes != null && setmealDishes.size() > 0) {
            //遍历套餐菜品表给SetmealId赋值当前套餐的id
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmealDTO.getId());
            }
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }
}
