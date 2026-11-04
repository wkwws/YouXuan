package com.example.secondhand_project.job;

import com.example.secondhand_project.enums.LikeBizTypeEnum;
import com.example.secondhand_project.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 点赞计数定时同步任务。
 *
 * <p>
 * 对应模板 tj-remark 里的 LikeRecordCountTest：
 * 每 20 秒把 ZSet 里攒着的一批点赞计数弹出来，通过 MQ 交给业务侧落库。
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LikeCountSyncJob {

    // 评论点赞接入后，把 LikeBizTypeEnum.COMMENT 加进来即可
    private static final List<LikeBizTypeEnum> BIZ_TYPES = List.of(LikeBizTypeEnum.POST);

    private static final int MAX_BIZ_COUNT = 30;

    private final ILikedRecordService likedRecordService;

    @Scheduled(fixedDelay = 20000)
    public void sync() {
        for (LikeBizTypeEnum bizType : BIZ_TYPES) {
            try {
                likedRecordService.countmq(bizType, MAX_BIZ_COUNT);
            } catch (Exception e) {
                log.error("同步点赞计数失败, bizType={}", bizType, e);
            }
        }
    }

}
