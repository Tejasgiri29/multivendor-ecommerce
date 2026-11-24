package com.example.shop.service;

import com.example.shop.dto.PageResponse;
import com.example.shop.dto.UserDto;
import com.example.shop.entity.Role;
import com.example.shop.entity.User;
import com.example.shop.exception.BadRequestException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> listUsers(int page, int size) {
        return PageResponse.of(userRepository.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("id"))), DtoMapper::toDto);
    }

    @Transactional
    public UserDto setEnabled(Long adminId, Long userId, boolean enabled) {
        User u = find(userId);
        if (u.getId().equals(adminId) && !enabled) {
            throw new BadRequestException("You cannot disable your own account");
        }
        u.setEnabled(enabled);
        return DtoMapper.toDto(u);
    }

    @Transactional
    public UserDto approveSeller(Long userId) {
        User u = find(userId);
        if (u.getRole() != Role.SELLER) {
            throw new BadRequestException("User is not a seller");
        }
        u.setSellerApproved(true);
        return DtoMapper.toDto(u);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
