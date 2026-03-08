package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //先封装日期dateList
        List<LocalDate> localDateList = new ArrayList<>();
        //由于“昨天”的数据传进来的begin和end是同一天，会出现bug
        // 修正：包含起始日期
        LocalDate current = begin;
        while (!current.isAfter(end)) {  // 使用 isAfter 而不是 equals
            localDateList.add(current);
            current = current.plusDays(1);
        }
        // 即使 begin 和 end 是同一天，也会执行一次  ， localDateList 包含 [begin]
        //把集合中的数据用"，"拼接起来 组成一个字符串返回给前端
        String dateList = StringUtils.join(localDateList, ",");

        //封装日期对应的营业额
        List<Double> doubleList = new ArrayList<>();
        for (LocalDate localDate : localDateList) {
            //获取这一天开始时间（00:00:00）和结束时间（23:59:59）
            LocalDateTime dayBeginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime dayEndTime = LocalDateTime.of(localDate, LocalTime.MAX);
            //调用mapper查询数据库：查询当天已完成订单的总金额
            //SQL：select sum(amount) from order where status = 5 and order_time between ? and ?
            Map map = new HashMap();
            map.put("status", Orders.COMPLETED);
            map.put("dayBeginTime",dayBeginTime);
            map.put("dayEndTime",dayEndTime);
            Double turnover = orderMapper.sumByMap(map);
            //如果查出来的是空值，那么说明没有收入，赋值0.0
            turnover = turnover == null ? 0.0 : turnover;
            doubleList.add(turnover);
        }
        String turnoverList = StringUtils.join(doubleList, ",");
        TurnoverReportVO turnoverReportVO = new TurnoverReportVO(dateList,turnoverList);
        return turnoverReportVO;
    }


    /**
     * 统计用户数量数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        /**
         * 查询最近一段时间内一天之内的新增用户数量以及截至当天所有用户
         */
        //封装dateList
        List<LocalDate> localDateList = new ArrayList<>();
        //由于“昨天”的数据传进来的begin和end是同一天，会出现bug
        // 修正：包含起始日期
        LocalDate current = begin;
        while (!current.isAfter(end)) {  // 使用 isAfter 而不是 equals
            localDateList.add(current);
            current = current.plusDays(1);
        }
        // 即使 begin 和 end 是同一天，也会执行一次  ， localDateList 包含 [begin]

        String dateList = StringUtils.join(localDateList, ",");

        //封装totalUserList  用户总量，以逗号分隔，例如：200,210,220
        List<Integer> allUserNumList = new ArrayList<>();
        //封装newUserList  新增用户，以逗号分隔，例如：20,21,10
        List<Integer> todayUserNumList = new ArrayList<>();

        for (LocalDate date : localDateList) {
            //这里在一个循环中完成查询和封装
            LocalDateTime dayBeginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEndTime = LocalDateTime.of(date, LocalTime.MAX);
            //先封装结束时间查询在此之前的总用户
            Map map = new HashMap();
            map.put("dayEndTime",dayEndTime);
            Integer totalNum = userMapper.getSumByMap(map);
            allUserNumList.add(totalNum);
            //在封装开始时间，查询一天之内的新增用户
            map.put("dayBeginTime",dayBeginTime);
            totalNum = userMapper.getSumByMap(map);
            todayUserNumList.add(totalNum);
        }
        //用逗号把集合中的数据分隔开并转成字符串
        String totalUserList = StringUtils.join(allUserNumList, ",");
        String newUserList = StringUtils.join(todayUserNumList, ",");
        //封装vo返回给前端
        return new UserReportVO(dateList,totalUserList,newUserList);
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        //封装dateList
        List<LocalDate> localDateList = new ArrayList<>();
        //由于“昨天”的数据传进来的begin和end是同一天，会出现bug 修正：包含起始日期
        LocalDate current = begin;
        while (!current.isAfter(end)) {  // 使用 isAfter 而不是 equals
            localDateList.add(current);
            current = current.plusDays(1);
        }
        // 即使 begin 和 end 是同一天，也会执行一次  ， localDateList 包含 [begin]
        String dateList = StringUtils.join(localDateList, ",");

        List<Integer> orderNumList = new ArrayList<>();
        List<Integer> validOrderNumList = new ArrayList<>();
        for (LocalDate date : localDateList) {
            LocalDateTime dayBeginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEndTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderNum = getOrderNum(dayBeginTime,dayEndTime,null);
            orderNumList.add(orderNum);
            Integer validOrderNum = getOrderNum(dayBeginTime,dayEndTime,Orders.COMPLETED);
            validOrderNumList.add(validOrderNum);
        }
        String orderCountList = StringUtils.join(orderNumList, ",");
        String validOrderCountList = StringUtils.join(validOrderNumList, ",");
        //订单总数，遍历集合取和
        Integer totalOrderCount = orderNumList.stream().reduce(Integer::sum).get();
        //有效订单总数，遍历集合取和
        Integer validOrderCount = validOrderNumList.stream().reduce(Integer::sum).get();
        //订单完成率:  订单完成率 = 有效订单总数 ÷ 订单总数
        Double orderCompletionRate = 0.0;
        //为防止出现除数为0也就是订单总数的情况
        if (totalOrderCount != 0){
            orderCompletionRate =  validOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO.builder()
                .orderCompletionRate(orderCompletionRate)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCountList(orderCountList)
                .validOrderCountList(validOrderCountList)
                .dateList(dateList)
                .build();
    }

    /**
     * 通过传过来的map集合查询订单数量
     * @param dayBeginTime
     * @param dayEndTime
     * @param status
     * @return
     */
    private Integer getOrderNum(LocalDateTime dayBeginTime, LocalDateTime dayEndTime, Integer status) {
        Map map = new HashMap();
        map.put("dayBeginTime",dayBeginTime);
        map.put("dayEndTime",dayEndTime);
        map.put("status", status);
        return orderMapper.getSumByMap(map);
    }

    /**
     * 查询销量排名top10接口
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        //LocalDate->LocalDateTime
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> list = orderMapper.getSalesTop10(beginTime,endTime);
        List<String> names = list.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = list.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        String numberList = StringUtils.join(numbers, ",");
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}
