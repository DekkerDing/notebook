package io.github.dekkerding.examples.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderService implements Execute{

    @Override
    public Object search(){
        log.info("execute OrderService.search()");
        try {
            Thread.sleep(4000);
        }catch (InterruptedException e){
            log.error("OrderService InterruptedException", e);
        }
        return "OrderService.search()";
    }
}