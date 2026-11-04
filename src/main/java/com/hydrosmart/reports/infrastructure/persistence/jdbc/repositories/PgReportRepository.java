package com.hydrosmart.reports.infrastructure.persistence.jdbc.repositories;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import com.hydrosmart.reports.domain.services.ReportRepository;
import com.hydrosmart.reports.shared.infrastructure.messaging.RpcBus;
@Repository
public class PgReportRepository implements ReportRepository {
 static Map<String,Object> toDomainRow(Map<String,Object> row){var result=new HashMap<>(row);result.replaceAll((key,value)->value instanceof Timestamp t?t.toInstant():value instanceof java.sql.Date d?d.toLocalDate():value);return result;}
 private final JdbcTemplate jdbc;private final TransactionTemplate tx;
 public PgReportRepository(JdbcTemplate jdbc,org.springframework.transaction.PlatformTransactionManager manager){this.jdbc=jdbc;tx=new TransactionTemplate(manager);}
 private UUID event(String type,UUID report,String user,Map<String,Object> data){
  UUID id=UUID.randomUUID();var payload=Map.of("event_id",id.toString(),"event_type",type,"producer","reports","schema_version",1,"occurred_at",Instant.now().toString(),"data",data);
  jdbc.update("INSERT INTO outbox_events(event_id,event_type,aggregate_id,payload) VALUES(?,?,?,?::jsonb)",id,type,report.toString(),RpcBus.JSON.writeValueAsString(payload));
  return id;
 }
 public void create(UUID id,String user,UUID unit,Instant from,Instant to,String format){tx.executeWithoutResult(status->{
  jdbc.update("INSERT INTO report_jobs(id,requested_by_user_id,unit_id,period_start,period_end,format) VALUES(?,?,?,?,?,?)",id,user,unit,Timestamp.from(from),Timestamp.from(to),format);
  UUID request=event("reports.report.requested.v1",id,user,Map.of("report_id",id.toString(),"user_id",user));
  jdbc.update("UPDATE report_jobs SET request_event_id=? WHERE id=?",request,id);
 });}
 public Map<String,Object> get(UUID id){return jdbc.queryForList("SELECT * FROM report_jobs WHERE id=?",id).stream().findFirst().map(PgReportRepository::toDomainRow).orElse(Map.of());}
 public List<Map<String,Object>> list(String user,UUID unit){
  var rows=jdbc.queryForList("SELECT id,unit_id,status,period_start,period_end,format,requested_at,completed_at,last_error,(status='failed' OR (status='pending' AND requested_at<now()-interval '5 minutes')) AS retryable FROM report_jobs WHERE requested_by_user_id=? AND unit_id=? ORDER BY requested_at DESC LIMIT 30",user,unit);
  for(var row:rows)for(String key:List.of("period_start","period_end","requested_at","completed_at"))if(row.get(key) instanceof Timestamp stamp)row.put(key,stamp.toInstant().toString());
  return rows;
 }
 public void complete(UUID id,UUID request,byte[] content){tx.executeWithoutResult(status->{
  var row=jdbc.queryForMap("SELECT * FROM report_jobs WHERE id=? FOR UPDATE",id);
  if(!"pending".equals(row.get("status"))||!Objects.equals(request,row.get("request_event_id")))return;
  jdbc.update("INSERT INTO report_artifacts(report_id,content) VALUES(?,?) ON CONFLICT DO NOTHING",id,content);
  jdbc.update("UPDATE report_jobs SET status='completed',completed_at=now(),last_error=NULL WHERE id=?",id);
  event("reports.report.completed.v1",id,row.get("requested_by_user_id").toString(),Map.of("report_id",id.toString(),"user_id",row.get("requested_by_user_id"),"unit_id",row.get("unit_id").toString()));
 });}
 public void failed(UUID id,UUID request,String reason){jdbc.update("UPDATE report_jobs SET status='failed',last_error=? WHERE id=? AND request_event_id=? AND status='pending'",reason,id,request);}
 public boolean retry(UUID id){return Boolean.TRUE.equals(tx.execute(status->{
  var rows=jdbc.queryForList("SELECT *,requested_at<now()-interval '5 minutes' AS stalled FROM report_jobs WHERE id=? FOR UPDATE",id);
  if(rows.isEmpty())return false;
  var row=rows.getFirst();
  if(!"failed".equals(row.get("status"))&&!("pending".equals(row.get("status"))&&Boolean.TRUE.equals(row.get("stalled"))))return false;
  String user=row.get("requested_by_user_id").toString();
  UUID request=event("reports.report.requested.v1",id,user,Map.of("report_id",id.toString(),"user_id",user));
  jdbc.update("UPDATE report_jobs SET status='pending',request_event_id=?,requested_at=now(),completed_at=NULL,last_error=NULL WHERE id=?",request,id);
  return true;
 }));}
 public byte[] content(UUID id){return jdbc.queryForObject("SELECT content FROM report_artifacts WHERE report_id=?",byte[].class,id);}
}

