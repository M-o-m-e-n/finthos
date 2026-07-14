package com.alahly.momkn.finthos.wallet.mapper;

import com.alahly.momkn.finthos.wallet.domain.Wallet;
import com.alahly.momkn.finthos.wallet.web.dto.WalletResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    WalletResponse toResponse(Wallet wallet);
}
