import com.solacesystems.solclientj.core.SolEnum;
import com.solacesystems.solclientj.core.Solclient;
import com.solacesystems.solclientj.core.SolclientException;
import com.solacesystems.solclientj.core.event.MessageCallback;
import com.solacesystems.solclientj.core.event.SessionEventCallback;
import com.solacesystems.solclientj.core.handle.*;
import com.solacesystems.solclientj.core.resource.Topic;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class TopicSubscriber {
    public static void main(String[] args) throws SolclientException {
        // Check command line arguments
        // Check command line arguments
        if (args.length != 3) {
            System.out.println("Usage: TopicSubscriber <host:port> <client-username@message-vpn> <client-password>");
            System.out.println();
            System.exit(-1);
        }
        String[] userSplit = args[1].split("@");
        if (userSplit.length != 2) {
            System.out.println("Usage: TopicSubscriber <host:port> <client-username@message-vpn> <client-password>");
            System.out.println();
            System.exit(-1);
        }
        if (userSplit[0].isEmpty()) {
            System.out.println("No client-username entered");
            System.out.println();
            System.exit(-1);
        }
        if (userSplit[1].isEmpty()) {
            System.out.println("No message-vpn entered");
            System.out.println();
            System.exit(-1);
        }

        String host = args[0];
        String username = userSplit[0];
        String vpnName = userSplit[1];
        String password = args[2];
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
        sessionProperties.add(host);
        sessionProperties.add(SessionHandle.PROPERTIES.USERNAME);
        sessionProperties.add(username);
        sessionProperties.add(SessionHandle.PROPERTIES.PASSWORD);
        sessionProperties.add(password);
        sessionProperties.add(SessionHandle.PROPERTIES.VPN_NAME);
        sessionProperties.add(vpnName);
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