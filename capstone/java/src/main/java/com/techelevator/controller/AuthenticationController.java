package com.techelevator.controller;

import javax.validation.Valid;

import com.techelevator.Exceptions.ThreadSleepTryCatch;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.techelevator.dao.UserDao;
import com.techelevator.model.LoginDTO;
import com.techelevator.model.RegisterUserDTO;
import com.techelevator.model.User;
import com.techelevator.model.UserAlreadyExistsException;
import com.techelevator.security.jwt.JWTFilter;
import com.techelevator.security.jwt.TokenProvider;

//@PreAuthorize("isAuthenticated()")
@RestController
@CrossOrigin
public class AuthenticationController {

    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private UserDao userDao;
    ThreadSleepTryCatch threadSleepTryCatch = new ThreadSleepTryCatch();

    public AuthenticationController(TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder, UserDao userDao) {
        this.tokenProvider = tokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userDao = userDao;
    }
//    @PreAuthorize("permitAll")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginDTO loginDto) throws InterruptedException {

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.createToken(authentication, false);
        
        User user = userDao.findByUsername(loginDto.getUsername());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        threadSleepTryCatch.threadSleep();
        return new ResponseEntity<>(new LoginResponse(jwt, user), httpHeaders, HttpStatus.OK);
    }

//    @PreAuthorize("permitAll")
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public void register(@Valid @RequestBody RegisterUserDTO newUser) throws InterruptedException {
        try {
            User user = userDao.findByUsername(newUser.getUsername());
            throw new UserAlreadyExistsException();
        } catch (UsernameNotFoundException e) {
            userDao.create(newUser.getUsername(),newUser.getPassword(), newUser.getRole());
        }
        threadSleepTryCatch.threadSleep();
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class LoginResponse {

        private String token;
        private User user;

        LoginResponse(String token, User user) {
            this.token = token;
            this.user = user;
        }

        @JsonProperty("token")
        String getToken() {
            return token;
        }

        void setToken(String token) {
            this.token = token;
        }

        @JsonProperty("user")
		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}
    }
}

