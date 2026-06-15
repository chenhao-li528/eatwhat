
package com.eatwhat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatwhat.entity.Food;
import com.eatwhat.mapper.FoodMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/foods")
@CrossOrigin
public class FoodController {

    @Autowired
    private FoodMapper foodMapper;

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String category) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Food::getCategory, category);
        }
        List<Food> list = foodMapper.selectList(wrapper);

        result.put("code", 200);
        result.put("data", list);
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        Food food = foodMapper.selectById(id);
        result.put("code", 200);
        result.put("data", food);
        return result;
    }
}