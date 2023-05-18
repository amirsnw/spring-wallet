package com.snw.controller.mapper;


import com.snw.controller.dto.FinancialDto;
import com.snw.controller.model.FinancialModel;
import com.snw.domain.FinancialEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {})
public interface FinancialMapper {

    FinancialEntity toEntity(FinancialDto dto);

    List<FinancialEntity> toEntity(List<FinancialDto> dto);

    FinancialModel toModel(FinancialEntity entity);

    List<FinancialModel> toModel(List<FinancialEntity> entity);
}
