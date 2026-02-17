package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.packcategory.CreatePackCategoryDTO;
import com.ybrainy.backend.dto.packcategory.PackCategoryResponseDTO;
import com.ybrainy.backend.dto.packcategory.UpdatePackCategoryDTO;
import com.ybrainy.backend.entity.PackCategory;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-17T02:07:26+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.8 (Oracle Corporation)"
)
@Component
public class PackCategoryMapperImpl implements PackCategoryMapper {

    @Override
    public PackCategory toEntity(CreatePackCategoryDTO dto) {
        if ( dto == null ) {
            return null;
        }

        PackCategory.PackCategoryBuilder packCategory = PackCategory.builder();

        packCategory.name( dto.getName() );
        packCategory.description( dto.getDescription() );
        packCategory.icon( dto.getIcon() );

        return packCategory.build();
    }

    @Override
    public void updateEntity(UpdatePackCategoryDTO dto, PackCategory entity) {
        if ( dto == null ) {
            return;
        }

        entity.setName( dto.getName() );
        entity.setDescription( dto.getDescription() );
        entity.setIcon( dto.getIcon() );
    }

    @Override
    public PackCategoryResponseDTO toResponseDTO(PackCategory entity) {
        if ( entity == null ) {
            return null;
        }

        PackCategoryResponseDTO.PackCategoryResponseDTOBuilder packCategoryResponseDTO = PackCategoryResponseDTO.builder();

        packCategoryResponseDTO.id( entity.getId() );
        packCategoryResponseDTO.name( entity.getName() );
        packCategoryResponseDTO.description( entity.getDescription() );
        packCategoryResponseDTO.icon( entity.getIcon() );
        packCategoryResponseDTO.status( entity.getStatus() );
        packCategoryResponseDTO.createdAt( entity.getCreatedAt() );
        packCategoryResponseDTO.updatedAt( entity.getUpdatedAt() );

        packCategoryResponseDTO.packCount( entity.getPacks() != null ? entity.getPacks().size() : 0 );

        return packCategoryResponseDTO.build();
    }
}
