package io.github.dekkerding.examples.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductService implements Execute{
    /**
     * 查询搜索
     */
    @Override
    public Object search() {
        log.info("execute ProductService.search()");
        try {
            Thread.sleep(5000);
        }catch (InterruptedException e){
            log.error("ProductService InterruptedException", e);
        }
        return "ProductService.search()";
    }
}