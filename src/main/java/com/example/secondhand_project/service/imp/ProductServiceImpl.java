package com.example.secondhand_project.service.imp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.example.secondhand_project.admin.service.AdminAuditTaskService;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.pojo.dto.ProductDTO;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.mapper.ProductMapper;
import com.example.secondhand_project.pojo.entity.User;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.ProductVO;
import com.example.secondhand_project.service.AuthService;
import com.example.secondhand_project.service.IProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author wkw
 * @since 2026-05-29
 */
@Service
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {


    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminAuditTaskService auditTaskService;

    @Autowired
    private ObjectMapper objectMapper;


    @Override
    @Transactional
    public ProductVO addProduct(ProductDTO productDTO) {

        //获取用户id
        Long currentId = BaseContext.getCurrentId();

        //
        Product Product = new Product();
        BeanUtils.copyProperties(productDTO,Product);

        Product product = Product.setUid(Math.toIntExact(currentId));
        Product.setAuditStatus(0);
        Product.setAuditRemark("");

        //存入product
        this.save(Product);

        auditTaskService.submit("PRODUCT", Product.getId(), Product.getUid(),
                Product.getTitle(), Product.getDescription(), snapshot(Product));

        Product byId = this.getById(Product.getId());


        ProductVO build = ProductVO.builder()
                .id(Product.getId())
                .title(Product.getTitle())
                .price(Product.getPrice())
                .createdAt(LocalDateTime.now())
                .build();

        return build;

    }

    @Override
    public PageVO<Product> getAllProduct(Pagee pagee) {

        log.info("查询分页数据");
        Page<Product> page=new Page<>(pagee.getPage(),pagee.getPageSize());

        Page<Product> page1 = this.lambdaQuery()
                .ne(Product::getAuditStatus, 2)
                .eq(pagee.getCategory()!=null&&!(pagee.getCategory()==0),Product::getCategory,pagee.getCategory())
                .like(pagee.getKeyword()!=null&&!(pagee.getKeyword().isEmpty()),Product::getTitle,pagee.getKeyword())
                .page(page);

        List<Product> records = page1.getRecords();
        long total = page1.getTotal();
        long pages = page1.getPages();
        Integer pageSize = pagee.getPageSize();

        //添加username 和 nickname
        for (Product record : records) {
            Integer uid = record.getUid();

            User one = Db.lambdaQuery(User.class)
                    .eq(User::getId, uid)
                    .one();

            record.setNickName(one.getNickname());
            record.setUserName(one.getUsername());
        }




        PageVO<Product> pageVO = new PageVO<>();
        pageVO.setPageSize(pageSize);
        pageVO.setList(records);
        pageVO.setTotal(((Number)total).intValue());
        pageVO.setPage(((Number)pages).intValue());

        return pageVO;
    }

    @Override
    public Product getdetailproduct(Integer id) {

        Product byId = this.getById(id);

        if (byId == null) {
            return null;
        }
        Long currentId = BaseContext.getCurrentId();
        if (byId.getAuditStatus() != null && byId.getAuditStatus() == 2
                && (currentId == null || !currentId.equals(byId.getUid().longValue()))) {
            return null;
        }

        Integer uid = byId.getUid();

        User byId1 = authService.getById(uid);

        byId.setUserName(byId1.getUsername());
        byId.setNickName(byId1.getNickname());


        return byId;

    }

    @Override
    public void updateproduct(ProductDTO productDTO, Integer id) {
        //将DTO copy 成 po
        Product product = new Product();
        BeanUtils.copyProperties(productDTO,product);
        //更新时间
        product.setUpdatedAt(LocalDateTime.now());
        product.setId(id);

        this.updateById(product);

    }

    private String snapshot(Product product) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", product.getId());
            map.put("title", product.getTitle());
            map.put("description", product.getDescription());
            map.put("price", product.getPrice());
            map.put("images", product.getImages());
            map.put("uid", product.getUid());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }
}
