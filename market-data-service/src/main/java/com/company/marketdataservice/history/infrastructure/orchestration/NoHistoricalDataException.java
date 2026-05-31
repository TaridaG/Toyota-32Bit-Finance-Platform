package com.company.marketdataservice.history.infrastructure.orchestration;

/**
 * Signals that a provider returned no data for the requested historical window
 * and retrying is not expected to help.
 */
public class NoHistoricalDataException extends RuntimeException {
    public NoHistoricalDataException(String message) {
        super(message);
    }
}

