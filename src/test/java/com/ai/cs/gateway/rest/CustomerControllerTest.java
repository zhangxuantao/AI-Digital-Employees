package com.ai.cs.gateway.rest;

import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.customer.repository.CustomerProfileRepository;
import com.ai.cs.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(authorities = "im:access")
@ActiveProfiles("test")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerProfileRepository customerRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private CustomerProfile createTestCustomer(Long id, String nickname, String platform) {
        CustomerProfile cp = new CustomerProfile();
        cp.setId(id);
        cp.setNickname(nickname);
        cp.setPlatform(platform);
        cp.setOpenid("openid_" + id);
        return cp;
    }

    @Test
    void list_shouldReturnPaginatedCustomers() throws Exception {
        List<CustomerProfile> customers = List.of(
                createTestCustomer(1L, "张三", "WEB"),
                createTestCustomer(2L, "李四", "XIAOHONGSHU")
        );
        PageImpl<CustomerProfile> page = new PageImpl<>(customers);
        when(customerRepository.findAll(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/customers")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].nickname").value("张三"))
                .andExpect(jsonPath("$.data.records[1].id").value(2))
                .andExpect(jsonPath("$.data.records[1].nickname").value("李四"))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void get_shouldReturnCustomerById() throws Exception {
        CustomerProfile cp = createTestCustomer(1L, "张三", "WEB");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(cp));

        mockMvc.perform(get("/api/v1/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nickname").value("张三"));
    }

    @Test
    void get_shouldReturnNullWhenNotFound() throws Exception {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/customers/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void update_shouldUpdateCustomerFields() throws Exception {
        CustomerProfile existing = createTestCustomer(1L, "张三", "WEB");
        existing.setPhone(null);
        existing.setEmail(null);

        CustomerProfile updated = createTestCustomer(1L, "张三(已修改)", "WEB");
        updated.setPhone("13800138000");
        updated.setEmail("test@example.com");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"张三(已修改)\",\"phone\":\"13800138000\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("张三(已修改)"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void update_shouldReturnErrorWhenCustomerNotFound() throws Exception {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/customers/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"新名字\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("客户不存在"));
    }

    @Test
    void update_shouldUpdateTagsAndExtraFields() throws Exception {
        CustomerProfile existing = createTestCustomer(1L, "张三", "WEB");

        CustomerProfile updated = createTestCustomer(1L, "张三", "WEB");
        updated.setTags("[\"vip\",\"premium\"]");
        updated.setExtraFields("{\"age\":\"30\"}");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":\"[\\\"vip\\\",\\\"premium\\\"]\",\"extraFields\":\"{\\\"age\\\":\\\"30\\\"}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void list_shouldUseDefaultPagination() throws Exception {
        when(customerRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
