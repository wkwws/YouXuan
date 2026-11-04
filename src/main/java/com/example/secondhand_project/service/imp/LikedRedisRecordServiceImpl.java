package com.example.secondhand_project.service.imp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secondhand_project.constants.MqConstants;
import com.example.secondhand_project.constants.RedisConstants;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.enums.LikeBizTypeEnum;
import com.example.secondhand_project.exception.LoginException;
import com.example.secondhand_project.mapper.LikedRecordMapper;
import com.example.secondhand_project.pojo.dto.LikeCountMessage;
import com.example.secondhand_project.pojo.dto.LikeRecordFormDTO;
import com.example.secondhand_project.pojo.dto.LikedTimesDTO;
import com.example.secondhand_project.pojo.entity.LikedRecord;
import com.example.secondhand_project.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 * 点赞记录表 服务实现类（Redis 版，融合自 点赞功能模板/tj-remark）
 * </p>
 *
 * <p>
 * 模板思路：
 * 1. 用 Redis Set 存“谁赞了这个业务对象”，SADD/SREM 的返回值天然幂等；
 * 2. 用 Redis ZSet 存“待同步的点赞总数”，member=bizId，score=当前点赞数；
 * 3. 定时任务从 ZSet 弹出（ZPOPMIN）一批，通过 MQ 发给业务侧；
 * 4. 业务侧把绝对计数写回 post.like_count，完成最终一致。
 * </p>
 *
 * @author wkw
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LikedRedisRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final StringRedisTemplate stringRedisTemplate;

    private final RabbitTemplate rabbitTemplate;

    @Override
    public Long addordeleteRecord(LikeRecordFormDTO likeRecordFormDTO) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new LoginException("请先登录");
        }

        String bizKey = RedisConstants.LIKE_RECORD_BIZ + likeRecordFormDTO.getBizId();
        boolean liked = Boolean.TRUE.equals(likeRecordFormDTO.getLiked());

        if (liked) {
            // SADD 返回 1 表示首次点赞；返回 0 说明已经点过，直接返回当前计数
            Long added = stringRedisTemplate.opsForSet().add(bizKey, userId.toString());
            if (added == null || added <= 0) {
                return getCount(bizKey);
            }
        } else {
            // SREM 返回 1 表示确实删掉了；返回 0 说明本来就没赞过
            Long removed = stringRedisTemplate.opsForSet().remove(bizKey, userId.toString());
            if (removed == null || removed <= 0) {
                return getCount(bizKey);
            }
        }

        long size = getCount(bizKey);

        // 把最新计数写进 ZSet，等待定时任务批量弹出并投递 MQ
        String countKey = RedisConstants.LIKE_RECORD_COUNT + likeRecordFormDTO.getBizType().name();
        stringRedisTemplate.opsForZSet().add(
                countKey,
                String.valueOf(likeRecordFormDTO.getBizId()),
                size
        );

        return size;
    }

    @Override
    public List<Long> showUserLike(List<Long> bizIds) {
        if (bizIds == null || bizIds.isEmpty()) {
            return Collections.emptyList();
        }
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Collections.emptyList();
        }

        // Pipeline 批量判断：一次网络往返拿到所有 bizId 的点赞状态
        List<Object> objects = stringRedisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                StringRedisConnection stringRedisConnection = (StringRedisConnection) connection;
                for (Long bizId : bizIds) {
                    String key = RedisConstants.LIKE_RECORD_BIZ + bizId;
                    stringRedisConnection.sIsMember(key, userId.toString());
                }
                return null;
            }
        });

        return IntStream.range(0, bizIds.size())
                .filter(i -> Boolean.TRUE.equals(objects.get(i)))
                .mapToObj(bizIds::get)
                .collect(Collectors.toList());
    }

    @Override
    public void countmq(LikeBizTypeEnum bizType, int maxBizCount) {
        String countKey = RedisConstants.LIKE_RECORD_COUNT + bizType.name();

        // 模板使用 ZPOPMIN：弹出计数最小的 N 个业务对象，避免热点一直占着队列
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().popMin(countKey, maxBizCount);

        if (tuples == null || tuples.isEmpty()) {
            return;
        }

        List<LikedTimesDTO> list = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String value = tuple.getValue();
            Double score = tuple.getScore();
            if (value == null || score == null) {
                continue;
            }
            list.add(new LikedTimesDTO(Long.valueOf(value), score.intValue()));
        }
        if (list.isEmpty()) {
            return;
        }

        LikeCountMessage message = new LikeCountMessage(bizType.name(), list);
        String routingKey = String.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, bizType.name());

        try {
            rabbitTemplate.convertAndSend(MqConstants.Exchange.LIKE_RECORD_EXCHANGE, routingKey, message);
            log.info("发送点赞计数消息, bizType={}, size={}", bizType, list.size());
        } catch (Exception e) {
            // 模板是直接 ZPOPMIN 后发送；这里补一层兜底：发送失败就把弹出的计数写回 ZSet，避免丢失
            log.error("点赞计数消息发送失败，重新写回 ZSet, bizType={}", bizType, e);
            for (LikedTimesDTO dto : list) {
                stringRedisTemplate.opsForZSet().add(
                        countKey,
                        String.valueOf(dto.getBizId()),
                        dto.getLikedTimes()
                );
            }
        }
    }

    private long getCount(String bizKey) {
        Long size = stringRedisTemplate.opsForSet().size(bizKey);
        return size == null ? 0L : size;
    }

}
