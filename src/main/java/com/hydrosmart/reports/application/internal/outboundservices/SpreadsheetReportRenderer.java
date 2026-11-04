package com.hydrosmart.reports.application.internal.outboundservices;

import java.util.Map;

public interface SpreadsheetReportRenderer {
 byte[] render(Map<String,Object> job,Map<String,Object> data);
}
