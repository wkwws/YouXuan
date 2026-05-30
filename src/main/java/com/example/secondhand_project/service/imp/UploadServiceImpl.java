package com.example.secondhand_project.service.imp;

import com.example.secondhand_project.pojo.entity.Upload;
import com.example.secondhand_project.mapper.UploadMapper;
import com.example.secondhand_project.service.IUploadService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文件上传记录表 服务实现类
 * </p>
 *
 * @author wkw
 * @since 2026-05-27
 */
@Service
public class UploadServiceImpl extends ServiceImpl<UploadMapper, Upload> implements IUploadService {

}
