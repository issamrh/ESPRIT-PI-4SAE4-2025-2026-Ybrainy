package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.cart.CartItemResponseDTO;
import com.ybrainy.backend.dto.cart.CartResponseDTO;
import com.ybrainy.backend.entity.Cart;
import com.ybrainy.backend.entity.CartItem;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-17T01:50:38+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class CartMapperImpl extends CartMapper {

    @Override
    public CartResponseDTO toCartResponseDTO(Cart cart) {
        if ( cart == null ) {
            return null;
        }

        CartResponseDTO cartResponseDTO = new CartResponseDTO();

        cartResponseDTO.setId( cart.getId() );
        cartResponseDTO.setUserId( cart.getUserId() );
        cartResponseDTO.setStatus( cart.getStatus() );
        cartResponseDTO.setTotalAmount( cart.getTotalAmount() );
        cartResponseDTO.setItems( cartItemListToCartItemResponseDTOList( cart.getItems() ) );

        return cartResponseDTO;
    }

    @Override
    public CartItemResponseDTO toCartItemResponseDTO(CartItem item) {
        if ( item == null ) {
            return null;
        }

        CartItemResponseDTO cartItemResponseDTO = new CartItemResponseDTO();

        cartItemResponseDTO.setId( item.getId() );
        cartItemResponseDTO.setPackId( item.getPackId() );
        cartItemResponseDTO.setPriceAtPurchase( item.getPriceAtPurchase() );
        cartItemResponseDTO.setQuantity( item.getQuantity() );
        cartItemResponseDTO.setSubtotal( item.getSubtotal() );

        cartItemResponseDTO.setPackTitle( getPackTitle(item.getPackId()) );

        return cartItemResponseDTO;
    }

    protected List<CartItemResponseDTO> cartItemListToCartItemResponseDTOList(List<CartItem> list) {
        if ( list == null ) {
            return null;
        }

        List<CartItemResponseDTO> list1 = new ArrayList<CartItemResponseDTO>( list.size() );
        for ( CartItem cartItem : list ) {
            list1.add( toCartItemResponseDTO( cartItem ) );
        }

        return list1;
    }
}
