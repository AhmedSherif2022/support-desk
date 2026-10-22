package com.supportdesk.auth.service;
import com.supportdesk.auth.entity.Role;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.common.exception.UnauthenticatedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CurrentUserService
{

    private final UserRepository userRepository;


    public AuthenticatedUser getCurrentUser()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new UnauthenticatedException();
    }
    public Long getCurrentUserId() {
        return getCurrentUser().id();
    }
    public boolean hasRole(Role role) {
        return getCurrentUser().role() == role;
    }

    @Transactional(readOnly = true)
    public User getCurrentUserEntity()
    {
        return userRepository.findById(getCurrentUserId())
                .orElseThrow(UnauthenticatedException::new);
    }
}
