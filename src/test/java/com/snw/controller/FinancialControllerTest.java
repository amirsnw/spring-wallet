package com.snw.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snw.WalletApplication;
import com.snw.controller.dto.FinancialDto;
import com.snw.domain.FinancialEntity;
import com.snw.domain.enumeration.AccountingStatus;
import com.snw.repository.FinancialRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@SpringBootTest(classes = WalletApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinancialControllerTest {

    protected final static ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private FinancialRepository repository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SneakyThrows
    void create() {
        int databaseSizeBeforeCreate = repository.findAll().size();
        FinancialDto dto = FinancialDto
                .builder()
                .amount(BigDecimal.valueOf(12.5))
                .status(AccountingStatus.CREDITOR)
                .user("user1")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/financial")
                        .content(mapper.writeValueAsBytes(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        List<FinancialEntity> entities = repository.findAll();
        assertThat(entities).hasSize(databaseSizeBeforeCreate + 1);
        FinancialEntity entity = entities.get(entities.size() - 1);
        assert entity.getAmount().compareTo(dto.getAmount()) == 0;
        assertThat(entity.getUser()).isEqualTo(dto.getUser());
        assertThat(entity.getStatus()).isEqualTo(dto.getStatus());
    }

    /*
     * TODO: PART 1: add test for RESTful APIs
     * create
     * create with id
     * update
     * update with out id
     * partial update
     * delete
     * delete wrong id
     * find by id
     * find by user id
     * */
}