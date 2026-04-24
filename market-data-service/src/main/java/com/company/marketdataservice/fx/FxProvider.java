package com.company.marketdataservice.fx;

import java.util.List;

public interface FxProvider {

    List<FxSnapshot> fetchLatestRates();

    String source();
}
