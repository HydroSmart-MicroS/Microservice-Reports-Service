package com.hydrosmart.reports.domain.services;
import java.util.*;
import java.time.Instant;
/** get returns period dates as Instant; list returns their ISO text for report metadata. */
public interface ReportRepository {
 void create(UUID id,String user,UUID unit,Instant from,Instant to,String format);
 Map<String,Object> get(UUID id);
 List<Map<String,Object>> list(String user,UUID unit);
 void complete(UUID id,UUID request,byte[] content);
 void failed(UUID id,UUID request,String reason);
 boolean retry(UUID id);
 byte[] content(UUID id);
}

