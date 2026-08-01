package com.example.secondhand_project.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新影响的行数")
public class UpdatedCount {
    @Schema(description = "更新的记录数")
    private Integer updatedCount;
}
