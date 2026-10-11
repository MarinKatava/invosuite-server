package com.invoisuite.server.repository;

import com.invoisuite.server.domain.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByConnectionIdAndExternalIdIn(Long connectionId, Collection<String> externalIds);

    @Query("select te from TimeEntry te where te.connection.id = :connectionId"
            + " and te.workDate between :from and :to")
    List<TimeEntry> findByConnectionAndPeriod(@Param("connectionId") Long connectionId,
                                              @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select te from TimeEntry te left join fetch te.invoiceItem ii left join fetch ii.invoice"
            + " where te.company.id = :companyId and te.workDate between :from and :to"
            + " order by te.workDate desc, te.id desc")
    List<TimeEntry> findForCompany(@Param("companyId") Long companyId,
                                   @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Unbilled entries for a client's projects in a period, ready to be turned into invoice lines. */
    @Query("select te from TimeEntry te join fetch te.project p"
            + " where te.company.id = :companyId and p.client.id = :clientId"
            + " and te.invoiceItem is null and te.workDate between :from and :to"
            + " order by p.name, te.workDate, te.id")
    List<TimeEntry> findUnbilledForClient(@Param("companyId") Long companyId, @Param("clientId") Long clientId,
                                          @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<TimeEntry> findAllByIdInAndCompanyId(Collection<Long> ids, Long companyId);

    List<TimeEntry> findByInvoiceItemIdIn(Collection<Long> invoiceItemIds);

    /** Frees every entry billed on the given invoice (before its lines are rebuilt or it is deleted). */
    @Modifying(flushAutomatically = true)
    @Query("update TimeEntry te set te.invoiceItem = null where te.invoiceItem.id in"
            + " (select i.id from InvoiceItem i where i.invoice.id = :invoiceId)")
    int releaseByInvoice(@Param("invoiceId") Long invoiceId);

    @Modifying(flushAutomatically = true)
    @Query("delete from TimeEntry te where te.connection.id = :connectionId and te.invoiceItem is null")
    int deleteUnbilledByConnection(@Param("connectionId") Long connectionId);

    /** Keeps billed entries (history) but detaches them from the connection being removed. */
    @Modifying(flushAutomatically = true)
    @Query("update TimeEntry te set te.connection = null, te.externalId = null where te.connection.id = :connectionId")
    int detachFromConnection(@Param("connectionId") Long connectionId);
}
