package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.cart.CartHistoryResponseDTO;
import com.ybrainy.backend.dto.cart.CartItemResponseDTO;
import com.ybrainy.backend.dto.cart.CartResponseDTO;
import com.ybrainy.backend.entity.CartHistory;
import com.ybrainy.backend.entity.Cart;
import com.ybrainy.backend.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    CartResponseDTO toCartResponseDTO(Cart cart);

    @Mapping(target = "packId", source = "pack.id")
    @Mapping(target = "packTitle", source = "pack.title")
    CartItemResponseDTO toCartItemResponseDTO(CartItem item);

    CartHistoryResponseDTO toCartHistoryResponseDTO(CartHistory history);
}
