package com.hydrosmart.reports.infrastructure.persistence.jdbc.repositories;
import java.util.Map;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReportRecordMappingTest {
 @Test void periodDatesAreIndependentOfTheJdbcDriver(){
  var start=Instant.parse("2026-10-26T05:00:00Z");
  var mapped=PgReportRepository.toDomainRow(Map.of("period_start",java.sql.Timestamp.from(start),"period_end",java.sql.Timestamp.from(start.plusSeconds(3600)),"status","completed"));
  assertEquals(start,mapped.get("period_start"));assertEquals(start.plusSeconds(3600),mapped.get("period_end"));assertEquals("completed",mapped.get("status"));
 }
}
