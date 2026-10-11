package com.invoisuite.server.web;

import com.invoisuite.server.dto.integration.TimeEntryResponse;
import com.invoisuite.server.dto.integration.TimeSummaryLine;
import com.invoisuite.server.service.TimeEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/companies/{companyId}/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    @GetMapping
    public List<TimeEntryResponse> list(
            @PathVariable Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean invoiced) {
        return timeEntryService.list(companyId, from, to, invoiced);
    }

    @GetMapping("/summary")
    public List<TimeSummaryLine> summary(
            @PathVariable Long companyId,
            @RequestParam Long clientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return timeEntryService.summarize(companyId, clientId, from, to);
    }
}
