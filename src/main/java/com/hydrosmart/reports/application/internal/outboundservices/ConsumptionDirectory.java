package com.hydrosmart.reports.application.internal.outboundservices;
import java.util.*;
public interface ConsumptionDirectory {
 Map<String,Object> access(UUID unit,String token);
 Map<String,Object> report(UUID unit,String from,String to);
}

