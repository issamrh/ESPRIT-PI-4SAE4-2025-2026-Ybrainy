package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.cart.CartHistoryResponseDTO;
import com.ybrainy.backend.dto.cart.CartItemResponseDTO;
import com.ybrainy.backend.dto.cart.CartResponseDTO;
import com.ybrainy.backend.entity.Cart;
import com.ybrainy.backend.entity.CartHistory;
import com.ybrainy.backend.entity.CartItem;
import com.ybrainy.backend.entity.Pack;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-17T03:06:37+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
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
        cartResponseDTO.setItems( cartItemListToCartItemResponseDTOList( cart.getItems() ) );
        cartResponseDTO.setStatus( cart.getStatus() );
        cartResponseDTO.setTotalAmount( cart.getTotalAmount() );
        cartResponseDTO.setUserId( cart.getUserId() );

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

    @Override
    public CartHistoryResponseDTO toCartHistoryResponseDTO(CartHistory history) {
        if ( history == null ) {
            return null;
        }

        CartHistoryResponseDTO cartHistoryResponseDTO = new CartHistoryResponseDTO();

        cartHistoryResponseDTO.setAction( history.getAction() );
        cartHistoryResponseDTO.setCartId( history.getCartId() );
        cartHistoryResponseDTO.setCartItemId( history.getCartItemId() );
        cartHistoryResponseDTO.setCartStatus( history.getCartStatus() );
        cartHistoryResponseDTO.setCreatedAt( history.getCreatedAt() );
        cartHistoryResponseDTO.setDescription( history.getDescription() );
        cartHistoryResponseDTO.setId( history.getId() );
        cartHistoryResponseDTO.setPackTitle( history.getPackTitle() );
        cartHistoryResponseDTO.setQuantity( history.getQuantity() );
        cartHistoryResponseDTO.setTotalAmount( history.getTotalAmount() );

        return cartHistoryResponseDTO;
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
