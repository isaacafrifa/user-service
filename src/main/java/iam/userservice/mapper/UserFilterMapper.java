package iam.userservice.mapper;

import iam.userservice.util.UserFilterCriteria;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

@Component
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserFilterMapper {

    @Mapping(source = "userIds", target = "userIds")
    @Mapping(source = "firstNames", target = "firstNames")
    @Mapping(source = "lastNames", target = "lastNames")
    @Mapping(source = "emails", target = "emails")
    @Mapping(source = "phoneNumbers", target = "phoneNumbers")
    UserFilterCriteria toCriteria(UserFilterDto filterDto, @Context CircularReferenceMappingContext context);
    /**
     * Conveniently using this default method approach so we don't manually create
     * new CircularReferenceMappingContext objects for each toCriteria call in the application code.
     * Without this default method, we would need to write calls like for e.g:
     * userFilterMapper.toCriteria(userFilter, new CircularReferenceMappingContext());
     */
    default UserFilterCriteria toCriteria(UserFilterDto filterDto) {
        return toCriteria(filterDto, new CircularReferenceMappingContext());
    }
}
