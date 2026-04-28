package com.company.marketdataservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "market.scheduler.enabled=false",
        "market.fx.scheduler-enabled=false",
        "market.stock.scheduler.enabled=false"
})
class MarketDataServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
