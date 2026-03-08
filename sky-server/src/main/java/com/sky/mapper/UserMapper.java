package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 新增用户
     * @param user
     */
    void insert(User user);

    /**
     * 通过id查找用户
     * @param userId
     * @return
     */
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    /**
     * 统计用户数量
     * @param map
     * @return
     */
    Integer getSumByMap(Map map);

    /**
     * 统计用户数量
     * 和上面的功能是一样的，只是适配导入提供的代码
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
