package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.cart.CartItemResponseDTO;
import com.ybrainy.backend.dto.cart.CartResponseDTO;
import com.ybrainy.backend.entity.Cart;
import com.ybrainy.backend.entity.CartItem;
import com.ybrainy.backend.repository.PackRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class CartMapper {

    @Autowired
    protected PackRepository packRepository;

    public abstract CartResponseDTO toCartResponseDTO(Cart cart);

    @Mapping(target = "packTitle", expression = "java(getPackTitle(item.getPackId()))")
    public abstract CartItemResponseDTO toCartItemResponseDTO(CartItem item);

    protected String getPackTitle(Long packId) {
        return packRepository.findById(packId)
                .map(p -> p.getTitle())
                .orElse("Unknown Pack");
    }
}
