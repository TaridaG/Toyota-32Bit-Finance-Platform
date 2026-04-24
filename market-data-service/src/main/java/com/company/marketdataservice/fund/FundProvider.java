package com.company.marketdataservice.fund;

import java.util.List;

public interface FundProvider {

    String source();

    List<FundSnapshot> fetchLatestNavs(List<String> fundCodes);
}
