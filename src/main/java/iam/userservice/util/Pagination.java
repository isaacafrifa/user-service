package iam.userservice.util;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@Builder
public class Pagination {
    private int pageNo;
    private int pageSize;
    private String direction;
    private String sortBy;

    public Pageable toPageable() {
        Sort.Direction sortDirection = direction != null && direction.contains("desc") 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
        return PageRequest.of(pageNo, pageSize, Sort.by(sortDirection, sortBy));
    }
}
