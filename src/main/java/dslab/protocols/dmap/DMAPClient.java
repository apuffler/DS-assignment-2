package dslab.protocols.dmap;

import at.ac.tuwien.dsg.orvell.StopShellException;
import dslab.basic.BasicServer;
import dslab.basic.TCPClient;
import dslab.protocols.Message;
import dslab.protocols.ProtocolException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class DMAPClient extends TCPClient implements MessageAccessProtocol {


    private final MessageAccessProtocolServer protocol;

    private String username;
    private boolean handshakeInProgress = false;
    private int handshakeStage = 0;
    private PrivateKey privateKey;
    private Cipher cipher;


    public DMAPClient(Socket socket, BasicServer server, MessageAccessProtocolServer protocol, String protocolname) throws IOException {
        super(socket, server, protocolname);
        this.protocol = protocol;
        this.username = null;
    }

    @Override
    public void processLine(String line) {

        if (!handshakeInProgress) { // normal DMAP2.0 commands when not in handshake

            int pos = line.indexOf(' ');
            String cmd = line.trim();
            String args = "";
            if (pos != -1) {
                cmd = line.substring(0, pos).trim();
                args = line.substring(pos).trim();
            }

            switch (cmd) {
                case "list":
                    list();
                    break;
                case "show":
                case "delete":
                    int id = -1;
                    try {
                        id = Integer.parseInt(args);
                        if ("show".equals(cmd)) {
                            show(id);
                        } else {
                            delete(id);
                        }
                    } catch (NumberFormatException e) {
                        this.out().println("S> No Number!");
                    }
                    break;
                case "login":
                    pos = args.indexOf(' ');
                    if (pos != -1) {
                        this.login(args.substring(0, pos).trim(), args.substring(pos).trim());
                    } else {
                        this.out().println("S> Username or Password missing!");
                    }
                    break;
                case "logout":
                    this.logout();
                    break;
                case "startsecure":
                    this.startsecure();
                    break;
                default:
                    super.processLine(line);

            }
        } else { // finish handshake or die
            handleHandShake(line);
        }
        this.out().print("C> ");

    }

    @Override
    public void login(String username, String password) {
        try {
            this.protocol.login(username, password);
            this.username = username;
            this.out().println("S> ok DMAP2.0");
        } catch (ProtocolException e) {
            this.out().println("S> " + e.getMessage());
        }
    }

    @Override
    public void list() {
        if (this.username == null) {
            this.out().println("S> error not logged in");
            return;
        }
        try {
            Message[] messages = this.protocol.list(this.username);
            for (Message m : messages) {
                this.out().printf("S> %d %s %s%n", m.getID(), m.getFrom(), m.getSubject());
            }
        } catch (ProtocolException e) {
            this.out().println("S> " + e.getMessage());
        }
    }

    @Override
    public void show(int msgid) {
        if (this.username == null) {
            this.out().println("S> error not logged in");
            return;
        }
        try {
            Message m = this.protocol.show(this.username, msgid);
            this.out().printf("S> from %s%n", m.getFrom());
            this.out().printf("S> to %s%n", m.getTo());
            this.out().printf("S> subject %s%n", m.getSubject());
            this.out().printf("S> data %s%n", m.getData());
        } catch (ProtocolException e) {
            this.out().printf("S> %s%n", e.getMessage());
        }
    }

    @Override
    public void delete(int msgid) {
        if (this.username == null) {
            this.out().println("S> error not logged in");
            return;
        }
        try {
            this.protocol.delete(this.username, msgid);
        } catch (ProtocolException e) {
            this.out().println("S> " + e.getMessage());
        }
    }

    @Override
    public void logout() {
        if (this.username == null) {
            this.out().println("S> error not logged in");
            return;
        }
        this.username = null;
        this.out().printf("S> ok%n");
    }

    @Override
    public void quit() {
        this.out().println("S> ok bye");
        this.shutdown();
        throw new StopShellException();
    }

    @Override
    public void startsecure() {
        String componentID = this.server.getName();
        this.establishPrivateKey();
        this.establishCipher();
        this.out().println("S> ok " + componentID);
        this.handshakeInProgress = true;
        this.handshakeStage = 1;
    }

    private void establishPrivateKey(){
        try {
            byte[] pk = this.getPrivateKey();
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pk);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA/ECB/PKCS1Padding");
            this.privateKey = keyFactory.generatePrivate(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            this.shutdown();
        }
    }

    private byte[] getPrivateKey() throws IOException {
        String name = this.server.getName();
        return Files.readAllBytes(Paths.get(("keys/server/" + name + ".der"));
    }

    private void establishCipher() {
        try {
            this.cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            this.cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            this.shutdown();
        }
    }

    public void handleHandShake(String line) {
        switch (handshakeStage) {
            case 1:
                handleClientChallenge(line);
                break;
            case 2:
                verifyClientHandshakeCompletion(line);
                break;
        }
    }

    private void handleClientChallenge(String line) {
        String[] parts = line.split(" ");
        if (parts.length != 4) {
            this.shutdown(); // kill if handshake wrong w.o. any error msg.
        }

        String msgReceivedOk = parts[0];
        String clientChallenge = parts[1];
        String aesSecretKey = parts[2];
        String initializationVector = parts[3];

        if (!msgReceivedOk.equals("ok")) {
            this.shutdown();
        }


        this.handshakeStage = 2;
    }

    private void verifyClientHandshakeCompletion(String line) {

    }


}
