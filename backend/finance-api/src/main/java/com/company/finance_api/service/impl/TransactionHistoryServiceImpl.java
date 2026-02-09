package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Transaction;
import com.company.finance_api.dto.TransactionHistoryResponse;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private final TransactionRepository transactionRepository;
    private final CurrentUserResolver currentUserResolver;
    private final UserRepository userRepository;

    @Override
    public List<TransactionHistoryResponse> getMyHistory() {

        UUID userId = currentUserResolver.getCurrentUserId();

        var user = userRepository.findById(userId)
                .orElseThrow();

        return transactionRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(tx -> new TransactionHistoryResponse(
                        tx.getId(),
                        tx.getInstrument().getSymbol(),
                        tx.getType().name(),
                        tx.getQuantity(),
                        tx.getPrice(),
                        tx.getTotalAmount(),
                        tx.getCreatedAt()
                ))
                .toList();
    }
}