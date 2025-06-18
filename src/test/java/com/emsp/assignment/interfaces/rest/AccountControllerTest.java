package com.emsp.assignment.interfaces.rest;

import com.emsp.assignment.application.AccountService;
import com.emsp.assignment.domain.account.model.Account;
import com.emsp.assignment.domain.account.model.AccountStatus;
import com.emsp.assignment.domain.account.service.AccountStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import(AccountControllerTest.MockConfig.class) // 导入自定义的Mock配置
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountStateService accountStateService;

    // 自定义Mock配置类
    @TestConfiguration
    static class MockConfig {
        @Bean
        public AccountService accountService() {
            return mock(AccountService.class);
        }

        @Bean
        public AccountStateService accountStateService() {
            return mock(AccountStateService.class);
        }
    }

    // 测试数据
    private Account createTestAccount() {
        Account account = new Account();
        account.setEmail("test@example.com");
        account.setContractId("AB123XYZ456789");
        account.setStatus(AccountStatus.CREATED);
        account.setLastUpdated(LocalDateTime.now());
        return account;
    }

    @AfterEach
    void resetMocks() {
        // 重置所有mock对象的调用计数和存根配置
        Mockito.reset(accountService);
        Mockito.reset(accountStateService);
    }

    // 1. 测试创建账户
    @Test
    void createAccount_shouldReturnCreatedStatus() throws Exception {
        Account testAccount = createTestAccount();

        given(accountService.createAccount(any(Account.class))).willReturn(testAccount);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAccount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(accountService, times(1)).createAccount(any(Account.class));
    }

    // 2. 测试更改账户状态
    @Test
    void changeAccountStatus_shouldReturnAcceptedStatus() throws Exception {
        String email = "test@example.com";
        AccountStatus newStatus = AccountStatus.ACTIVATED;
        String contractId = "AB123XYZ456789";

        // 准备模拟返回的 Account 对象
        Account mockAccount = new Account();
        mockAccount.setEmail(email);
        mockAccount.setStatus(newStatus);
        mockAccount.setContractId(contractId);

        // 模拟 service 返回更新后的 Account
        when(accountService.changeAccountStatus(anyString(), any(AccountStatus.class), anyString()))
                .thenReturn(mockAccount);

        mockMvc.perform(put("/api/accounts/{email}/status", email)
                        .param("newStatus", newStatus.name())
                        .param("contractId", contractId))
                .andExpect(status().isAccepted())  // 改为 200 OK
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value(newStatus.name()))
                .andExpect(jsonPath("$.contractId").value(contractId));

        verify(accountService, times(1))
                .changeAccountStatus(eq(email), eq(newStatus), eq(contractId));
    }

    // 3. 测试分页查询账户
    @Test
    void getAccountsByUpdateTime_shouldReturnPageOfAccounts() throws Exception {
        Account testAccount = createTestAccount();
        List<Account> accounts = Collections.singletonList(testAccount);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Account> page = new PageImpl<>(accounts, pageable, 1);

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        given(accountService.getAccountsWithCardsByLastUpdated(eq(start), eq(end), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/api/accounts")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(accountService, times(1))
                .getAccountsWithCardsByLastUpdated(eq(start), eq(end), any(Pageable.class));
    }

    // 4. 测试激活账户
    @Test
    void activateAccount_shouldReturnOkStatus() throws Exception {
        String email = "test@example.com";

        doNothing().when(accountStateService).activateAccount(anyString());

        mockMvc.perform(post("/api/accounts/{email}/activate", email))
                .andExpect(status().isOk());

        verify(accountStateService, times(1)).activateAccount(eq(email));
    }

    // 5. 测试停用账户
    @Test
    void deactivateAccount_shouldReturnOkStatus() throws Exception {
        String email = "test@example.com";

        doNothing().when(accountStateService).deactivateAccount(anyString());

        mockMvc.perform(post("/api/accounts/{email}/deactivate", email))
                .andExpect(status().isOk());

        verify(accountStateService, times(1)).deactivateAccount(eq(email));
    }

    // 6. 测试创建账户 - 验证失败场景（无效输入）
    @Test
    void createAccount_shouldReturnBadRequestForInvalidInput() throws Exception {
        Account invalidAccount = new Account();
        invalidAccount.setEmail("invalid-email"); // 无效邮箱格式

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAccount)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(accountService, times(0)).createAccount(any());
    }

    // 7. 测试更改账户状态 - 验证失败场景（无效状态）
    @Test
    void changeAccountStatus_shouldReturnBadRequestForInvalidStatus() throws Exception {
        String email = "test@example.com";
        String invalidStatus = "INVALID_STATUS";

        mockMvc.perform(put("/api/accounts/{email}/status", email)
                        .param("newStatus", invalidStatus))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Valid values are: CREATED, ACTIVATED, DEACTIVATED")));

        verify(accountService, never()).changeAccountStatus(any(), any(), any());
    }

    // 8. 测试更改账户状态 - 边界情况（空状态）
    @Test
    void changeAccountStatus_shouldReturnBadRequestForEmptyStatus() throws Exception {
        String email = "test@example.com";

        mockMvc.perform(put("/api/accounts/{email}/status", email)
                        .param("newStatus", "")) // 空状态值
                .andExpect(status().isBadRequest());

        verify(accountService, never()).changeAccountStatus(any(), any(), any());
    }

    // 9. 测试更改账户状态 - 边界情况（null状态）
    @Test
    void changeAccountStatus_shouldReturnBadRequestForNullStatus() throws Exception {
        String email = "test@example.com";

        mockMvc.perform(put("/api/accounts/{email}/status", email))
                // 不提供 newStatus 参数
                .andExpect(status().isBadRequest());

        verify(accountService, never()).changeAccountStatus(any(), any(), any());
    }
}