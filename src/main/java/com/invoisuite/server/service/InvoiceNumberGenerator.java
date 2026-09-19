package com.invoisuite.server.service;

import com.invoisuite.server.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/** Suggests invoice numbers in the "{sequence}/{month}/{year}" shape used in the example template. */
@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;

    public String suggestFor(Long companyId, LocalDate issueDate) {
        YearMonth ym = YearMonth.from(issueDate);
        long countThisMonth = invoiceRepository.countByCompanyIdAndIssueDateBetween(
                companyId, ym.atDay(1), ym.atEndOfMonth());
        long sequence = countThisMonth + 1;
        return sequence + "/" + issueDate.getMonthValue() + "/" + issueDate.getYear();
    }
}
