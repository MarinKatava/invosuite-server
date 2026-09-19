package com.invoisuite.server.service.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoisuite.server.domain.Invoice;
import com.invoisuite.server.domain.InvoiceItem;
import com.invoisuite.server.service.ClientSnapshot;
import com.invoisuite.server.service.IssuerSnapshot;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Renders an {@link Invoice} to a PDF using a modern, self-designed layout
 * (colored header band, two-column party cards, a highlighted totals card) -
 * it deliberately does not mimic the look of any particular spreadsheet
 * template it might have been modelled after.
 */
@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final String ACCENT = "#4338CA";
    private static final String ACCENT_LIGHT = "#EEF2FF";
    private static final String TEXT_PRIMARY = "#111827";
    private static final String TEXT_SECONDARY = "#6B7280";
    private static final String BORDER = "#E5E7EB";
    private static final String STRIPE = "#F9FAFB";

    private static final String STYLE = """
            @page { margin: 0; }
            * { box-sizing: border-box; }
            body { font-family: Helvetica, Arial, sans-serif; font-size: 10.5px; color: %TEXT_PRIMARY%; margin: 0; }
            .topbar { height: 10px; background: %ACCENT%; width: 100%%; }
            .page { padding: 28px 40px 40px 40px; }

            table.layout { width: 100%%; border-collapse: collapse; }
            table.layout td { vertical-align: top; padding: 0; border: none; }

            .header-row td { padding-bottom: 22px; }
            .brand-name { font-size: 20px; font-weight: bold; color: %ACCENT%; }
            .brand-sub { font-size: 10px; letter-spacing: 2px; color: %TEXT_SECONDARY%; margin-top: 2px; }
            .invoice-title { font-size: 19px; font-weight: bold; text-align: right; color: %TEXT_PRIMARY%; white-space: nowrap; }
            .invoice-number { text-align: right; color: %TEXT_SECONDARY%; margin-top: 2px; font-size: 11px; }

            .card { background: %STRIPE%; border: 1px solid %BORDER%; border-radius: 6px; padding: 12px 14px; }
            .card + .card { margin-left: 14px; }
            .label { font-size: 8.5px; letter-spacing: 1.5px; color: %TEXT_SECONDARY%; font-weight: bold; margin-bottom: 6px; }
            .party-name { font-weight: bold; font-size: 12px; margin-bottom: 3px; }
            .party-line { line-height: 1.5; color: %TEXT_PRIMARY%; }
            .party-line.muted { color: %TEXT_SECONDARY%; }

            .meta-row { display: block; padding: 2px 0; }
            .meta-row .k { color: %TEXT_SECONDARY%; }
            .meta-row .v { font-weight: bold; float: right; }

            table.items { width: 100%%; border-collapse: collapse; margin-top: 26px; }
            table.items thead th {
                background: %ACCENT%; color: #FFFFFF; text-align: left;
                padding: 8px 10px; font-size: 9px; letter-spacing: 0.5px;
            }
            table.items thead th.num { text-align: right; }
            table.items tbody td { padding: 7px 10px; border-bottom: 1px solid %BORDER%; }
            table.items tbody td.num { text-align: right; }
            table.items tbody tr.odd td { background: %STRIPE%; }

            table.summary { width: 45%%; margin-left: 55%%; margin-top: 16px; border-collapse: collapse; }
            table.summary td { padding: 5px 0; }
            table.summary td.k { color: %TEXT_SECONDARY%; }
            table.summary td.v { text-align: right; }
            table.summary tr.total td { border-top: 2px solid %ACCENT%; padding-top: 8px; }
            table.summary tr.total td.k { font-weight: bold; color: %TEXT_PRIMARY%; font-size: 13px; }
            table.summary tr.total td.v { font-weight: bold; color: %ACCENT%; font-size: 15px; }

            .footer { margin-top: 34px; padding-top: 14px; border-top: 1px solid %BORDER%; }
            .footer .bank { color: %TEXT_SECONDARY%; font-size: 9.5px; line-height: 1.6; }
            .footer .exempt { color: %TEXT_SECONDARY%; font-size: 8.5px; margin-top: 10px; font-style: italic; }
            """
            .replace("%ACCENT%", ACCENT)
            .replace("%ACCENT_LIGHT%", ACCENT_LIGHT)
            .replace("%TEXT_PRIMARY%", TEXT_PRIMARY)
            .replace("%TEXT_SECONDARY%", TEXT_SECONDARY)
            .replace("%BORDER%", BORDER)
            .replace("%STRIPE%", STRIPE);

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public byte[] render(Invoice invoice) {
        String html = buildHtml(invoice);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
    }

    @SneakyThrows
    private String buildHtml(Invoice invoice) {
        IssuerSnapshot issuer = objectMapper.readValue(invoice.getIssuerSnapshot(), IssuerSnapshot.class);
        ClientSnapshot client = objectMapper.readValue(invoice.getClientSnapshot(), ClientSnapshot.class);
        String symbol = invoice.getCurrency().symbol();

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"/><style>").append(STYLE).append("</style></head><body>");
        html.append("<div class=\"topbar\"></div><div class=\"page\">");

        // Header: brand on the left, big "INVOICE" title + number on the right.
        html.append("<table class=\"layout header-row\"><tr>");
        html.append("<td style=\"width:50%\">")
                .append("<div class=\"brand-name\">").append(escape(issuer.name())).append("</div>")
                .append("<div class=\"brand-sub\">INVOICE</div>")
                .append("</td>");
        html.append("<td style=\"width:50%\">")
                .append("<div class=\"invoice-title\">").append(escape(invoice.getName())).append("</div>")
                .append("<div class=\"invoice-number\">No. ").append(escape(invoice.getInvoiceNumber())).append("</div>")
                .append("<div class=\"invoice-number\">Issued ").append(invoice.getIssueDate().format(DATE_FORMAT)).append("</div>")
                .append("</td>");
        html.append("</tr></table>");

        // Three cards: From / Bill To / Details.
        html.append("<table class=\"layout\"><tr>");

        html.append("<td style=\"width:34%\"><div class=\"card\">")
                .append("<div class=\"label\">FROM</div>")
                .append("<div class=\"party-name\">").append(escape(issuer.name())).append("</div>")
                .append("<div class=\"party-line\">").append(escape(issuer.addressLine())).append("</div>")
                .append("<div class=\"party-line\">").append(escape(issuer.postalCode())).append(" ").append(escape(issuer.city())).append("</div>")
                .append("<div class=\"party-line\">").append(escape(issuer.country())).append("</div>");
        if (notBlank(issuer.vatId())) {
            html.append("<div class=\"party-line muted\">VAT ").append(escape(issuer.vatId())).append("</div>");
        }
        html.append("</div></td>");

        html.append("<td style=\"width:2%\"></td>");

        html.append("<td style=\"width:32%\"><div class=\"card\">")
                .append("<div class=\"label\">BILL TO</div>")
                .append("<div class=\"party-name\">").append(escape(client.name())).append("</div>")
                .append("<div class=\"party-line\">").append(escape(client.addressLine())).append("</div>");
        if (notBlank(client.vatId())) {
            html.append("<div class=\"party-line muted\">VAT/UID ").append(escape(client.vatId())).append("</div>");
        }
        html.append("</div></td>");

        html.append("<td style=\"width:2%\"></td>");

        html.append("<td style=\"width:30%\"><div class=\"card\">")
                .append("<div class=\"label\">DETAILS</div>")
                .append("<div class=\"meta-row\"><span class=\"k\">Period</span><span class=\"v\">")
                .append(invoice.getPeriodStart().format(DATE_FORMAT)).append(" &#8211; ").append(invoice.getPeriodEnd().format(DATE_FORMAT))
                .append("</span></div>")
                .append("<div class=\"meta-row\"><span class=\"k\">Delivery</span><span class=\"v\">")
                .append(invoice.getDeliveryDate() != null ? invoice.getDeliveryDate().format(DATE_FORMAT) : "-")
                .append("</span></div>")
                .append("<div class=\"meta-row\"><span class=\"k\">Place</span><span class=\"v\">")
                .append(escape(nullToEmpty(invoice.getInvoicingPlace())))
                .append("</span></div>")
                .append("</div></td>");

        html.append("</tr></table>");

        // Line items.
        html.append("<table class=\"items\"><thead><tr>")
                .append("<th>Project</th><th>Unit</th><th class=\"num\">Qty</th>")
                .append("<th class=\"num\">Unit price</th><th class=\"num\">Amount (").append(symbol).append(")</th>")
                .append("</tr></thead><tbody>");

        int rowIndex = 0;
        for (InvoiceItem item : invoice.getItems()) {
            String rowClass = (rowIndex++ % 2 == 1) ? " class=\"odd\"" : "";
            html.append("<tr").append(rowClass).append(">")
                    .append("<td>").append(escape(item.getProjectName())).append("</td>")
                    .append("<td>").append(escape(item.getUnit())).append("</td>")
                    .append("<td class=\"num\">").append(format(item.getQuantity())).append("</td>")
                    .append("<td class=\"num\">").append(format(item.getUnitPrice())).append(" ").append(symbol).append("</td>")
                    .append("<td class=\"num\">").append(format(item.getLineTotal())).append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");

        // Totals card.
        html.append("<table class=\"summary\">")
                .append("<tr><td class=\"k\">Net total</td><td class=\"v\">").append(format(invoice.getNetTotal())).append(" ").append(symbol).append("</td></tr>")
                .append("<tr><td class=\"k\">VAT (").append(format(invoice.getVatPercent())).append("%)</td><td class=\"v\">")
                .append(format(invoice.getVatAmount())).append(" ").append(symbol).append("</td></tr>")
                .append("<tr class=\"total\"><td class=\"k\">Total due</td><td class=\"v\">")
                .append(format(invoice.getTotal())).append(" ").append(symbol).append("</td></tr>")
                .append("</table>");

        // Footer: bank details + optional VAT exemption note.
        html.append("<div class=\"footer\"><div class=\"bank\">");
        boolean any = false;
        if (notBlank(issuer.bankName())) {
            html.append("Bank: ").append(escape(issuer.bankName()));
            any = true;
        }
        if (notBlank(issuer.iban())) {
            html.append(any ? " &#160;|&#160; " : "").append("IBAN: ").append(escape(issuer.iban()));
            any = true;
        }
        if (notBlank(issuer.bic())) {
            html.append(any ? " &#160;|&#160; " : "").append("BIC: ").append(escape(issuer.bic()));
            any = true;
        }
        if (notBlank(issuer.registrationNumber())) {
            html.append(any ? " &#160;|&#160; " : "").append("Reg. no: ").append(escape(issuer.registrationNumber()));
        }
        html.append("</div>");
        if (notBlank(invoice.getVatExemptText())) {
            html.append("<div class=\"exempt\">").append(escape(invoice.getVatExemptText())).append("</div>");
        }
        html.append("</div>");

        html.append("</div></body></html>");
        return html.toString();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String format(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
