package com.hydrosmart.reports.infrastructure.messaging.rabbitmq;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import com.hydrosmart.reports.shared.infrastructure.messaging.RpcBus;
import com.hydrosmart.reports.interfaces.messaging.ReportEventsController;
@Component
public class ReportConsumer {
 private final RpcBus bus;private final ReportEventsController controller;
 public ReportConsumer(RpcBus bus,ReportEventsController controller){this.bus=bus;this.controller=controller;}
 @EventListener(ApplicationReadyEvent.class)public void start()throws Exception{bus.consume("reports.jobs",List.of("reports.report.requested.v1"),controller::receive,controller::failed);}
}

