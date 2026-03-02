package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.UserResponse;
import com.ia.aggregator.application.auth.port.in.GetCurrentUserUseCase;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetCurrentUserUseCaseImpl implements GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public GetCurrentUserUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GEN_003, "User not found"));

        return UserResponse.from(user);
    }
}
