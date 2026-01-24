package com.company.finance_api.security;

import java.util.UUID;

public interface CurrentUserResolver {

    UUID getCurrentUserId();
}
