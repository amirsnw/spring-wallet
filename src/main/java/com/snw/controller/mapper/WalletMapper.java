package com.snw.controller.mapper;


import com.snw.controller.model.WalletModel;
import com.snw.domain.WalletEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {})
public interface WalletMapper {

    WalletModel toModel(WalletEntity entity);

    List<WalletModel> toModel(List<WalletEntity> entity);

}
