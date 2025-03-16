package io.github.dekkerding.examples.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MemberService implements Execute{

    @Override
    public  Object search(){
        log.info("execute MemberService.search()");
        try {
            Thread.sleep(3000);
        }catch (InterruptedException e){
            log.error("MemberService InterruptedException", e);
        }
        return "MemberService.search()";
    }

}