package com.nikhitha.whispr.security;

import com.nikhitha.whispr.entity.User;
import com.nikhitha.whispr.repository.UserRepository;

import com.nikhitha.whispr.security.UserPrincipal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // return org.springframework.security.core.userdetails.User
        //         .withUsername(user.getUsername())
        //         .password(user.getPassword())
        //         .authorities("USER") 
        //         .build();
         return UserPrincipal.create(user);
    } 
}
