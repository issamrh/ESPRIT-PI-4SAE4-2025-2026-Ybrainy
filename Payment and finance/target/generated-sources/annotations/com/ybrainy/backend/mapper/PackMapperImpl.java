package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.pack.CreatePackDTO;
import com.ybrainy.backend.dto.pack.PackResponseDTO;
import com.ybrainy.backend.dto.pack.UpdatePackDTO;
import com.ybrainy.backend.entity.Pack;
import com.ybrainy.backend.entity.PackCategory;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-16T23:33:31+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class PackMapperImpl implements PackMapper {

    @Override
    public Pack toEntity(CreatePackDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Pack.PackBuilder pack = Pack.builder();

        pack.certificateName( dto.getCertificateName() );
        pack.description( dto.getDescription() );
        pack.durationHours( dto.getDurationHours() );
        pack.level( dto.getLevel() );
        pack.originalPrice( dto.getOriginalPrice() );
        pack.salePrice( dto.getSalePrice() );
        pack.title( dto.getTitle() );

        return pack.build();
    }

    @Override
    public void updateEntity(UpdatePackDTO dto, Pack entity) {
        if ( dto == null ) {
            return;
        }

        entity.setCertificateName( dto.getCertificateName() );
        entity.setDescription( dto.getDescription() );
        entity.setDurationHours( dto.getDurationHours() );
        entity.setLevel( dto.getLevel() );
        entity.setOriginalPrice( dto.getOriginalPrice() );
        entity.setSalePrice( dto.getSalePrice() );
        entity.setTitle( dto.getTitle() );
    }

    @Override
    public PackResponseDTO toResponseDTO(Pack entity) {
        if ( entity == null ) {
            return null;
        }

        PackResponseDTO.PackResponseDTOBuilder packResponseDTO = PackResponseDTO.builder();

        packResponseDTO.categoryId( entityCategoryId( entity ) );
        packResponseDTO.categoryName( entityCategoryName( entity ) );
        packResponseDTO.certificateName( entity.getCertificateName() );
        packResponseDTO.createdAt( entity.getCreatedAt() );
        packResponseDTO.description( entity.getDescription() );
        packResponseDTO.durationHours( entity.getDurationHours() );
        packResponseDTO.id( entity.getId() );
        packResponseDTO.level( entity.getLevel() );
        packResponseDTO.originalPrice( entity.getOriginalPrice() );
        packResponseDTO.salePrice( entity.getSalePrice() );
        packResponseDTO.status( entity.getStatus() );
        packResponseDTO.title( entity.getTitle() );
        packResponseDTO.updatedAt( entity.getUpdatedAt() );

        return packResponseDTO.build();
    }

    private Long entityCategoryId(Pack pack) {
        if ( pack == null ) {
            return null;
        }
        PackCategory category = pack.getCategory();
        if ( category == null ) {
            return null;
        }
        Long id = category.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String entityCategoryName(Pack pack) {
        if ( pack == null ) {
            return null;
        }
        PackCategory category = pack.getCategory();
        if ( category == null ) {
            return null;
        }
        String name = category.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
