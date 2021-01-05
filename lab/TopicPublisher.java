import com.solacesystems.solclientj.core.SolEnum;
import com.solacesystems.solclientj.core.Solclient;
import com.solacesystems.solclientj.core.SolclientException;
import com.solacesystems.solclientj.core.event.MessageCallback;
import com.solacesystems.solclientj.core.event.SessionEventCallback;
import com.solacesystems.solclientj.core.handle.ContextHandle;
import com.solacesystems.solclientj.core.handle.Handle;
import com.solacesystems.solclientj.core.handle.MessageHandle;
import com.solacesystems.solclientj.core.handle.SessionHandle;
import com.solacesystems.solclientj.core.resource.Topic;

import java.nio.ByteBuffer;
import java.util.ArrayList;


public class TopicPublisher {

    /**
     * @param args
     */
    public static void main(String[] args) throws SolclientException {
        // Check command line arguments
        if (args.length != 3) {
            System.out.println("Usage: TopicPublisher <host:port> <client-username@message-vpn> <client-password>");
            System.out.println();
            System.exit(-1);
        }
        String[] userSplit = args[1].split("@");
        if (userSplit.length != 2) {
            System.out.println("Usage: TopicPublisher <host:port> <client-username@message-vpn> <client-password>");
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
        System.out.println("TopicPublisher initializing...");

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

        // [Session] -> define a message callback
        MessageCallback messageCallback = new MessageCallback() {
            @Override
            public void onMessage(Handle handle) {
                // Nothing to do here for publisher.
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