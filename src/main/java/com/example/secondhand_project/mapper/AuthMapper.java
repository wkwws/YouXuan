package com.example.secondhand_project.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.secondhand_project.pojo.dto.LoginDTO;
import com.example.secondhand_project.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

public interface AuthMapper extends BaseMapper<User> {

    @Select("select * from user where username=#{username}")
    User getbyusername(LoginDTO loginDTO);

}
