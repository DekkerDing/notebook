package io.github.dekkerding.examples;

import io.github.dekkerding.examples.service.MemberService;
import io.github.dekkerding.examples.service.OrderService;
import io.github.dekkerding.examples.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Controller
public class PortalController {

    @Resource
    private MemberService memberService;

    @Resource
    private OrderService orderService;

    @Resource
    private ProductService productService;

    /**
     * 串行执行
     */
    public void case1() {
        log.info(String.valueOf(memberService.search()));
        log.info(String.valueOf(orderService.search()));
        log.info(String.valueOf(productService.search()));
    }

    /**
     * 并行执行
     *
     * A B C
     * -----
     * 执行其他
     *
     */
    public void case2() throws ExecutionException, InterruptedException {
        CompletableFuture<Object> memberServiceCompletableFuture = CompletableFuture.supplyAsync(() -> memberService.search());
        CompletableFuture<Object> orderServiceCompletableFuture = CompletableFuture.supplyAsync(() -> orderService.search());
        CompletableFuture<Object> productServiceCompletableFuture = CompletableFuture.supplyAsync(() -> productService.search());
        CompletableFuture.allOf(
                memberServiceCompletableFuture,
                orderServiceCompletableFuture,
                productServiceCompletableFuture
        );

        log.info(String.valueOf(memberServiceCompletableFuture.get()));
        log.info(String.valueOf(orderServiceCompletableFuture.get()));
        log.info(String.valueOf(productServiceCompletableFuture.get()));
    }

    /**
     *      A
     *     / \
     *    B  C
     *    ------
     *    执行其他
     */
    public void case3() throws ExecutionException, InterruptedException {

        // 返回处理完成的任务，这里会产生阻塞
        CompletableFuture<Object> memberServiceCompletableFuture = CompletableFuture.completedFuture(memberService.search());

        // 得到结果 这里可嵌入业务校验
        log.info(String.valueOf(memberServiceCompletableFuture.get()));

        CompletableFuture<Object> orderServiceCompletableFuture = CompletableFuture.supplyAsync(() -> orderService.search());
        CompletableFuture<Object> productServiceCompletableFuture = CompletableFuture.supplyAsync(() -> productService.search());
        CompletableFuture.allOf(
                orderServiceCompletableFuture,
                productServiceCompletableFuture
        );

        log.info(String.valueOf(orderServiceCompletableFuture.get()));
        log.info(String.valueOf(productServiceCompletableFuture.get()));
    }

    /**
     *  A B
     *    |
     *    C
     *  ------
     *  执行其他
     */
    public void case4() throws ExecutionException, InterruptedException {
        CompletableFuture<Object> memberServiceCompletableFuture = CompletableFuture.supplyAsync(() -> memberService.search());
        CompletableFuture<Object> productServiceCompletableFuture = CompletableFuture.supplyAsync(() -> productService.search());

        CompletableFuture<Object> orderServiceCompletableFuture = productServiceCompletableFuture.thenApply((product) -> {
            // product = 上一步处理结果
            return orderService.search();
        });
        CompletableFuture.allOf(memberServiceCompletableFuture,orderServiceCompletableFuture);

        log.info(String.valueOf(memberServiceCompletableFuture.get()));
        log.info(String.valueOf(orderServiceCompletableFuture.get()));
        log.info(String.valueOf(productServiceCompletableFuture.get()));
    }
}