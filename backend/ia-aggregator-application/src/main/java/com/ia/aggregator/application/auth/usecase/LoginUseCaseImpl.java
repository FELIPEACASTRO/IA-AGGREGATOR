package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.LoginCommand;
import com.ia.aggregator.application.auth.dto.TokenResponse;
import com.ia.aggregator.application.auth.port.in.LoginUseCase;
import com.ia.aggregator.application.auth.port.out.TokenProvider;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import com.ia.aggregator.domain.auth.service.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCaseImpl implements LoginUseCase {

    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900; // 15 min

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public LoginUseCaseImpl(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public TokenResponse execute(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_001,
                        "Invalid email or password"));

        if (!user.isActive()) {
            if (user.getStatus() == com.ia.aggregator.domain.auth.vo.UserStatus.SUSPENDED) {
                throw new BusinessException(ErrorCode.AUTH_002, "Account is suspended");
            }
            if (user.getStatus() == com.ia.aggregator.domain.auth.vo.UserStatus.PENDING_VERIFICATION) {
                throw new BusinessException(ErrorCode.AUTH_006, "Email not verified");
            }
            throw new BusinessException(ErrorCode.AUTH_001, "Account is not active");
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_001, "Invalid email or password");
        }

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        // Record login
        user.recordLogin();
        userRepository.save(user);

        return TokenResponse.of(accessToken, refreshToken, ACCESS_TOKEN_EXPIRY_SECONDS);
    }
}
