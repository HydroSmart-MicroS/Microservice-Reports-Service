package com.hydrosmart.reports.domain.services;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public interface ReportService {
 record Document(byte[] content,String format){}
 UUID create(String user,UUID unit,Instant from,Instant to,String token,String format);
 List<Map<String,Object>> list(String user,UUID unit,String token);
 Document download(String user,UUID id,String token);
 void generate(UUID id,UUID request);
 void failed(UUID id,UUID request);
 void retry(String user,UUID id,String token);
}
