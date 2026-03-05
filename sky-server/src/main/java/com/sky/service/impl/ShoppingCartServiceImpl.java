package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     *
     * @param shoppingCartDTO
     * @return
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //查询数据库有没有对应的菜品或者套餐
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //设置用户id，要确定是当前用户的购物车
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        //如果有，那么需要把数量+1
        if (list != null && list.size() > 0) {
            //注意这里list有值的话里面应该只有一条数据
            shoppingCart = list.get(0);
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(shoppingCart);
        } else {
            //如果没有，那么就新增一条数据
            Long dishId = shoppingCart.getDishId();
            if (dishId != null) {
                //传过来的是菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());

            }else {
                //传过来的是套餐
                Long setmealId = shoppingCart.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setImage(setmeal.getImage());
            }
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setNumber(1);
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        //根据当前用户id查询购物车表并返回
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }
}
