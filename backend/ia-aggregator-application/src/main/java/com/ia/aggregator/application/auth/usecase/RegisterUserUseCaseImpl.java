package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.RegisterUserCommand;
import com.ia.aggregator.application.auth.dto.UserResponse;
import com.ia.aggregator.application.auth.port.in.RegisterUserUseCase;
import com.ia.aggregator.application.auth.port.out.UserEventPublisher;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import com.ia.aggregator.domain.auth.service.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher eventPublisher;

    public RegisterUserUseCaseImpl(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UserResponse execute(RegisterUserCommand command) {
        // Check for duplicate email
        if (userRepository.existsByEmail(command.email())) {
            throw new BusinessException(ErrorCode.AUTH_003,
                    "Email already registered: " + command.email());
        }

        // Create domain entity
        String hashedPassword = passwordEncoder.encode(command.password());
        User user = User.register(command.email(), hashedPassword, command.fullName());

        // Persist
        User savedUser = userRepository.save(user);

        // Publish domain events
        savedUser.getDomainEvents().forEach(eventPublisher::publish);
        savedUser.clearDomainEvents();

        return UserResponse.from(savedUser);
    }
}
