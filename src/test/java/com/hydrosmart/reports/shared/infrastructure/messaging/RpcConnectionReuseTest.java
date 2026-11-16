package com.hydrosmart.reports.shared.infrastructure.messaging;
import com.rabbitmq.client.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RpcConnectionReuseTest {
 @Test void oneConnectionUsesIsolatedChannelsAndClosesOnShutdown()throws Exception{
  var connection=mock(Connection.class);var channels=new java.util.ArrayList<Channel>();var opens=new AtomicInteger();
  when(connection.isOpen()).thenReturn(true);
  when(connection.createChannel()).thenAnswer(invocation->{var ch=mock(Channel.class);channels.add(ch);return ch;});
  var bus=new RpcBus("reports",()->{opens.incrementAndGet();return connection;});
  bus.publish("test","first",new byte[0]);bus.publish("test","second",new byte[0]);
  assertEquals(1,opens.get());assertEquals(2,channels.size());
  verify(connection,never()).close();for(var ch:channels)verify(ch).close();
  bus.close();verify(connection).close();
  assertThrows(IOException.class,()->bus.publish("test","closed",new byte[0]));
 }
 @Test void closedConnectionIsReplacedWithoutReplayingTheFailedOperation()throws Exception{
  var first=mock(Connection.class);var second=mock(Connection.class);var channel=mock(Channel.class);var opens=new AtomicInteger();
  when(first.isOpen()).thenReturn(false);when(second.isOpen()).thenReturn(true);
  when(first.createChannel()).thenThrow(new IOException("synthetic failure"));when(second.createChannel()).thenReturn(channel);
  var bus=new RpcBus("reports",()->opens.getAndIncrement()==0?first:second);
  assertThrows(IOException.class,()->bus.publish("test","failed",new byte[0]));assertEquals(1,opens.get());
  bus.publish("test","next",new byte[0]);assertEquals(2,opens.get());verify(first,times(1)).createChannel();bus.close();
 }
 @Test void concurrentPublishersShareTheConnectionWithoutSharingChannels()throws Exception{
  var connection=mock(Connection.class);var opens=new AtomicInteger();var channels=new java.util.concurrent.CopyOnWriteArrayList<Channel>();
  when(connection.isOpen()).thenReturn(true);
  when(connection.createChannel()).thenAnswer(invocation->{var channel=mock(Channel.class);channels.add(channel);return channel;});
  var bus=new RpcBus("reports",()->{opens.incrementAndGet();return connection;});
  try(var executor=Executors.newFixedThreadPool(4)){
   var tasks=new java.util.ArrayList<Callable<Void>>();
   for(int i=0;i<12;i++)tasks.add(()->{bus.publish("test","event",new byte[0]);return null;});
   for(var result:executor.invokeAll(tasks))result.get();
  }
  assertEquals(1,opens.get());assertEquals(12,channels.size());for(var channel:channels)verify(channel).close();bus.close();
 }
}
