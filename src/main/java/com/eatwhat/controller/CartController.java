package com.eatwhat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatwhat.dto.CartDTO;
import com.eatwhat.entity.Cart;
import com.eatwhat.entity.Food;
import com.eatwhat.mapper.CartMapper;
import com.eatwhat.mapper.FoodMapper;
import com.eatwhat.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cart")
@CrossOrigin
public class CartController {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private FoodMapper foodMapper;

    @Autowired
    private JwtUtil jwtUtil;

    // 从请求头获取用户ID
    private Integer getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        token = token.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody CartDTO dto, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);

        System.out.println("========== 添加购物车 ==========");
        System.out.println("userId: " + userId);
        System.out.println("foodId: " + dto.getFoodId());
        System.out.println("quantity: " + dto.getQuantity());

        if (userId == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return result;
        }

        // 查找是否已存在
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getFoodId, dto.getFoodId());
        Cart exist = cartMapper.selectOne(wrapper);

        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + dto.getQuantity());
            cartMapper.updateById(exist);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setFoodId(dto.getFoodId());
            cart.setQuantity(dto.getQuantity());
            cartMapper.insert(cart);
        }

        result.put("code", 200);
        result.put("message", "添加成功");
        return result;
    }

    @GetMapping
    public Map<String, Object> list(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);

        if (userId == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            result.put("data", new ArrayList<>());
            return result;
        }

        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        List<Cart> carts = cartMapper.selectList(wrapper);

        List<Map<String, Object>> list = carts.stream().map(cart -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cart.getId());
            item.put("quantity", cart.getQuantity());
            Food food = foodMapper.selectById(cart.getFoodId());
            item.put("food", food);
            return item;
        }).collect(Collectors.toList());

        result.put("code", 200);
        result.put("data", list);
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Integer id, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);

        if (userId == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return result;
        }

        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getId, id).eq(Cart::getUserId, userId);
        Cart cart = cartMapper.selectOne(wrapper);

        if (cart == null) {
            result.put("code", 404);
            result.put("message", "购物车项不存在");
            return result;
        }

        Integer quantity = body.get("quantity");
        if (quantity == 0) {
            cartMapper.deleteById(id);
        } else {
            cart.setQuantity(quantity);
            cartMapper.updateById(cart);
        }

        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Integer userId = getUserId(request);

        if (userId == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return result;
        }

        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getId, id).eq(Cart::getUserId, userId);
        cartMapper.delete(wrapper);

        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }
}