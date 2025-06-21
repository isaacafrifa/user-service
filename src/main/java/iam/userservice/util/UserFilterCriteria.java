package iam.userservice.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFilterCriteria implements Serializable {

    private List<Long> userIds;
    private List<String> firstNames;
    private List<String> lastNames;
    private List<String> emails;
    private List<String> phoneNumbers;
    private boolean isExactUserIdsFlag= false;
    // New field for free-text search
    private String searchText;

}
