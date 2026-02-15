package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.pack.CreatePackDTO;
import com.ybrainy.backend.dto.pack.PackResponseDTO;
import com.ybrainy.backend.dto.pack.UpdatePackDTO;
import com.ybrainy.backend.entity.Pack;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PackMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Pack toEntity(CreatePackDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdatePackDTO dto, @MappingTarget Pack entity);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    PackResponseDTO toResponseDTO(Pack entity);
}

