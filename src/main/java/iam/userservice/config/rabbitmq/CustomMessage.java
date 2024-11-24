package iam.userservice.config.rabbitmq;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Custom message class for RabbitMQ communication.
 * Used for asynchronous message processing between services.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the message.
     * Used for tracking and deduplication.
     */
    @NotBlank(message = "Message ID cannot be blank")
    private String messageId;

    /**
     * Content of the message to be processed.
     */
    @NotBlank(message = "Message content cannot be blank")
    private String message;

    /**
     * Timestamp when the message was created.
     * Format: yyyy-MM-dd HH:mm:ss
     */
    @NotNull(message = "Message date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime messageDate;
}

