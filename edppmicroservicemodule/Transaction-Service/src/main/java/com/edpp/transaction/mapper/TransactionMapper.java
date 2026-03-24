package com.edpp.transaction.mapper;
import com.edpp.transaction.dtoresponse.CustomerLimitResponse;
import com.edpp.transaction.dtoresponse.TransactionResponse;
import com.edpp.transaction.entity.Transaction;
import com.edpp.transaction.enums.TransactionStatus;
import com.edpp.transaction.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TransactionMapper {

    /**
     * Convert Transaction entity to TransactionResponse DTO
     */
    public TransactionResponse toResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .merchantTransactionId(transaction.getMerchantTransactionId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .sourceWalletId(transaction.getSourceWalletId())
                .destinationWalletId(transaction.getDestinationWalletId())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .totalAmount(transaction.getTotalAmount())
                .currency(transaction.getCurrency())
                .paymentMethod(transaction.getPaymentMethod())
                .processorName(transaction.getProcessorName())
                .processorTransactionId(transaction.getProcessorTransactionId())
                .processorResponseCode(transaction.getProcessorResponseCode())
                .processorResponseMessage(transaction.getProcessorResponseMessage())
                .description(transaction.getDescription())
                .customerId(transaction.getCustomerId())
                .customerEmail(transaction.getCustomerEmail())
                .customerPhone(transaction.getCustomerPhone())
                .transactionDate(transaction.getTransactionDate())
                .settledAt(transaction.getSettledAt())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    /**
     * Convert Transaction entity to simplified TransactionResponse DTO
     * Used for list views and search results
     */
    public TransactionResponse toSimplifiedResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .merchantTransactionId(transaction.getMerchantTransactionId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .paymentMethod(transaction.getPaymentMethod())
                .customerEmail(transaction.getCustomerEmail())
                .transactionDate(transaction.getTransactionDate())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Convert list of Transaction entities to list of TransactionResponse DTOs
     */
    public List<TransactionResponse> toResponseList(List<Transaction> transactions) {
        if (transactions == null) {
            return List.of();
        }
        return transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert list to simplified responses
     */
    public List<TransactionResponse> toSimplifiedResponseList(List<Transaction> transactions) {
        if (transactions == null) {
            return List.of();
        }
        return transactions.stream()
                .map(this::toSimplifiedResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update Transaction entity from response DTO (for partial updates)
     */
    public void updateEntity(Transaction transaction, TransactionResponse response) {
        if (transaction == null || response == null) {
            return;
        }

        if (response.getStatus() != null) {
            transaction.setStatus(response.getStatus());
        }
        if (response.getProcessorTransactionId() != null) {
            transaction.setProcessorTransactionId(response.getProcessorTransactionId());
        }
        if (response.getProcessorResponseCode() != null) {
            transaction.setProcessorResponseCode(response.getProcessorResponseCode());
        }
        if (response.getProcessorResponseMessage() != null) {
            transaction.setProcessorResponseMessage(response.getProcessorResponseMessage());
        }
        if (response.getSettledAt() != null) {
            transaction.setSettledAt(response.getSettledAt());
        }
        if (response.getDescription() != null) {
            transaction.setDescription(response.getDescription());
        }
    }

    /**
     * Create a summary response for dashboard/list views
     */
    public TransactionResponse toSummaryResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .customerEmail(transaction.getCustomerEmail())
                .transactionDate(transaction.getTransactionDate())
                .build();
    }

    /**
     * Create a detailed response with all fields
     */
    public TransactionResponse toDetailedResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionResponse response = toResponse(transaction);
        
        // Add any additional detailed fields that might not be in base response
        if (response != null) {
            // You can add more fields here if needed
        }
        
        return response;
    }

    /**
     * Convert TransactionResponse to Transaction entity (for creation)
     */
    public Transaction toEntity(TransactionResponse response) {
        if (response == null) {
            return null;
        }

        Transaction transaction = Transaction.builder()
                .transactionReference(response.getTransactionReference())
                .merchantTransactionId(response.getMerchantTransactionId())
                .type(response.getType())
                .status(response.getStatus())
                .sourceWalletId(response.getSourceWalletId())
                .destinationWalletId(response.getDestinationWalletId())
                .amount(response.getAmount())
                .fee(response.getFee())
                .totalAmount(response.getTotalAmount())
                .currency(response.getCurrency())
                .paymentMethod(response.getPaymentMethod())
                .processorName(response.getProcessorName())
                .processorTransactionId(response.getProcessorTransactionId())
                .processorResponseCode(response.getProcessorResponseCode())
                .processorResponseMessage(response.getProcessorResponseMessage())
                .description(response.getDescription())
                .customerId(response.getCustomerId())
                .customerEmail(response.getCustomerEmail())
                .customerPhone(response.getCustomerPhone())
                .transactionDate(response.getTransactionDate())
                .settledAt(response.getSettledAt())
                .build();
        
        if (response.getId() != null) {
            transaction.setId(response.getId());
        }
        
        return transaction;
    }

    /**
     * Create an error response for failed transactions
     */
    public TransactionResponse toErrorResponse(Transaction transaction, String errorMessage) {
        TransactionResponse response = toSimplifiedResponse(transaction);
        if (response != null) {
            response.setProcessorResponseMessage(errorMessage);
            response.setStatus(TransactionStatus.FAILED);
        }
        return response;
    }

    /**
     * Create a pending response for initiated transactions
     */
    public TransactionResponse toPendingResponse(Transaction transaction) {
        TransactionResponse response = toSimplifiedResponse(transaction);
        if (response != null) {
            response.setStatus(TransactionStatus.PENDING);
        }
        return response;
    }

    /**
     * Create a completed response for successful transactions
     */
    public TransactionResponse toCompletedResponse(Transaction transaction, String processorTransactionId) {
        TransactionResponse response = toResponse(transaction);
        if (response != null) {
            response.setStatus(TransactionStatus.COMPLETED);
            response.setProcessorTransactionId(processorTransactionId);
            response.setSettledAt(java.time.LocalDateTime.now());
        }
        return response;
    }

    /**
     * Map transaction type to string
     */
    public String mapTypeToString(TransactionType type) {
        if (type == null) {
            return null;
        }
        return type.name();
    }

    /**
     * Map string to transaction type
     */
    public TransactionType mapStringToType(String type) {
        if (type == null) {
            return null;
        }
        try {
            return TransactionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Map status to string
     */
    public String mapStatusToString(TransactionStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    /**
     * Map string to transaction status
     */
    public TransactionStatus mapStringToStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return TransactionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Create a batch of responses from a list of transactions with pagination info
     */
    public BatchResponse<TransactionResponse> toBatchResponse(List<Transaction> transactions, 
                                                               long totalElements, 
                                                               int page, 
                                                               int size) {
        List<TransactionResponse> content = toResponseList(transactions);
        
        return BatchResponse.<TransactionResponse>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / size))
                .page(page)
                .size(size)
                .first(page == 0)
                .last(page == ((int) Math.ceil((double) totalElements / size) - 1))
                .build();
    }

    /**
     * Batch response wrapper class
     */
    public static class BatchResponse<T> {
        private List<T> content;
        private long totalElements;
        private int totalPages;
        private int page;
        private int size;
        private boolean first;
        private boolean last;

        public static <T> Builder<T> builder() {
            return new Builder<>();
        }

        public static class Builder<T> {
            private List<T> content;
            private long totalElements;
            private int totalPages;
            private int page;
            private int size;
            private boolean first;
            private boolean last;

            public Builder<T> content(List<T> content) {
                this.content = content;
                return this;
            }

            public Builder<T> totalElements(long totalElements) {
                this.totalElements = totalElements;
                return this;
            }

            public Builder<T> totalPages(int totalPages) {
                this.totalPages = totalPages;
                return this;
            }

            public Builder<T> page(int page) {
                this.page = page;
                return this;
            }

            public Builder<T> size(int size) {
                this.size = size;
                return this;
            }

            public Builder<T> first(boolean first) {
                this.first = first;
                return this;
            }

            public Builder<T> last(boolean last) {
                this.last = last;
                return this;
            }

            public BatchResponse<T> build() {
                BatchResponse<T> response = new BatchResponse<>();
                response.content = this.content;
                response.totalElements = this.totalElements;
                response.totalPages = this.totalPages;
                response.page = this.page;
                response.size = this.size;
                response.first = this.first;
                response.last = this.last;
                return response;
            }
        }

        public List<T> getContent() { return content; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public boolean isFirst() { return first; }
        public boolean isLast() { return last; }
    }
}