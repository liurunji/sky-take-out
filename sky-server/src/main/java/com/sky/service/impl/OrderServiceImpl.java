package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理各种业务异常(地址簿为空、购物车数据为空)
        //1.1 处理用户地址簿为空的情况
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //1.2 处理购物车数据为空的情况
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入1条数据
        //2.1拷贝属性值到orders对象
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        //2.2继续封装其他属性
        orders.setOrderTime(LocalDateTime.now());  //下单时间
        orders.setAddress(addressBook.getDetail());  //详细地址
        orders.setConsignee(addressBook.getConsignee());  //收货人
        orders.setPhone(addressBook.getPhone());  //手机号
        orders.setUserId(userId);  //用户id
        orders.setNumber(String.valueOf(System.currentTimeMillis()));  //设置订单号（用时间戳作为订单号）
        orders.setPayStatus(Orders.UN_PAID);  //支付状态为未支付
        orders.setStatus(Orders.PENDING_PAYMENT);  //订单状态为待支付
        //orders.setUserName();
        //2.3调用mapper插入数据
        orderMapper.insert(orders);
        //向订单明细表插入n条数据
        //3.1 orderDetailList大集合放OrderDetail
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : list) {
            //遍历ShoppingCart集合，拷贝属性到orderDetail
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            //封装订单id
            orderDetail.setOrderId(orders.getId());
            //放到大集合当中
            orderDetailList.add(orderDetail);
        }
        //3.2 调用mapper批量插入
        orderDetailMapper.insertBatch(orderDetailList);
        //清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);
        //封装VO返回结果
        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        orderSubmitVO.setId(orders.getId());  //订单id
        orderSubmitVO.setOrderTime(orders.getOrderTime());  //下单时间
        orderSubmitVO.setOrderAmount(orders.getAmount());  //总金额
        orderSubmitVO.setOrderNumber(orders.getNumber());  //订单号
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
/*        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));*/

        /**
         * 跳过微信支付
         */
        log.info("跳过微信支付，支付成功");
        paySuccess(ordersPaymentDTO.getOrderNumber());
        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }


    /**
     * 历史订单查询
     *
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult pageQueryForUser(int page, int pageSize, Integer status) {
        //设置分页参数
        PageHelper.startPage(page, pageSize);
        //封装到dto
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        //查询orders
        List<Orders> ordersList = orderMapper.pageQuery(ordersPageQueryDTO);

        //如果查到了订单，遍历订单去查询订单详细数据
        List<OrderVO> orderVOList = new ArrayList<>();
        if (ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                //查询对应的详细订单数据
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                //将订单明细数据和一条订单数据封装到一个orderVO对象里
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                //把vo对象放到大集合中
                orderVOList.add(orderVO);
            }
        }
        Page p = (Page) ordersList;
        //总记录数就是订单的数量，数据集合就是orderVOList
        PageResult pageResult = new PageResult(p.getTotal(),orderVOList);
        return pageResult;
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO orderDetail(Long id) {
        //通过id查询订单
        Orders orders = orderMapper.getById(id);
        //通过订单id查询订单明细
        List<OrderDetail> list = orderDetailMapper.getByOrderId(id);
        //把订单明细和订单数据封装到vo对象并返回
        OrderVO orderVO = new OrderVO();
        orderVO.setOrderDetailList(list);
        BeanUtils.copyProperties(orders,orderVO);
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void userCancelById(Long id) {
        /**
         * 业务规则：
         * - 待支付和待接单状态下，用户可直接取消订单
         * - 商家已接单状态下，用户取消订单需电话沟通商家
         * - 派送中状态下，用户取消订单需电话沟通商家
         * - 如果在待接单状态下取消订单，需要给用户退款
         * - 取消订单后需要将订单状态修改为“已取消”
         */
        //根据id查询订单
        Orders o = orderMapper.getById(id);
        //校验订单是否存在
        if (o == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Integer status = o.getStatus(); //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (status > 2){
            //3已接单 4派送中 5已完成 6已取消  此时不允许用户端取消订单
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setUserId(o.getUserId());
        orders.setId(o.getId());
        //如果在待接单状态下取消订单，需要给用户退款
        if (status.equals(Orders.TO_BE_CONFIRMED)){
            //调用微信支付退款接口(跳过支付相关功能)
            /*weChatPayUtil.refund(
                    o.getNumber(), //商户订单号
                    o.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额*/

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        //再来一单就是将原订单中的商品重新加入到购物车中
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        //根据id查询订单明细
        List<OrderDetail> detailsList = orderDetailMapper.getByOrderId(id);
        //创建shoppingCard的集合
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        for (OrderDetail orderDetail : detailsList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            //把orderDetail拷贝到shoppingCart
            BeanUtils.copyProperties(orderDetail,shoppingCart);
            //设置当前用户id和创建时间
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            //加入到集合中
            shoppingCartList.add(shoppingCart);
        }
        //批量插入到购物车
        shoppingCartMapper.insertBatch(shoppingCartList);
    }


}
