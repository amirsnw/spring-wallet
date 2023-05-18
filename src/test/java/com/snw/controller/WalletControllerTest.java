package com.snw.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snw.controller.dto.FinancialDto;
import com.snw.domain.WalletEntity;
import com.snw.domain.enumeration.AccountingStatus;
import com.snw.repository.WalletRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AutoConfigureMockMvc
@SpringBootTest(classes = com.snw.WalletApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WalletControllerTest {

    protected final static ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private WalletRepository repository;

    @Autowired
    private MockMvc mockMvc;

    private static List<FinancialDto> getSadEndingLowBoundaryBeforeInsertInputList() {
        return List.of(
                FinancialDto.builder().status(AccountingStatus.CREDITOR).user("user1")
                        .amount((new BigDecimal(12.5)).setScale(2)).build(),
                FinancialDto.builder().status(AccountingStatus.DEBTOR).user("user1")
                        .amount((new BigDecimal(45)).setScale(2)).build(),
                FinancialDto.builder().status(AccountingStatus.CREDITOR).user("user2")
                        .amount((new BigDecimal(18.5)).setScale(2)).build()
        );
    }

    private static List<FinancialDto> getSadEndingMoreThan1000InputList() {
        return List.of(
                FinancialDto.builder().status(AccountingStatus.CREDITOR).user("user1")
                        .amount((new BigDecimal(12.5)).setScale(2)).build(),
                FinancialDto.builder().status(AccountingStatus.DEBTOR).user("user1")
                        .amount((new BigDecimal(1001)).setScale(2)).build(),
                FinancialDto.builder().status(AccountingStatus.CREDITOR).user("user2")
                        .amount((new BigDecimal(18.5)).setScale(2)).build()
        );
    }

    private static List<FinancialDto> getHappyEndingInputList() {
        return List.of(
                FinancialDto.builder().status(AccountingStatus.CREDITOR).user("user1")
                        .amount((new BigDecimal(445)).setScale(2)).build(),
                FinancialDto.builder().status(AccountingStatus.DEBTOR).user("user1")
                        .amount((new BigDecimal(112.5)).setScale(2)).build(),
                FinancialDto.builder().status(AccountingStatus.CREDITOR).user("user2")
                        .amount((new BigDecimal(500)).setScale(2)).build()
        );
    }

    private static List<FinancialDto> getSadEndingLowBoundaryAfterInsertInputList() {
        return List.of(
                FinancialDto.builder().status(AccountingStatus.CREDITOR).user("user1")
                        .amount((new BigDecimal(50)).setScale(2)).build(),
                FinancialDto.builder().status(AccountingStatus.DEBTOR).user("user2")
                        .amount((new BigDecimal(501)).setScale(2)).build()
        );
    }

    private static List<FinancialDto> getSadEndingHighBoundaryInputList() {
        int index = 0;
        List<FinancialDto> inputList = new ArrayList<>();

        while (index < 10000) {
            inputList.add(FinancialDto.builder().status(AccountingStatus.CREDITOR).user("user1")
                    .amount((new BigDecimal(100)).setScale(2)).build());
            index++;
        }
        inputList.add(FinancialDto.builder().status(AccountingStatus.CREDITOR).user("user1")
                .amount((new BigDecimal(50)).setScale(2)).build());
        inputList.add(FinancialDto.builder().status(AccountingStatus.DEBTOR).user("user1")
                .amount((new BigDecimal(49)).setScale(2)).build());
        return inputList;
    }

    private static List<FinancialDto> getSadEndingLowHighBoundaryInputlList() {
        List<FinancialDto> inputList = getSadEndingHighBoundaryInputList();
        inputList.add(FinancialDto.builder().status(AccountingStatus.DEBTOR).user("user2")
                        .amount((new BigDecimal(800)).setScale(2)).build());
        return inputList;
    }

    @Test
    @Order(1)
    @SneakyThrows
    @DisplayName("Negative Sum Constraint Before Initialize")
    void TestAddingCreditToWallet_WhenSumIsNegativeInitialInsert_ShouldReturnBadRequestWithDetails() {
        // Arrange
        Map<String, List<String>> constraintErrorMap = null;
        List<FinancialDto> financialDtos = getSadEndingLowBoundaryBeforeInsertInputList();
        String expectedErrorMessage = "Record amount constraints minimum boundary (0)";

        // Act and Assert
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallet")
                        .content(mapper.writeValueAsBytes(financialDtos))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andReturn();

        constraintErrorMap = mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, List<String>>>() {});
        assertThat(constraintErrorMap.keySet().size()).isEqualTo(1);
        assertThat(constraintErrorMap.get("user1").size()).isEqualTo(1);
        assertEquals(expectedErrorMessage, constraintErrorMap.get("user1").get(0));
    }

    @Test
    @Order(2)
    @SneakyThrows
    @DisplayName("No Financial More Than 1000")
    void TestAddingCreditToWallet_WhenThereIsOneOrMoreFinancialMoreThan1000_ShouldReturnBadRequestWithDetails() {
        // Arrange
        List<FinancialDto> financialDtos = getSadEndingMoreThan1000InputList();
        String expectedErrorMessage = "Record amount cannot be greater than 1000";

        // Act and Assert
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallet")
                        .content(mapper.writeValueAsBytes(financialDtos))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andReturn();

        assertEquals(expectedErrorMessage, result.getResolvedException().getMessage());
    }

    @Test
    @Order(3)
    @SneakyThrows
    @DisplayName("Initialize Wallet For Two Users")
    void TestInitializingWalletCredit_WhenAmountsAreOk_ShouldReturnOkAndWalletIsCreated() {
        // Arrange
        List<WalletEntity> entities;
        WalletEntity user1Wallet;
        WalletEntity user2Wallet;
        List<FinancialDto> financialDtos = getHappyEndingInputList();

        // Act and Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallet")
                        .content(mapper.writeValueAsBytes(financialDtos))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        entities = repository.findAll();
        assertThat(entities).hasSize(2);

        user1Wallet = entities.get(0);
        user2Wallet = entities.get(1);

        assertEquals("user1", user1Wallet.getUser());
        assertEquals("user2", user2Wallet.getUser());

        assertEquals((new BigDecimal(500)).setScale(2), user2Wallet.getCredit());
    }

    @Test
    @Order(4)
    @SneakyThrows
    @DisplayName("Negative Sum Constraint After Initialize")
    void TestAddingCreditToWallet_WhenSumIsNegativeAfterInitialInsert_ShouldReturnBadRequestWithDetails() {
        // Arrange
        Map<String, List<String>> constraintErrorMap = null;
        List<FinancialDto> financialDtos = getSadEndingLowBoundaryAfterInsertInputList();
        String expectedErrorMessage = "Record amount constraints minimum boundary (0)";

        // Act and Assert
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallet")
                        .content(mapper.writeValueAsBytes(financialDtos))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andReturn();

        constraintErrorMap = mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, List<String>>>() {});
        assertThat(constraintErrorMap.keySet().size()).isEqualTo(1);
        assertThat(constraintErrorMap.get("user2").size()).isEqualTo(1);
        assertEquals(expectedErrorMessage, constraintErrorMap.get("user2").get(0));
    }

    @Test
    @Order(5)
    @SneakyThrows
    @DisplayName("Million Sum Constraint")
    void TestAddingCreditToWallet_WhenSumIsMoreThan1Million_ShouldReturnBadRequestWithDetails() {
        // Arrange
        Map<String, List<String>> constraintErrorMap = null;
        List<FinancialDto> financialDtos = getSadEndingHighBoundaryInputList();
        String expectedErrorMessage = "Record amount constraints maximum boundary (1,000,000)";

        // Act and Assert
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallet")
                        .content(mapper.writeValueAsBytes(financialDtos))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andReturn();

        constraintErrorMap = mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, List<String>>>() {});
        assertThat(constraintErrorMap.keySet().size()).isEqualTo(1);
        assertThat(constraintErrorMap.get("user1").size()).isEqualTo(3);
        assertEquals(expectedErrorMessage, constraintErrorMap.get("user1").get(0));
    }

    @Test
    @Order(6)
    @SneakyThrows
    @DisplayName("Two Constraint Happened The Same Time")
    void TestAddingCreditToWallet_WhenLowAndHighConstraintHappen_ShouldReturnBadRequestWithDetails() {
        // Arrange
        Map<String, List<String>> constraintErrorMap = null;
        List<FinancialDto> financialDtos = getSadEndingLowHighBoundaryInputlList();
        String expectedErrorMessage1 = "Record amount constraints maximum boundary (1,000,000)";
        String expectedErrorMessage2 = "Record amount constraints minimum boundary (0)";

        // Act and Assert
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallet")
                        .content(mapper.writeValueAsBytes(financialDtos))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andReturn();

        constraintErrorMap = mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, List<String>>>() {});
        assertThat(constraintErrorMap.keySet().size()).isEqualTo(2);
        assertThat(constraintErrorMap.get("user1").size()).isEqualTo(3);
        assertThat(constraintErrorMap.get("user2").size()).isEqualTo(1);
        assertEquals(expectedErrorMessage1, constraintErrorMap.get("user1").get(0));
        assertEquals(expectedErrorMessage2, constraintErrorMap.get("user2").get(0));
    }

    @Test
    @Order(7)
    @SneakyThrows
    @DisplayName("Add More Credit To Wallet")
    void TestAddingMoreCreditToWallet_WhenAmountsAreOk_ShouldReturnOk() {
        // Arrange
        List<FinancialDto> financialDtos = getHappyEndingInputList();

        // Act and Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallet")
                        .content(mapper.writeValueAsBytes(financialDtos))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    @Order(8)
    @SneakyThrows
    @DisplayName("Get Balance")
    void TestGettingWalletBalance_WhenFinancialRecordsHaveCommitted_ShouldReturnOkIncludesBalance() {
        // Act and Assert
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/wallet/{user}", "user1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/wallet/{user}", "user2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        assertEquals("665.00", result.getResponse().getContentAsString());
        assertEquals("1000.00", result2.getResponse().getContentAsString());
    }
}