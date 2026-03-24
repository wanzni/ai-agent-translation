package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.dto.review.ReviewTaskDetailResponse;
import cn.net.wanzni.ai.translation.dto.review.ReviewQueueStatsResponse;
import cn.net.wanzni.ai.translation.dto.review.ReviewTaskResponse;
import cn.net.wanzni.ai.translation.dto.review.ReviewTaskReviseRequest;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.service.ReviewTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Slf4j
@RestController
@RequestMapping("/api/review/tasks")
@RequiredArgsConstructor
@Validated
public class ReviewTaskController {

    private final ReviewTaskService reviewTaskService;

    @GetMapping
    public Page<ReviewTaskResponse> list(@RequestParam(value = "status", defaultValue = "PENDING") ReviewStatusEnum status,
                                         @RequestParam(value = "reasonCode", required = false) String reasonCode,
                                         @RequestParam(value = "reviewerId", required = false) Long reviewerId,
                                         @RequestParam(value = "mine", defaultValue = "false") boolean mine,
                                         @RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Long effectiveReviewerId = mine ? requireReviewerId() : reviewerId;
        log.info("List review tasks: status={}, reasonCode={}, reviewerId={}, mine={}, page={}, size={}",
                status, reasonCode, effectiveReviewerId, mine, page, size);
        return reviewTaskService.list(status, reasonCode, effectiveReviewerId, pageable);
    }

    @GetMapping("/views/public")
    public Page<ReviewTaskResponse> publicQueue(@RequestParam(value = "reasonCode", required = false) String reasonCode,
                                                @RequestParam(value = "page", defaultValue = "0") int page,
                                                @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        log.info("List public review queue: reasonCode={}, page={}, size={}", reasonCode, page, size);
        return reviewTaskService.listPublicQueue(reasonCode, pageable);
    }

    @GetMapping("/views/mine")
    public Page<ReviewTaskResponse> myQueue(@RequestParam(value = "page", defaultValue = "0") int page,
                                            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Long reviewerId = requireReviewerId();
        log.info("List my review queue: reviewerId={}, page={}, size={}", reviewerId, page, size);
        return reviewTaskService.listMyTasks(reviewerId, pageable);
    }

    @GetMapping("/views/processed")
    public Page<ReviewTaskResponse> processedQueue(@RequestParam(value = "reasonCode", required = false) String reasonCode,
                                                   @RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        log.info("List processed review queue: reasonCode={}, page={}, size={}", reasonCode, page, size);
        return reviewTaskService.listProcessedTasks(reasonCode, pageable);
    }

    @GetMapping("/stats")
    public ReviewQueueStatsResponse stats() {
        log.info("Get review queue stats");
        return reviewTaskService.stats();
    }

    @GetMapping("/{id}")
    public ReviewTaskDetailResponse get(@PathVariable Long id) {
        log.info("Get review task detail: {}", id);
        return reviewTaskService.get(id);
    }

    @PostMapping("/{id}/claim")
    public ReviewTaskResponse claim(@PathVariable Long id) {
        Long reviewerId = requireReviewerId();
        log.info("Claim review task: id={}, reviewerId={}", id, reviewerId);
        return reviewTaskService.claim(id, reviewerId);
    }

    @PostMapping("/{id}/release")
    public ReviewTaskResponse release(@PathVariable Long id) {
        Long reviewerId = requireReviewerId();
        log.info("Release review task: id={}, reviewerId={}", id, reviewerId);
        return reviewTaskService.release(id, reviewerId);
    }

    @PostMapping("/{id}/approve")
    public ReviewTaskResponse approve(@PathVariable Long id) {
        Long reviewerId = requireReviewerId();
        log.info("Approve review task: id={}, reviewerId={}", id, reviewerId);
        return reviewTaskService.approve(id, reviewerId);
    }

    @PostMapping("/{id}/revise")
    public ReviewTaskResponse revise(@PathVariable Long id,
                                     @Valid @RequestBody ReviewTaskReviseRequest request) {
        Long reviewerId = requireReviewerId();
        log.info("Revise review task: id={}, reviewerId={}", id, reviewerId);
        return reviewTaskService.revise(id, request.getFinalText(), reviewerId);
    }

    private Long requireReviewerId() {
        Long reviewerId = UserContext.getUserId();
        if (reviewerId == null) {
            throw new IllegalStateException("Reviewer must be logged in");
        }
        return reviewerId;
    }
}
