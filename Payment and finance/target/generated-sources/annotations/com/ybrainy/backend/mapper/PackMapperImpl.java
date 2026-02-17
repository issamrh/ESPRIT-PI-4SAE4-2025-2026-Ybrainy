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
    date = "2026-02-17T01:53:22+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.8 (Oracle Corporation)"
)
@Component
public class PackMapperImpl implements PackMapper {

    @Override
    public Pack toEntity(CreatePackDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Pack.PackBuilder pack = Pack.builder();

        pack.title( dto.getTitle() );
        pack.description( dto.getDescription() );
        pack.originalPrice( dto.getOriginalPrice() );
        pack.salePrice( dto.getSalePrice() );
        pack.level( dto.getLevel() );
        pack.durationHours( dto.getDurationHours() );
        pack.certificateName( dto.getCertificateName() );

        return pack.build();
    }

    @Override
    public void updateEntity(UpdatePackDTO dto, Pack entity) {
        if ( dto == null ) {
            return;
        }

        entity.setTitle( dto.getTitle() );
        entity.setDescription( dto.getDescription() );
        entity.setOriginalPrice( dto.getOriginalPrice() );
        entity.setSalePrice( dto.getSalePrice() );
        entity.setLevel( dto.getLevel() );
        entity.setDurationHours( dto.getDurationHours() );
        entity.setCertificateName( dto.getCertificateName() );
    }

    @Override
    public PackResponseDTO toResponseDTO(Pack entity) {
        if ( entity == null ) {
            return null;
        }

        PackResponseDTO.PackResponseDTOBuilder packResponseDTO = PackResponseDTO.builder();

        packResponseDTO.categoryId( entityCategoryId( entity ) );
        packResponseDTO.categoryName( entityCategoryName( entity ) );
        packResponseDTO.id( entity.getId() );
        packResponseDTO.title( entity.getTitle() );
        packResponseDTO.description( entity.getDescription() );
        packResponseDTO.originalPrice( entity.getOriginalPrice() );
        packResponseDTO.salePrice( entity.getSalePrice() );
        packResponseDTO.level( entity.getLevel() );
        packResponseDTO.durationHours( entity.getDurationHours() );
        packResponseDTO.certificateName( entity.getCertificateName() );
        packResponseDTO.status( entity.getStatus() );
        packResponseDTO.createdAt( entity.getCreatedAt() );
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
