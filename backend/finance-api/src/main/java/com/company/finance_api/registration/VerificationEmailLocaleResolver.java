package com.company.finance_api.registration;

import com.company.finance_api.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class VerificationEmailLocaleResolver {

    private final UserRepository userRepository;

    public VerificationEmailLocaleResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @param explicitLocale optional locale from the client (registration page or profile UI)
     * @param email          recipient address used to look up stored preference when explicit is absent
     */
    public String resolve(String explicitLocale, String email) {
        if (StringUtils.hasText(explicitLocale)) {
            return VerificationMailLocale.normalize(explicitLocale);
        }
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> VerificationMailLocale.normalize(user.getPreferredLocale()))
                .orElse("en");
    }
}
