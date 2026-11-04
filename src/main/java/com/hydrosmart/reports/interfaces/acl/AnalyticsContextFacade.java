package com.hydrosmart.reports.interfaces.acl;
import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import com.hydrosmart.reports.application.internal.outboundservices.ConsumptionDirectory;
import com.hydrosmart.reports.shared.infrastructure.messaging.RpcBus;
@Component
public class AnalyticsContextFacade implements ConsumptionDirectory {
 private final RpcBus bus;public AnalyticsContextFacade(RpcBus bus){this.bus=bus;}
 public Map<String,Object> access(UUID unit,String token){return bus.get("properties","/internal/v1/units/"+unit+"/access",RpcBus.bearer(token));}
 public Map<String,Object> report(UUID unit,String from,String to){return bus.get("analytics","/internal/v1/analytics/estimate?unitId="+unit+"&from="+URLEncoder.encode(from,StandardCharsets.UTF_8)+"&to="+URLEncoder.encode(to,StandardCharsets.UTF_8),bus.internalHeaders());}
}

