package com.snw.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snw.WalletApplication;
import com.snw.controller.dto.FinancialDto;
import com.snw.controller.mapper.FinancialMapper;
import com.snw.controller.model.FinancialModel;
import com.snw.domain.FinancialEntity;
import com.snw.domain.enumeration.AccountingStatus;
import com.snw.exception.NoRecordFoundException;
import com.snw.repository.FinancialRepository;
import com.snw.service.FinancialService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.TransactionSystemException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@SpringBootTest(classes = WalletApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinancialControllerTest {

    protected final static ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private FinancialRepository repository;

    @Autowired
    private FinancialService service;

    @Autowired
    private FinancialMapper financialMapper;

    @Autowired
    private MockMvc mockMvc;

    String targetFinancialId;

    private static Stream<Arguments> financialGenerator() {
        return Stream.of(
                Arguments.of( AccountingStatus.CREDITOR, "user1", (new BigDecimal(12.5)).setScale(2)),
                Arguments.of(AccountingStatus.CREDITOR, "user2", (new BigDecimal(45)).setScale(2)),
                Arguments.of(AccountingStatus.CREDITOR, "user3", (new BigDecimal(18.5)).setScale(2))
        );
    }

    @ParameterizedTest
    @Order(1)
    @SneakyThrows
    @MethodSource("financialGenerator")
    @DisplayName("Create Record")
    void TestCreatingFinancialRecord_WhenCallingRestMethodPOST_PreAndPostRecordsAreIdentical(AccountingStatus status,
                                                                                             String user,
                                                                                             BigDecimal amount) {
        // Arrange
        List<FinancialEntity> entities;
        int databaseSizeBeforeCreate = repository.findAll().size();
        FinancialDto dto = FinancialDto
                .builder()
                .status(status)
                .user(user)
                .amount(amount)
                .build();

        // Act and Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/financial")
                        .content(mapper.writeValueAsBytes(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Assert
        entities = repository.findAll();
        assertThat(entities).hasSize(databaseSizeBeforeCreate + 1);

        FinancialEntity entity = entities.get(entities.size() - 1);
        assertEquals(entity.getAmount().compareTo(dto.getAmount()), 0);
        assertThat(entity.getUser()).isEqualTo(dto.getUser());
        assertThat(entity.getStatus()).isEqualTo(dto.getStatus());

        // Target the last record for update, delete, find test
        targetFinancialId = entity.getId();
    }

    @Test
    @Order(2)
    @SneakyThrows
    @DisplayName("Get All")
    void TestFetchingAllRecord_WhenThreeRecordsHasCreated_CreatedAndFetchedRecordsAreIdentical() {
        // Arrange
        List<FinancialModel> actualModels = null;
        List<FinancialModel> expectedModels =
                financialGenerator().map(item -> new FinancialModel(
                                null,
                                (AccountingStatus) item.get()[0],
                                (String) item.get()[1],
                                (BigDecimal) item.get()[2]))
                        .collect(Collectors.toList());

        // Act and Assert
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/financial"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        // Assert
        ObjectMapper objectMapper = new ObjectMapper();
        actualModels = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<FinancialModel>>() {});

        actualModels.forEach(item -> item.setId(null));
        assertEquals(expectedModels, actualModels);
    }

    @RepeatedTest(value = 2, name="{displayName}. Repetition {currentRepetition} of " +
            "{totalRepetitions}")
    @Order(2)
    @SneakyThrows
    @DisplayName("Lock Record")
    void TestLock_WhenFirstUserAccessRecord_FirstRecordWillGetLocked() {
        // Arrange
        String expectedExceptionMessage = "Could not roll back JPA transaction; nested exception is" +
                " org.hibernate.TransactionException: Unable to rollback against JDBC Connection";

        // Act: Lock entity in a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> future = executor.submit(() -> {
            assertDoesNotThrow(() -> {
                        service.getByIdAndLock(targetFinancialId);
                    },
                    () -> "Finding Financial with id=" + targetFinancialId + " should not throw any exception");
            return null;
        });

        Thread.sleep(1000);

        // Act & Assert
        TransactionSystemException actualException =
                assertThrows(TransactionSystemException.class,
                        () -> service.getByIdAndLock(targetFinancialId),
                        "Entity should throw TransactionSystemException by the second call");

        // Assert
        assertEquals(expectedExceptionMessage, actualException.getMessage(),
                "Lock exception message should be thrown");

        // Wait for the other thread to complete
        future.get();

        // Shut down the executor
        executor.shutdown();
        Thread.sleep(1000);
    }

    @Test
    @Order(4)
    @SneakyThrows
    @DisplayName("Update By Id")
    void TestUpdateById_WhenAnUpdatedDtoPassed_OnlyAmountAndUserFieldsShouldChange() {
        // Arrange
        Optional<FinancialEntity> entity;
        FinancialDto dto = FinancialDto
                .builder()
                .id(targetFinancialId)
                .status(AccountingStatus.DEBTOR)
                .user("user4")
                .amount(new BigDecimal(22).setScale(2))
                .build();

        FinancialEntity expectedEntity = FinancialEntity
                .builder()
                .id(targetFinancialId)
                .status(AccountingStatus.CREDITOR)
                .user("user4")
                .amount(new BigDecimal(22).setScale(2))
                .build();

        // Act and Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/financial/{id}", targetFinancialId)
                        .content(mapper.writeValueAsBytes(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
        entity = repository.findById(targetFinancialId);

        // Assert
        assertEquals(expectedEntity, entity.get());
        assertEquals(expectedEntity.getStatus(), entity.get().getStatus());
        assertNotEquals(financialMapper.toEntity(dto).getStatus(), entity.get().getStatus());
    }

    @Test
    @Order(5)
    @SneakyThrows
    @DisplayName("Update Without Id")
    void TestUpdate_WhenAnUpdatedDtoPassed_OnlyAmountAndUserFieldsShouldChange() {
        // Arrange
        Optional<FinancialEntity> entity;
        FinancialDto dto = FinancialDto
                .builder()
                .id(targetFinancialId)
                .status(AccountingStatus.DEBTOR)
                .user("user5")
                .amount(new BigDecimal(10.5).setScale(2))
                .build();

        FinancialEntity expectedEntity = FinancialEntity
                .builder()
                .id(targetFinancialId)
                .status(AccountingStatus.CREDITOR)
                .user("user5")
                .amount(new BigDecimal(10.5).setScale(2))
                .build();

        // Act and Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/financial")
                        .content(mapper.writeValueAsBytes(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
        entity = repository.findById(targetFinancialId);

        // Assert
        assertEquals(expectedEntity, entity.get());
        assertEquals(expectedEntity.getStatus(), entity.get().getStatus());
        assertNotEquals(financialMapper.toEntity(dto).getStatus(), entity.get().getStatus());
    }

    @Test
    @Order(6)
    @SneakyThrows
    @DisplayName("Partial Update With Legal Changes")
    void TestPartialUpdateById_WhenChangesPassed_AmountOrUserFieldsIsChanged() {
        // Arrange
        Optional<FinancialEntity> entity;
        Map<String, String> changes = Stream.of(new String[][] {
                { "user", "user6" },
                { "amount", "41.50" },
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        FinancialEntity expectedEntity = FinancialEntity
                .builder()
                .id(targetFinancialId)
                .status(AccountingStatus.CREDITOR)
                .user("user6")
                .amount(new BigDecimal(41.5).setScale(2))
                .build();

        // Act and Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/financial/{id}", targetFinancialId)
                        .content(mapper.writeValueAsBytes(changes))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
        entity = repository.findById(targetFinancialId);

        // Assert
        assertNotEquals(changes.get("id"), entity.get().getId());
        assertNotEquals(changes.get("status"), entity.get().getStatus());
        assertEquals(changes.get("user"), entity.get().getUser());
        assertEquals(changes.get("amount"), entity.get().getAmount().toString());
        assertEquals(expectedEntity, entity.get());
        assertEquals(expectedEntity.getStatus(), entity.get().getStatus());
    }

    @Test
    @Order(7)
    @SneakyThrows
    @DisplayName("Partial Update With Illegal Changes")
    void TestPartialUpdateById_WhenIllegalChangesPassed_ShouldGetBadRequestAndExpectEntityRollback() {
        // Arrange
        Optional<FinancialEntity> entity;
        Map<String, String> changes = Stream.of(new String[][] {
                { "id", targetFinancialId + "1" },
                { "status", AccountingStatus.DEBTOR.toString() },
                { "user", "user7" },
                { "amount", "38" },
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        // Entity will not change due to rollback (Bad Request)
        FinancialEntity expectedEntity = FinancialEntity
                .builder()
                .id(targetFinancialId)
                .status(AccountingStatus.CREDITOR)
                .user("user6")
                .amount(new BigDecimal(41.5).setScale(2))
                .build();

        // Act and Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/financial/{id}", targetFinancialId)
                        .content(mapper.writeValueAsBytes(changes))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        entity = repository.findById(targetFinancialId);

        // Assert
        assertEquals(expectedEntity, entity.get());
        assertEquals(expectedEntity.getStatus(), entity.get().getStatus());
    }

    @Test
    @Order(8)
    @SneakyThrows
    @DisplayName("Get By Correct Id")
    void TestGetById_WhenCorrectIdPassed_ExpectedOkResponse() {
        // Act and Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/financial/{id}", targetFinancialId))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(8)
    @SneakyThrows
    @DisplayName("Get By User")
    void TestGetByUser_WhenCorrectIdPassed_ExpectedOkResponse() {
        // Arrange
        List<FinancialEntity> entities;
        FinancialEntity expectedEntity = FinancialEntity
                .builder()
                .id(targetFinancialId)
                .status(AccountingStatus.CREDITOR)
                .user("user6")
                .amount(new BigDecimal(41.5).setScale(2))
                .build();

        // Act and Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/financial/user/{user}", "user6"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        entities = repository.findByUser("user6");

        // Assert
        assertThat(entities).hasSize(1);
        FinancialEntity entity = entities.get(0);
        assertEquals(expectedEntity, entity);
        assertEquals(expectedEntity.getStatus(), entity.getStatus());
    }

    @Test
    @Order(10)
    @SneakyThrows
    @DisplayName("Delete By Incorrect Id")
    void TestDeleteById_WhenIncorrectIdPassed_BadRequestReturnsAndRecordNotDeleted() {
        // Act
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/financial/{id}", targetFinancialId + "2"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        // Act and Assert
        assertDoesNotThrow(() -> {
                    service.getById(targetFinancialId);
                },
                () -> "Financial Record with id=" + targetFinancialId + " should be deleted");
    }

    @Test
    @Order(11)
    @SneakyThrows
    @DisplayName("Delete By Correct Id")
    void TestDeleteById_WhenCorrectIdPassed_NoContentReturnsAndRecordDeleted() {
        // Arrange
        String expectedExceptionMessage = "Financial entity not found";

        // Act
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/financial/{id}", targetFinancialId))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        // Act and Assert
        NoRecordFoundException actualException =
                assertThrows(NoRecordFoundException.class,
                        () -> service.getById(targetFinancialId),
                        "Entity should throw TransactionSystemException by the second call");

        // Assert
        assertEquals(expectedExceptionMessage, actualException.getMessage(),
                "Should Not Find Any Entity");
    }

    /*
     * TODO: PART 1: add test for RESTful APIs
     * create: •done
     * create with id
     * update: •done
     * update with out id: •done
     * partial update: •done
     * delete: •done
     * delete wrong id: •done
     * find by id: •done
     * find by user id: •done
     * */
}