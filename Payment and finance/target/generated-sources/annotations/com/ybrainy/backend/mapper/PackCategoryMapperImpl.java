package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.packcategory.CreatePackCategoryDTO;
import com.ybrainy.backend.dto.packcategory.PackCategoryResponseDTO;
import com.ybrainy.backend.dto.packcategory.UpdatePackCategoryDTO;
import com.ybrainy.backend.entity.PackCategory;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-17T03:06:36+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class PackCategoryMapperImpl implements PackCategoryMapper {

    @Override
    public PackCategory toEntity(CreatePackCategoryDTO dto) {
        if ( dto == null ) {
            return null;
        }

        PackCategory.PackCategoryBuilder packCategory = PackCategory.builder();

        packCategory.description( dto.getDescription() );
        packCategory.icon( dto.getIcon() );
        packCategory.name( dto.getName() );

        return packCategory.build();
    }

    @Override
    public void updateEntity(UpdatePackCategoryDTO dto, PackCategory entity) {
        if ( dto == null ) {
            return;
        }

        entity.setDescription( dto.getDescription() );
        entity.setIcon( dto.getIcon() );
        entity.setName( dto.getName() );
    }

    @Override
    public PackCategoryResponseDTO toResponseDTO(PackCategory entity) {
        if ( entity == null ) {
            return null;
        }

        PackCategoryResponseDTO.PackCategoryResponseDTOBuilder packCategoryResponseDTO = PackCategoryResponseDTO.builder();

        packCategoryResponseDTO.createdAt( entity.getCreatedAt() );
        packCategoryResponseDTO.description( entity.getDescription() );
        packCategoryResponseDTO.icon( entity.getIcon() );
        packCategoryResponseDTO.id( entity.getId() );
        packCategoryResponseDTO.name( entity.getName() );
        packCategoryResponseDTO.status( entity.getStatus() );
        packCategoryResponseDTO.updatedAt( entity.getUpdatedAt() );

        packCategoryResponseDTO.packCount( entity.getPacks() != null ? entity.getPacks().size() : 0 );

        return packCategoryResponseDTO.build();
    }
}
