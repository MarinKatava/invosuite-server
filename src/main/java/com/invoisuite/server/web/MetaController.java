package com.invoisuite.server.web;

import com.invoisuite.server.domain.Currency;
import com.invoisuite.server.dto.meta.CurrencyOption;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** Static reference data the Android app uses to populate dropdowns. */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @GetMapping("/currencies")
    public List<CurrencyOption> currencies() {
        return Arrays.stream(Currency.values()).map(CurrencyOption::from).toList();
    }

    @GetMapping("/units")
    public List<String> units() {
        return List.of("Hour", "Day", "Piece", "Fixed");
    }
}
