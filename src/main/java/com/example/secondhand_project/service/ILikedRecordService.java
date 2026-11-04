package com.example.secondhand_project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secondhand_project.enums.LikeBizTypeEnum;
import com.example.secondhand_project.pojo.dto.LikeRecordFormDTO;
import com.example.secondhand_project.pojo.entity.LikedRecord;

import java.util.List;

/**
 * <p>
 * 点赞记录表 服务类（对应模板 tj-remark 的 ILikedRecordService）
 * </p>
 *
 * @author wkw
 */
public interface ILikedRecordService extends IService<LikedRecord> {

    /**
     * 点赞或取消点赞。
     *
     * @return 该业务对象当前的点赞总数
     */
    Long addordeleteRecord(LikeRecordFormDTO likeRecordFormDTO);

    /**
     * 查询当前用户在上述业务对象中已经点赞的 ID 列表。
     */
    List<Long> showUserLike(List<Long> bizIds);

    /**
     * 从 ZSet 中弹出一批计数并通过 MQ 发给业务侧落库。
     *
     * @param bizType     业务类型，例如 POST / COMMENT
     * @param maxBizCount 一次最多处理多少个业务对象
     */
    void countmq(LikeBizTypeEnum bizType, int maxBizCount);

}
