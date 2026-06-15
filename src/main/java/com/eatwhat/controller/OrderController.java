
package com.eatwhat.controller;



import com.eatwhat.entity.Order;
import com.eatwhat.entity.OrderItem;
import com.eatwhat.entity.Cart;
import com.eatwhat.entity.Food;
import com.eatwhat.mapper.OrderMapper;
import com.eatwhat.mapper.OrderItemMapper;
import com.eatwhat.mapper.CartMapper;
import com.eatwhat.mapper.FoodMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatwhat.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
@CrossOrigin
public class OrderController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private FoodMapper foodMapper;
    @Autowired
    private JwtUtil jwtUtil;

    private Integer getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        System.out.println("========== 获取用户ID ==========");
        System.out.println("Authorization头: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Authorization头无效或不存在");
            return null;
        }

        String token = authHeader.substring(7);
        System.out.println("Token: " + token);

        Integer userId = jwtUtil.getUserIdFromToken(token);
        System.out.println("解析出的userId: " + userId);

        return userId;
    }
    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);
        String address = body.get("address");

        // 获取购物车
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        List<Cart> carts = cartMapper.selectList(wrapper);

        if (carts.isEmpty()) {
            result.put("code", 400);
            result.put("message", "购物车为空");
            return result;
        }

        // 计算总金额
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (Cart cart : carts) {
            Food food = foodMapper.selectById(cart.getFoodId());
            BigDecimal subtotal = food.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            total = total.add(subtotal);

            OrderItem item = new OrderItem();
            item.setFoodId(cart.getFoodId());
            item.setQuantity(cart.getQuantity());
            item.setPrice(food.getPrice());
            items.add(item);
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus("pending");
        order.setAddress(address);
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        // 保存订单项
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 清空购物车
        cartMapper.delete(wrapper);

        result.put("code", 200);
        result.put("message", "下单成功");
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        return result;
    }

    @GetMapping
    public Map<String, Object> list(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId).orderByDesc(Order::getCreatedAt);
        List<Order> orders = orderMapper.selectList(wrapper);

        result.put("code", 200);
        result.put("data", orders);
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Integer id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);

        LambdaQueryWrapper<com.eatwhat.entity.Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getId, id).eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            result.put("code", 404);
            result.put("message", "订单不存在");
            return result;
        }

        // 获取订单项
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, id);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        // 填充菜品信息
        List<Map<String, Object>> itemList = items.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("quantity", item.getQuantity());
            map.put("price", item.getPrice());
            Food food = foodMapper.selectById(item.getFoodId());
            map.put("food", food);
            return map;
        }).collect(Collectors.toList());

        result.put("code", 200);
        result.put("order", order);
        result.put("items", itemList);
        return result;
    }
    // 支付订单
    @PutMapping("/{id}/pay")
    public Map<String, Object> pay(@PathVariable Integer id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);

        if (userId == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return result;
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getId, id).eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            result.put("code", 404);
            result.put("message", "订单不存在");
            return result;
        }

        if (!"pending".equals(order.getStatus())) {
            result.put("code", 400);
            result.put("message", "订单状态不正确");
            return result;
        }

        order.setStatus("paid");
        orderMapper.updateById(order);

        result.put("code", 200);
        result.put("message", "支付成功");
        return result;
    }

    // 取消订单
    @PutMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable Integer id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);

        if (userId == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return result;
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getId, id).eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            result.put("code", 404);
            result.put("message", "订单不存在");
            return result;
        }

        if (!"pending".equals(order.getStatus())) {
            result.put("code", 400);
            result.put("message", "订单状态不正确，无法取消");
            return result;
        }

        order.setStatus("cancelled");
        orderMapper.updateById(order);

        result.put("code", 200);
        result.put("message", "订单已取消");
        return result;
    }

    // 确认收货
    @PutMapping("/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable Integer id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);

        if (userId == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return result;
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getId, id).eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            result.put("code", 404);
            result.put("message", "订单不存在");
            return result;
        }

        if (!"delivered".equals(order.getStatus())) {
            result.put("code", 400);
            result.put("message", "订单状态不正确");
            return result;
        }

        order.setStatus("completed");
        orderMapper.updateById(order);

        result.put("code", 200);
        result.put("message", "确认收货成功");
        return result;
    }


// 清空所有订单
    @DeleteMapping
    public Map<String, Object> clearAll(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);

        if (userId == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return result;
        }

        // 1. 先查询用户的所有订单
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getUserId, userId);
        List<Order> orders = orderMapper.selectList(orderWrapper);

        // 2. 删除每个订单对应的订单项
        for (Order order : orders) {
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            orderItemMapper.delete(itemWrapper);
        }

        // 3. 删除订单
        orderMapper.delete(orderWrapper);

        result.put("code", 200);
        result.put("message", "清空成功");
        return result;
    }
}