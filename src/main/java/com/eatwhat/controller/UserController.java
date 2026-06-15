package com.eatwhat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatwhat.dto.LoginDTO;
import com.eatwhat.dto.RegisterDTO;
import com.eatwhat.entity.User;
import com.eatwhat.mapper.UserMapper;
import com.eatwhat.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    // MD5 加密
    private String md5Encode(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(rawPassword.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return rawPassword;
        }
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterDTO dto) {
        Map<String, Object> result = new HashMap<>();

        // 检查用户名是否存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            result.put("code", 400);
            result.put("message", "用户名已存在");
            return result;
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(md5Encode(dto.getPassword()));  // MD5 加密
        user.setPhone(dto.getPhone());
        userMapper.insert(user);

        result.put("code", 200);
        result.put("message", "注册成功");
        result.put("userId", user.getId());
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginDTO dto) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            result.put("code", 401);
            result.put("message", "用户名或密码错误");
            return result;
        }

        // 验证密码（MD5 比对）
        if (!md5Encode(dto.getPassword()).equals(user.getPassword())) {
            result.put("code", 401);
            result.put("message", "用户名或密码错误");
            return result;
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        result.put("code", 200);
        result.put("message", "登录成功");
        result.put("token", token);
        result.put("user", user);
        return result;
    }
}