package org.apache.qpid.proton.hawtdispatch.api;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.hawtdispatch.test.MessengerServer;
import org.apache.qpid.proton.message.Message;
import org.fusesource.hawtdispatch.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
/**
 * Hello world!
 *
 */

public class SampleTest {

	private MessengerServer server;

	@Before
	public void startServer() {
		server = new MessengerServer();
		server.start();
	}

	@After
	public void stopServer() {
		server.stop();
	}

	@Test
	public void test() {
		int expected = 10;
		final AtomicInteger countdown = new AtomicInteger(expected);
		AmqpConnectOptions options = new AmqpConnectOptions();
		final String container = UUID.randomUUID().toString();
		try {
			options.setHost(server.getHost(), server.getPort());
			options.setLocalContainerId(container);
			options.setUser("anonymous");
			options.setPassword("changeit");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		final AmqpConnection conn = AmqpConnection.connect(options );
		System.err.println("connection queue");
		conn.queue().execute(new Task() {

			@Override
			public void run() {
				System.err.println("connection running, setup callbacks");
				conn.onTransportFailure(new Callback<Throwable>() {

					@Override
					public void onSuccess(Throwable value) {
						System.err.println("transportFailure Success? " + str(value));
						conn.close();
					}

					@Override
					public void onFailure(Throwable value) {
						System.err.println("transportFailure Trouble! " + str(value));
						conn.close();
					}
				});

				conn.onConnected(new Callback<Void>() {

					@Override
					public void onSuccess(Void value) {
						System.err.println("on connect Success! in container " + container);
						final AmqpSession session = conn.createSession();
						Target rqtarget = new Target();
						rqtarget.setAddress("rq-tgt");
						final AmqpSender sender = session.createSender(rqtarget, QoS.AT_LEAST_ONCE, "request-yyy");
						Source rqsource = new Source();
						rqsource.setAddress("rs-src");
						sender.getEndpoint().setSource(rqsource);
						Source rssource = new Source();
						rssource.setAddress("rs-src");
						final AmqpReceiver receiver = session.createReceiver(rssource , QoS.AT_LEAST_ONCE, 10, "response-yyy");
						Target rstarget = new Target();
						final String address = "rs-tgt";
						rstarget.setAddress(address);
						receiver.getEndpoint().setTarget(rstarget);
						sender.onRemoteClose(new Callback<ErrorCondition>() {

							@Override
							public void onSuccess(ErrorCondition value) {
								System.err.println("sender remote close!" + str(value));
							}

							@Override
							public void onFailure(Throwable value) {
								System.err.println("sender remote close Trouble!" + str(value));
								conn.close();

							}

						});
						receiver.onRemoteClose(new Callback<ErrorCondition>() {

							@Override
							public void onSuccess(ErrorCondition value) {
								System.err.println("receiver remote close!" + str(value));
							}

							@Override
							public void onFailure(Throwable value) {
								System.err.println("receiver remote close Trouble!" + str(value));
								conn.close();

							}

						});

						final Task work = new Task() {

							private AtomicInteger count = new AtomicInteger();

							@Override
							public void run() {
								Message message = session.createTextMessage("hello world! " + String.valueOf(count.incrementAndGet()));
								message.setAddress("amqp://joze/rq-src");
								String reply_to = "amqp://" + container + "/" + address;
								message.setReplyTo(reply_to);
								message.setCorrelationId("correlator");
								final MessageDelivery md = sender.send(message);
								md.onRemoteStateChange(new Callback<DeliveryState>() {

									@Override
									public void onSuccess(DeliveryState value) {
										System.err.println("delivery remote state change! " + str(value) +
												" local: "+ str(md.getLocalState()) +
												" remote: " + str(md.getRemoteState()));
									}

									@Override
									public void onFailure(Throwable value) {
										System.err.println("remote state change Trouble!" + str(value));
										conn.close();
									}

								});
								md.onSettle(new Callback<DeliveryState>() {

									@Override
									public void onSuccess(DeliveryState value) {
										System.err.println("delivery settled! " + str(value) +
												" local: "+ str(md.getLocalState()) +
												" remote: " + str(md.getRemoteState()));
										System.err.println("sender settle mode state " +
												" local receiver " + str(sender.getEndpoint().getReceiverSettleMode()) +
												" local sender " + str(sender.getEndpoint().getSenderSettleMode()) +
												" remote receiver " + str(sender.getEndpoint().getRemoteReceiverSettleMode()) +
												" remote sender " + str(sender.getEndpoint().getRemoteSenderSettleMode()) +
												""
												);
									}

									@Override
									public void onFailure(Throwable value) {
										System.err.println("delivery sending Trouble!" + str(value));
										conn.close();
									}
								});
							}

						};
						receiver.setDeliveryListener(new AmqpDeliveryListener() {

							@Override
							public void onMessageDelivery(
									MessageDelivery delivery) {
								Message message = delivery.getMessage();
								System.err.println("incoming message delivery! " + 
										" local " + str(delivery.getLocalState()) +
										" remote " + str(delivery.getRemoteState()) +
										" message " + str(message.getBody()) +
										"");
								delivery.onSettle(new Callback<DeliveryState>() {

									@Override
									public void onSuccess(DeliveryState value) {
										System.err.println("incoming message settled! "); 
										int i = countdown.decrementAndGet();
										if ( i > 0 ) {
											System.err.println("More work " + str(i));
											work.run();
										} else {
											conn.queue().executeAfter(100, TimeUnit.MILLISECONDS, new Task() {

												@Override
												public void run() {
													System.err.println("stopping sender");
													sender.close();												
												}
											});
											conn.queue().executeAfter(200, TimeUnit.MILLISECONDS, new Task() {

												@Override
												public void run() {
													System.err.println("stopping receiver");
													receiver.close();

												}
											});
											conn.queue().executeAfter(300, TimeUnit.MILLISECONDS, new Task() {

												@Override
												public void run() {
													System.err.println("stopping session");
													session.close();

												}
											});
											conn.queue().executeAfter(400, TimeUnit.MILLISECONDS, new Task() {

												@Override
												public void run() {
													System.err.println("stopping connection");
													conn.close();

												}
											});
										}
									}

									@Override
									public void onFailure(Throwable value) {
										System.err.println("trouble settling incoming message " + str(value));
										conn.close();
									}
								});
								delivery.settle();
							}

						});

						// start the receiver
						receiver.resume();

						// send first message
						conn.queue().execute(work);
					}

					@Override
					public void onFailure(Throwable value) {
						System.err.println("on connect Failure?" + str(value));
						conn.close();
					}
				});
				System.err.println("connection setup done");


			}

		});
		try {
			System.err.println("Waiting...");
			Future<Void> disconnectedFuture = conn.getDisconnectedFuture();
			disconnectedFuture.await(300, TimeUnit.SECONDS);
			System.err.println("done");
			assertEquals(expected, server.getMessagesReceived());
		} catch (Exception e) {
			e.printStackTrace();
			fail("timeout");
		}
	}

	private String str(Object value) {
		if (value == null)
			return "null";
		return value.toString();
	}


}
