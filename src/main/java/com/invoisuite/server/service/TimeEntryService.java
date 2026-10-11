package com.invoisuite.server.service;

import com.invoisuite.server.domain.TimeEntry;
import com.invoisuite.server.dto.integration.TimeEntryResponse;
import com.invoisuite.server.dto.integration.TimeSummaryLine;
import com.invoisuite.server.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final CompanyService companyService;
    private final ClientService clientService;

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> list(Long companyId, LocalDate from, LocalDate to, Boolean invoiced) {
        companyService.getOwned(companyId);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(90);
        return timeEntryRepository.findForCompany(companyId, start, end).stream()
                .filter(e -> invoiced == null || (e.getInvoiceItem() != null) == invoiced)
                .map(TimeEntryResponse::from)
                .toList();
    }

    /** Unbilled hours of a client's projects in the period, grouped per project as ready-made invoice lines. */
    @Transactional(readOnly = true)
    public List<TimeSummaryLine> summarize(Long companyId, Long clientId, LocalDate from, LocalDate to) {
        clientService.getOwned(companyId, clientId);

        Map<Long, List<TimeEntry>> byProject = new LinkedHashMap<>();
        for (TimeEntry e : timeEntryRepository.findUnbilledForClient(companyId, clientId, from, to)) {
            byProject.computeIfAbsent(e.getProject().getId(), id -> new ArrayList<>()).add(e);
        }

        List<TimeSummaryLine> lines = new ArrayList<>();
        for (List<TimeEntry> entries : byProject.values()) {
            TimeEntry first = entries.get(0);
            BigDecimal hours = entries.stream().map(TimeEntry::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
            lines.add(new TimeSummaryLine(
                    first.getProject().getId(),
                    first.getProject().getName(),
                    first.getProject().getDefaultUnit() != null ? first.getProject().getDefaultUnit() : "Hour",
                    hours,
                    first.getProject().getDefaultRate(),
                    entries.size(),
                    entries.stream().map(TimeEntry::getId).toList()));
        }
        return lines;
    }
}
