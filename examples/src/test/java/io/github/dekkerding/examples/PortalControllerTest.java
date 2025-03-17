package io.github.dekkerding.examples;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutionException;

@SpringBootTest
public class PortalControllerTest {
    @Autowired
    private PortalController portalController;

    @Test
    public void case1() {
        portalController.case1();
    }

    @Test
    public void case2() throws ExecutionException, InterruptedException {
        portalController.case2();
    }

    @Test
    public void case3() throws ExecutionException, InterruptedException {
        portalController.case3();
    }

    @Test
    public void case4() throws ExecutionException, InterruptedException {
        portalController.case4();
    }

    @Test
    public void case5() throws ExecutionException, InterruptedException {
        portalController.case5();
    }
}