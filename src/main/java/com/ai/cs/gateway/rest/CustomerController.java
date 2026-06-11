package com.ai.cs.gateway.rest;

import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.customer.repository.CustomerProfileRepository;
import com.ai.cs.shared.dto.ApiResponse;
import com.ai.cs.shared.dto.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('im:access')")
public class CustomerController {

    private final CustomerProfileRepository customerRepository;

    @GetMapping
    public ApiResponse<PageResult<CustomerProfile>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        var pageResult = customerRepository.findAll(PageRequest.of(page - 1, pageSize));
        return ApiResponse.success(PageResult.of(
                pageResult.getContent(), pageResult.getTotalElements(), page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerProfile> get(@PathVariable Long id) {
        return ApiResponse.success(customerRepository.findById(id).orElse(null));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerProfile> update(@PathVariable Long id, @RequestBody CustomerProfile update) {
        CustomerProfile cp = customerRepository.findById(id).orElse(null);
        if (cp == null) return ApiResponse.error(404, "客户不存在");
        if (update.getNickname() != null) cp.setNickname(update.getNickname());
        if (update.getPhone() != null) cp.setPhone(update.getPhone());
        if (update.getEmail() != null) cp.setEmail(update.getEmail());
        if (update.getTags() != null) cp.setTags(update.getTags());
        if (update.getExtraFields() != null) cp.setExtraFields(update.getExtraFields());
        return ApiResponse.success(customerRepository.save(cp));
    }
}
