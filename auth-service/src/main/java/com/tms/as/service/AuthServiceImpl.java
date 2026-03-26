package com.tms.as.service;


import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tms.as.config.RabbitMQConfig;
import com.tms.as.dto.AdminUpdateUserRequest;
import com.tms.as.dto.AuthResponse;
import com.tms.as.dto.LoginRequest;
import com.tms.as.dto.RegisterRequest;
import com.tms.as.dto.UpdateProfileRequest;
import com.tms.as.dto.UserResponse;
import com.tms.as.entity.Role;
import com.tms.as.entity.Status;
import com.tms.as.entity.User;
import com.tms.common.event.UserRegisteredEvent;
import com.tms.as.repository.UserRepository;
import com.tms.common.exception.ResourceAlreadyExistsException;
import com.tms.common.exception.ResourceNotFoundException;
import com.tms.common.exception.UnauthorizedException;
import com.tms.common.util.IdGeneratorUtil;
import com.tms.common.util.JwtUtil;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final IdGeneratorUtil idGeneratorUtil;
    private final RabbitTemplate rabbitTemplate;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           IdGeneratorUtil idGeneratorUtil,
                           RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.idGeneratorUtil = idGeneratorUtil;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already registered");
        }

        if (userRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new ResourceAlreadyExistsException("Employee code already exists");
        }

        User user = new User();
        user.setId(idGeneratorUtil.generateId("USR"));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setEmployeeCode(request.getEmployeeCode());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.EMPLOYEE);
        user.setStatus(Status.ACTIVE);

        User savedUser = userRepository.save(user);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                new UserRegisteredEvent(savedUser.getId(), savedUser.getFullName(), savedUser.getEmail())
        );

        return mapToUserResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == Status.INACTIVE) {
            throw new UnauthorizedException("Account is inactive");
        }

        if (user.getStatus() == Status.LOCKED) {
            throw new UnauthorizedException("Account is locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponse getProfile(String id, String loggedInEmail) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!loggedInUser.getId().equals(id) && loggedInUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("You can only view your own profile");
        }

        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmployeeCode(user.getEmployeeCode());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
    
    @Override
    public UserResponse updateProfile(String id, UpdateProfileRequest request, String loggedInEmail) {

        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!loggedInUser.getId().equals(id)) {
            throw new UnauthorizedException("You can only update your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already in use");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    @Override
    public UserResponse adminUpdateUser(String id, AdminUpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already in use");
        }

        if (!user.getEmployeeCode().equals(request.getEmployeeCode()) &&
                userRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new ResourceAlreadyExistsException("Employee code already in use");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setEmployeeCode(request.getEmployeeCode());

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    @Override
    public UserResponse assignManager(String id, String managerId) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        if (user.getId().equals(manager.getId())) {
            throw new UnauthorizedException("User cannot be assigned as their own manager");
        }

        if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Assigned manager must have MANAGER or ADMIN role");
        }

        user.setManagerId(manager.getId());

        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }
    
    @Override
    public String getManagerForEmployee(String employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return user.getManagerId();
    }
}
