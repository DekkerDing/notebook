package io.github.dekkerding.examples.chainofresponsibility;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class HandlerA implements Handler{
    private Handler next;

    /**
     * @param handler
     */
    @Override
    public void setNext(Handler handler) {
        this.next = handler;
    }

    /**
     * @param request
     */
    @Override
    public void handle(Request request) {
        log.info("HandlerA执行代码逻辑，处理： {}" ,request.getData());
        if(null!=next){
            next.handle(request);
        }else {
            log.info("HandlerA执行代码逻辑，处理完成");
        }
    }
}