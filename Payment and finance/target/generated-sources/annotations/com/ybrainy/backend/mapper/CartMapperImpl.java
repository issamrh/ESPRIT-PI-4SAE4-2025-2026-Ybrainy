package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.cart.CartItemResponseDTO;
import com.ybrainy.backend.dto.cart.CartResponseDTO;
import com.ybrainy.backend.entity.Cart;
import com.ybrainy.backend.entity.CartItem;
import com.ybrainy.backend.entity.Pack;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-17T02:07:26+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.8 (Oracle Corporation)"
)
@Component
public class CartMapperImpl implements CartMapper {

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

        cartItemResponseDTO.setPackId( itemPackId( item ) );
        cartItemResponseDTO.setPackTitle( itemPackTitle( item ) );
        cartItemResponseDTO.setId( item.getId() );
        cartItemResponseDTO.setPriceAtPurchase( item.getPriceAtPurchase() );
        cartItemResponseDTO.setQuantity( item.getQuantity() );
        cartItemResponseDTO.setSubtotal( item.getSubtotal() );

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

    private Long itemPackId(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        Pack pack = cartItem.getPack();
        if ( pack == null ) {
            return null;
        }
        Long id = pack.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String itemPackTitle(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        Pack pack = cartItem.getPack();
        if ( pack == null ) {
            return null;
        }
        String title = pack.getTitle();
        if ( title == null ) {
            return null;
        }
        return title;
    }
}
