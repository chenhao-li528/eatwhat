package com.eatwhat.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("food")
public class Food {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String description;
    private String image;
    private BigDecimal price;
    private String category;
    private Integer stock;
}
