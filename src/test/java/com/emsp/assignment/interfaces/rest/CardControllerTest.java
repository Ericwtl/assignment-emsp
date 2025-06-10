package com.emsp.assignment.interfaces.rest;

import com.emsp.assignment.domain.card.model.Card;
import com.emsp.assignment.domain.card.model.CardStatus;
import com.emsp.assignment.domain.card.service.CardStateService;
import com.emsp.assignment.infrastructure.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CardStateService cardStateService;

    @InjectMocks
    private CardController cardController;

    private Card validCard;
    private final String VALID_RFID = "RFID-123456";
    private final String VALID_ACCOUNT_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        // 配置 ObjectMapper 支持 Java 8 时间类型
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        mockMvc = MockMvcBuilders.standaloneSetup(cardController)
                .setControllerAdvice(exceptionHandler)
                .build();

        // 创建有效测试卡对象
        validCard = new Card();
        validCard.setRfidUid(VALID_RFID);
        validCard.setVisibleNumber("1234-5678-9012-3456");
        validCard.setStatus(CardStatus.CREATED);
    }

    // ================= 创建卡片接口测试 =================
    @Test
    void createCard_shouldReturnCreatedWhenValid() throws Exception {
        when(cardStateService.createCard(any(Card.class))).thenReturn(validCard);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCard)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rfidUid").value(VALID_RFID))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createCard_shouldReturnBadRequestWhenInvalidVisibleNumber() throws Exception {
        // 设置无效的卡号格式
        validCard.setVisibleNumber("invalid-number");

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCard)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_shouldReturnConflictWhenCardExists() throws Exception {
        String errorMessage = "卡号已存在";
        when(cardStateService.createCard(any(Card.class)))
                .thenThrow(new BusinessResponseException(HttpStatus.CONFLICT, errorMessage));

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCard)))
                .andDo(result -> {
                    // 打印完整响应信息
                    System.out.println("Status: " + result.getResponse().getStatus());
                    System.out.println("Content: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    // ================= 激活卡片接口测试 =================
    @Test
    void activateCard_shouldReturnOkWhenValid() throws Exception {
        mockMvc.perform(post("/api/cards/{rfidUid}/activate", VALID_RFID))
                .andExpect(status().isOk());
    }

    @Test
    void activateCard_shouldReturnNotFoundWhenCardNotExist() throws Exception {
        String errorMessage = "卡片不存在";
        doThrow(new CardNotFoundException(errorMessage))
                .when(cardStateService).activateCard(VALID_RFID);

        mockMvc.perform(post("/api/cards/{rfidUid}/activate", VALID_RFID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    @Test
    void activateCard_shouldReturnBadRequestWhenUnassigned() throws Exception {
        String errorMessage = "未分配卡片无法激活";
        doThrow(new IllegalStateException(errorMessage))
                .when(cardStateService).activateCard(VALID_RFID);

        mockMvc.perform(post("/api/cards/{rfidUid}/activate", VALID_RFID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    // ================= 停用卡片接口测试 =================
    @Test
    void deactivateCard_shouldReturnOkWhenValid() throws Exception {
        mockMvc.perform(post("/api/cards/{rfidUid}/deactivate", VALID_RFID))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateCard_shouldReturnNotFoundWhenCardNotExist() throws Exception {
        String errorMessage = "卡片不存在";
        doThrow(new CardNotFoundException(errorMessage))
                .when(cardStateService).deactivateCard(VALID_RFID);

        mockMvc.perform(post("/api/cards/{rfidUid}/deactivate", VALID_RFID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    // ================= 分配卡片接口测试 =================
    @Test
    void assignCardToAccount_shouldReturnAcceptedWhenValid() throws Exception {
        mockMvc.perform(put("/api/cards/{rfidUid}/assign", VALID_RFID)
                        .param("accountEmail", VALID_ACCOUNT_EMAIL))
                .andExpect(status().isAccepted());
    }

    @Test
    void assignCardToAccount_shouldReturnBadRequestWhenMissingParams() throws Exception {
        mockMvc.perform(put("/api/cards/{rfidUid}/assign", VALID_RFID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignCardToAccount_shouldReturnNotFoundWhenAccountNotExist() throws Exception {
        String errorMessage = "账户不存在";
        doThrow(new AccountNotFoundException(errorMessage))
                .when(cardStateService).assignCard(VALID_RFID, VALID_ACCOUNT_EMAIL);

        mockMvc.perform(put("/api/cards/{rfidUid}/assign", VALID_RFID)
                        .param("accountEmail", VALID_ACCOUNT_EMAIL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    @Test
    void assignCardToAccount_shouldReturnConflictWhenInvalidCardState() throws Exception {
        String errorMessage = "卡片状态无法分配";
        doThrow(new IllegalCardOperationException(errorMessage))
                .when(cardStateService).assignCard(VALID_RFID, VALID_ACCOUNT_EMAIL);

        mockMvc.perform(put("/api/cards/{rfidUid}/assign", VALID_RFID)
                        .param("accountEmail", VALID_ACCOUNT_EMAIL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    // ================= 更改卡片状态接口测试 =================
    @Test
    void changeCardStatus_shouldReturnAcceptedWhenValid() throws Exception {
        mockMvc.perform(put("/api/cards/{rfidUid}/status", VALID_RFID)
                        .param("newStatus", CardStatus.ACTIVATED.name())
                        .param("accountEmail", VALID_ACCOUNT_EMAIL))
                .andExpect(status().isAccepted());
    }

    @Test
    void changeCardStatus_shouldReturnBadRequestWhenMissingParams() throws Exception {
        mockMvc.perform(put("/api/cards/{rfidUid}/status", VALID_RFID)
                        .param("newStatus", CardStatus.ACTIVATED.name()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/cards/{rfidUid}/status", VALID_RFID)
                        .param("accountEmail", VALID_ACCOUNT_EMAIL))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeCardStatus_shouldReturnBadRequestWhenInvalidStatus() throws Exception {
        mockMvc.perform(put("/api/cards/{rfidUid}/status", VALID_RFID)
                        .param("newStatus", "INVALID_STATUS")
                        .param("accountEmail", VALID_ACCOUNT_EMAIL))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeCardStatus_shouldReturnConflictWhenInvalidTransition() throws Exception {
        String errorMessage = "无效的状态转换";
        doThrow(new IllegalCardOperationException(errorMessage))
                .when(cardStateService).changeCardStatus(VALID_RFID, CardStatus.ACTIVATED, VALID_ACCOUNT_EMAIL);

        mockMvc.perform(put("/api/cards/{rfidUid}/status", VALID_RFID)
                        .param("newStatus", CardStatus.ACTIVATED.name())
                        .param("accountEmail", VALID_ACCOUNT_EMAIL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    @Test
    void changeCardStatus_shouldReturnForbiddenWhenUnauthorized() throws Exception {
        String errorMessage = "无权操作此卡片";
        doThrow(new BusinessResponseException(HttpStatus.FORBIDDEN, errorMessage))
                .when(cardStateService).changeCardStatus(VALID_RFID, CardStatus.ACTIVATED, VALID_ACCOUNT_EMAIL);

        mockMvc.perform(put("/api/cards/{rfidUid}/status", VALID_RFID)
                        .param("newStatus", CardStatus.ACTIVATED.name())
                        .param("accountEmail", VALID_ACCOUNT_EMAIL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }
}