package com.supportdesk.auth.service;

import com.supportdesk.auth.dto.RegistrationRequest;
import com.supportdesk.auth.entity.Role;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final UserRepository userRepository ;
    private final PasswordEncoder passwordEncoder ;


    @Transactional
    public Boolean createAccount(RegistrationRequest registrationRequest) {
        boolean result = false;

        if (isAlreadyRegistered(registrationRequest)) {
            return result;
        }

        User createUser = userRepository.save(transformDto(registrationRequest));
        if (createUser != null && createUser.getId() != null) {
            result = true;
        }
        return result;
    }

    private boolean isAlreadyRegistered(RegistrationRequest request) {
        return userRepository.existsByUsernameIgnoreCase(request.username().trim())
                || userRepository.existsByEmail(request.email().trim().toLowerCase())
                || userRepository.existsByMobileNumber(request.mobileNumber().trim());
    }

    private User transformDto(RegistrationRequest request) {
        return User.builder()
                .username(request.username().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .email(request.email().trim().toLowerCase())
                .mobileNumber(request.mobileNumber().trim())
                .role(Role.AGENT)
                .build();
    }
}
