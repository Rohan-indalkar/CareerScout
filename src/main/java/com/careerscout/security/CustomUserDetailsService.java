package com.careerscout.security;

import com.careerscout.user.UserRepository;
import com.careerscout.user.UserAccount;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmailIgnoreCase(email)
                .filter(UserAccount::isEnabled)
                .map(user -> new AuthenticatedUser(user.getId(), user.getEmail(),
                        user.getPasswordHash(), user.getRole(), null))
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }
}
