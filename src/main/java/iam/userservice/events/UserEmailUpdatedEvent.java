package iam.userservice.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEmailUpdatedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * User's unique identifier
     */
    private Long userId;

    /**
     * User's old email address
     */
    @NotBlank(message = "Old email cannot be blank")
    private String oldEmail;

    /**
     * User's new email address
     */
    @NotBlank(message = "New email cannot be blank")
    private String newEmail;

    /**
     * Timestamp when the email was updated
     */
    @NotNull(message = "Update timestamp cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
