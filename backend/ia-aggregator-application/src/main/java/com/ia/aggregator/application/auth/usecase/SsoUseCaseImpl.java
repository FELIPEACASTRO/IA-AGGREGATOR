package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.port.in.SsoUseCase;
import com.ia.aggregator.application.auth.port.out.SsoProviderPort;
import com.ia.aggregator.application.auth.port.out.TokenProvider;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import com.ia.aggregator.domain.auth.vo.AuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * SSO/OAuth implementation supporting Google, GitHub, Apple, Microsoft.
 */
@Service
public class SsoUseCaseImpl implements SsoUseCase {

    private static final Logger log = LoggerFactory.getLogger(SsoUseCaseImpl.class);

    private final SsoProviderPort ssoProviderPort;
    private final UserRepository userRepository;
    private final TokenProvider tokenGenerator;

    public SsoUseCaseImpl(SsoProviderPort ssoProviderPort,
                           UserRepository userRepository,
                           TokenProvider tokenGenerator) {
        this.ssoProviderPort = ssoProviderPort;
        this.userRepository = userRepository;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public String getAuthorizationUrl(AuthProvider provider, String redirectUri) {
        return ssoProviderPort.getAuthorizationUrl(provider, redirectUri);
    }

    @Override
    public SsoResult exchangeCode(AuthProvider provider, String code, String redirectUri) {
        SsoProviderPort.OAuthUserInfo userInfo = ssoProviderPort.exchangeCodeForUser(provider, code, redirectUri);

        if (userInfo == null || userInfo.email() == null) {
            throw new BusinessException(ErrorCode.AUTH_007, "Failed to retrieve user info from " + provider);
        }

        Optional<User> existingUser = userRepository.findByEmail(userInfo.email());
        boolean newUser = existingUser.isEmpty();

        User user;
        if (newUser) {
            user = User.registerOAuth(
                    userInfo.email(),
                    userInfo.name(),
                    provider,
                    userInfo.providerId(),
                    userInfo.avatarUrl()
            );
            userRepository.save(user);
            log.info("New SSO user registered: {} via {}", userInfo.email(), provider);
        } else {
            user = existingUser.get();
            user.recordLogin();
            userRepository.save(user);
        }

        String accessToken = tokenGenerator.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = tokenGenerator.generateRefreshToken(user.getId());

        return new SsoResult(accessToken, refreshToken, 900L, newUser,
                user.getEmail(), user.getFullName());
    }
}
