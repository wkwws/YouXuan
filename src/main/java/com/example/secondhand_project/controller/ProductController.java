package com.example.secondhand_project.controller;


import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.pojo.dto.ProductDTO;
import com.example.secondhand_project.pojo.entity.Pagee;
import com.example.secondhand_project.pojo.entity.Product;
import com.example.secondhand_project.pojo.vo.PageVO;
import com.example.secondhand_project.pojo.vo.ProductVO;
import com.example.secondhand_project.result.Result;
import com.example.secondhand_project.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author wkw
 * @since 2026-05-29
 */
@RestController
@RequestMapping("/products")
@Tag(name = "02-商品管理", description = "商品的增删改查")
public class ProductController {


    @Autowired
    private IProductService iProductService;


    /**
     *添加商品
     * @param productDTO
     * @return
     */
    @Operation(summary = "添加商品", description = "发布一个新的二手商品（需登录）")
    @PostMapping()
    public Result addProduct(@RequestBody ProductDTO productDTO){

        ProductVO productVO = iProductService.addProduct(productDTO);

        return Result.success(productVO);
    }

    /**
     * 获取商品列表
     * @return
     */
    @Operation(summary = "获取商品列表", description = "分页获取所有在售商品")
    @GetMapping()
    public Result getAllProduct(Pagee page){

       PageVO<Product> p= iProductService.getAllProduct(page);

        return Result.success(p);
    }

    /**
     * 获取商品详细信息
     * @param id
     * @return
     */
    @Operation(summary = "获取商品详情", description = "根据商品 ID 获取详细信息")
    @GetMapping("/{id}")
    public Result getdetailproduct(@Parameter(description = "商品 ID") @PathVariable Integer id){

        Product getdetailproduct = iProductService.getdetailproduct(id);

        return Result.success(getdetailproduct);
    }


    /**
     * 更新商品
     * @return
     */
    @Operation(summary = "更新商品", description = "根据商品 ID 更新商品信息（需登录，仅自己的商品）")
    @PutMapping("/{id}")
    public Result updateproduct(@Parameter(description = "商品 ID") @PathVariable Integer id,
                                @RequestBody ProductDTO productDTO){

        Product existing = iProductService.getById(id);
        if (existing == null) {
            return Result.error("商品不存在");
        }
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null || !currentId.equals(existing.getUid().longValue())) {
            return Result.error("无权操作该商品");
        }
        iProductService.updateproduct(productDTO,id);

        return Result.success("更新成功");
    }


    /**
     * 删除商品
     * @return
     */
    @Operation(summary = "删除商品", description = "根据商品 ID 删除商品（需登录，仅自己的商品）")
    @DeleteMapping("/{id}")
    public Result deleteproduct(@Parameter(description = "商品 ID") @PathVariable Integer id){

        Product existing = iProductService.getById(id);
        if (existing == null) {
            return Result.error("商品不存在");
        }
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null || !currentId.equals(existing.getUid().longValue())) {
            return Result.error("无权操作该商品");
        }
        iProductService.removeById(id);

        return Result.success();
    }

}
