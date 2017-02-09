/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.solace.samples;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import com.solacesystems.solclientj.core.SolEnum;
import com.solacesystems.solclientj.core.Solclient;
import com.solacesystems.solclientj.core.SolclientException;
import com.solacesystems.solclientj.core.event.MessageCallback;
import com.solacesystems.solclientj.core.event.SessionEventCallback;
import com.solacesystems.solclientj.core.handle.ContextHandle;
import com.solacesystems.solclientj.core.handle.Handle;
import com.solacesystems.solclientj.core.handle.MessageHandle;
import com.solacesystems.solclientj.core.handle.MessageSupport;
import com.solacesystems.solclientj.core.handle.SessionHandle;
import com.solacesystems.solclientj.core.resource.Topic;

/**
 * 
 * TopicPublisher.java
 * 
 * This sample demonstrates:
 * <ul>
 * <li>Publishing a direct message to a topic.
 * </ul>
 * 
 * <p>
 * This sample shows the basics of creating a context, creating a session,
 * connecting a session, and publishing a direct message to a topic. This is
 * meant to be a very basic example, so there are minimal session properties and
 * a message handler that simply prints any received message to the screen.
 * 
 * <p>
 * Common code to perform some of the most common actions, are explicitly
 * included in this sample to emphasize the most basic building blocks of any
 * application.
 * 
 * @author Dishant Langayan
 */
public class TopicSubscriber {
    public static void main(String[] args) throws SolclientException {
        // Check command line arguments
        if (args.length < 1) {
            System.out.println("Usage: TopicSubscriber <msg_backbone_ip:port>");
            System.exit(-1);
        }
        System.out.println("TopicSubscriber initializing...");

        final CountDownLatch latch = new CountDownLatch(1); // used for
                                                            // synchronizing b/w
                                                            // threads

        // Initialize the API first
        System.out.println(" Initializing the Java RTO Messaging API...");
        int rc = Solclient.init(new String[0]);
        assertReturnCode("Solclient.init()", rc, SolEnum.ReturnCode.OK);

        // Create the context
        System.out.println(" Creating a context ...");
        final ContextHandle contextHandle = Solclient.Allocator.newContextHandle();
        rc = Solclient.createContextForHandle(contextHandle, new String[0]);
        assertReturnCode("Solclient.createContextForHandle()", rc, SolEnum.ReturnCode.OK);

        // Create the Session
        System.out.println(" Creating a session ...");
        // [Session] -> create the session properties
        ArrayList<String> sessionProperties = new ArrayList<String>();
        sessionProperties.add(SessionHandle.PROPERTIES.HOST);
        sessionProperties.add(args[0]);
        sessionProperties.add(SessionHandle.PROPERTIES.USERNAME);
        sessionProperties.add("default");
        sessionProperties.add(SessionHandle.PROPERTIES.VPN_NAME);
        sessionProperties.add("default");
        String[] props = new String[sessionProperties.size()];

        // [Session] -> define a message callback to receive messages
        MessageCallback messageCallback = new MessageCallback() {
            @Override
            public void onMessage(Handle handle) {
                try {
                    // Get the received msg from the handle
                    MessageSupport messageSupport = (MessageSupport) handle;
                    MessageHandle rxMessage = messageSupport.getRxMessage();

                    // Get the binary attachment from the msg
                    ByteBuffer buffer = ByteBuffer.allocateDirect(rxMessage.getBinaryAttachmentSize());
                    rxMessage.getBinaryAttachment(buffer);
                    buffer.flip();
                    byte[] content = new byte[buffer.remaining()];
                    buffer.get(content);

                    System.out.println("");
                    System.out.println(" Received a message with content: " + new String(content));
                    System.out.println(" Complete message dump: ");

                    // Display the contents of a message in human-readable form
                    System.out.println(rxMessage.dump(SolEnum.MessageDumpMode.FULL));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                latch.countDown(); // unblock main thread
            }

        };

        // [Session] -> define a session event callback to events such as
        // connect/disconnect events
        SessionEventCallback sessionEventCallback = new SessionEventCallback() {

            @Override
            public void onEvent(SessionHandle sessionHandle) {
                System.out.println(" Received SessionEvent:" + sessionHandle.getSessionEvent());
            }
        };

        // [Session] -> create a session handle and the actual session
        final SessionHandle sessionHandle = Solclient.Allocator.newSessionHandle();
        rc = contextHandle.createSessionForHandle(sessionHandle, sessionProperties.toArray(props), messageCallback,
                sessionEventCallback);
        assertReturnCode("contextHandle.createSession()", rc, SolEnum.ReturnCode.OK);

        // [Session] -> finally connect the session
        System.out.println(" Connecting session ...");
        rc = sessionHandle.connect();
        assertReturnCode("sessionHandle.connect()", rc, SolEnum.ReturnCode.OK);

        // Subscribe to the destination to receive messages
        Topic topic = Solclient.Allocator.newTopic("tutorial/topic");
        System.out.println(" Subscribing to topic: " + topic.getName());
        rc = sessionHandle.subscribe(topic, SolEnum.SubscribeFlags.WAIT_FOR_CONFIRM, 0);
        assertReturnCode("sessionHandle.subscribe()", rc, SolEnum.ReturnCode.OK);

        System.out.println(" Subscribed. Awaiting message...");
        try {
            latch.await(); // block here until message received, and latch will
                           // flip
        } catch (InterruptedException e) {
            System.out.println("I was awoken while waiting");
        }

        System.out.println(" Existing.");

        // Cleanup!
        // [Cleanup] -> disconnect session
        sessionHandle.disconnect();
        sessionHandle.destroy();

        // [Cleanup] -> destroy the context
        contextHandle.destroy();
    }

    /**
     * Helper method to validate return codes.
     * 
     * @param operation
     * @param returnCode
     * @param rc
     * @throws IllegalStateException
     */
    private static void assertReturnCode(String operation, int returnCode, int... rc) throws IllegalStateException {
        boolean oneRCMatched = false;
        for (int i = 0; i < rc.length; i++) {
            if (rc[i] == returnCode) {
                oneRCMatched = true;
                break;
            }
        }
        if (!oneRCMatched) {
            throw new IllegalStateException(String.format("'%s' returned unexpected returnCode %d:%s", operation,
                    returnCode, SolEnum.ReturnCode.toString(returnCode)));
        }
    }
}
