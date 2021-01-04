package dslab.protocols.dmap;

import at.ac.tuwien.dsg.orvell.StopShellException;
import dslab.basic.BasicServer;
import dslab.basic.TCPClient;
import dslab.client.util.EncryptionManager;
import dslab.protocols.Message;
import dslab.protocols.ProtocolException;
import dslab.util.Keys;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
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
import java.util.Base64;

public class DMAPClient extends TCPClient implements MessageAccessProtocol {


    private final MessageAccessProtocolServer protocol;

    private String username;
    private boolean handshakeInProgress = false;
    private int handshakeStage = 0;
    private PrivateKey privateKey;
    private String keyFileName;
    private Cipher cipher;
    private EncryptionManager encryptionManager;


    public DMAPClient(Socket socket, BasicServer server, MessageAccessProtocolServer protocol, String protocolname) throws IOException {
        super(socket, server, protocolname);
        this.protocol = protocol;
        this.username = null;
        this.keyFileName = "mailbox-" + this.server.getName().replace(".","-");
        this.encryptionManager = new EncryptionManager();
    }

    @Override
    public void processLine(String line) {
        if (encryptionManager.aesActive()) {
            try {
                line = encryptionManager.decryptWithAes(line);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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
                        println("No Number!");
                    }
                    break;
                case "login":
                    pos = args.indexOf(' ');
                    if (pos != -1) {
                        this.login(args.substring(0, pos).trim(), args.substring(pos).trim());
                    } else {
                        println("Username or Password missing!");
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
            try {
                handleHandShake(line);
            } catch (Exception e) {
                this.shutdown();
            }
        }
    }

    @Override
    public void login(String username, String password) {
        try {
            this.protocol.login(username, password);
            this.username = username;
            println("ok DMAP2.0");
        } catch (ProtocolException e) {
            println(e.getMessage());
        }
    }

    @Override
    public void list() {
        if (this.username == null) {
            println("error not logged in");
            return;
        }
        try {
            Message[] messages = this.protocol.list(this.username);
            for (Message m : messages) {
                println(String.format("%d %s %s%n", m.getID(), m.getFrom(), m.getSubject()));
            }
        } catch (ProtocolException e) {
            println(e.getMessage());
        }
    }

    @Override
    public void show(int msgid) {
        if (this.username == null) {
            println("error not logged in");
            return;
        }
        try{
            Message m = this.protocol.show(this.username,msgid);
            println(String.format("from %s%n",m.getFrom()));
            println(String.format("to %s%n",m.getTo()));
            println(String.format("subject %s%n",m.getSubject()));
            println(String.format("data %s%n",m.getData()));
            println(String.format("hash %s%n",m.getHash()));
            println("ok");
        }catch(ProtocolException e){
            println(String.format("%s%n" , e.getMessage()));
        }
    }

    @Override
    public void delete(int msgid) {
        if (this.username == null) {
            println("error not logged in");
            return;
        }
        try {
            this.protocol.delete(this.username, msgid);
        } catch (ProtocolException e) {
            println("" + e.getMessage());
        }
    }

    @Override
    public void logout() {
        if (this.username == null) {
            println("error not logged in");
            return;
        }
        this.username = null;
        printf("ok%n");
    }

    @Override
    public void quit() {
        println("ok bye");
        this.shutdown();
        throw new StopShellException();
    }

    @Override
    public void startsecure() {
        this.establishPrivateKey();
        this.establishCipher();
        println("ok " + this.keyFileName);
        this.handshakeInProgress = true;
        this.handshakeStage = 1;
    }

    private void establishPrivateKey(){
        try {
            File pk = new File(("keys/server/" + this.keyFileName +".der"));
            this.privateKey = Keys.readPrivateKey(pk);
        } catch (IOException e) {
            this.shutdown();
        }
    }

    private void establishCipher() {
        try {
            this.cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            this.cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            this.shutdown();
        }
    }

    public void handleHandShake(String line) throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        switch (handshakeStage) {
            case 1:
                handleClientChallenge(line);
                break;
            case 2:
                verifyClientHandshakeCompletion(line);
                break;
        }
    }

    private void handleClientChallenge(String line) throws IllegalBlockSizeException, InvalidKeyException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        String decrypted = encryptionManager.decryptBase64WithRSA(privateKey,line);
        Base64.Decoder decoder = Base64.getDecoder();
        String[] parts = decrypted.split(" ");
        if (parts.length != 4) {
            this.shutdown(); // kill if handshake wrong w.o. any error msg.
        }

        String msgReceivedOk = parts[0];
        String clientChallenge = parts[1];
        byte[] aesSecretKey = decoder.decode(parts[2]);
        byte[] initializationVector = decoder.decode(parts[3]);

        if (!msgReceivedOk.equals("ok")) {
            this.shutdown();
        }

        setupAes(aesSecretKey, initializationVector);
        sendAesEncChallengeResponse(clientChallenge);
        this.handshakeStage = 2;
    }

    private void sendAesEncChallengeResponse(String challenge) throws NoSuchPaddingException, InvalidKeyException,
            NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String response = "ok " + challenge;
        String encResponse = encryptionManager.encryptWithAes(response);
        println(encResponse);
    }

    private void setupAes(byte[] aesSecret, byte[] iv) {
        encryptionManager.setInitializationVector(iv);
        encryptionManager.setSecretKey(aesSecret);
    }

    private void verifyClientHandshakeCompletion(String line) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String decResp = encryptionManager.decryptWithAes(line);
        if (!decResp.equals("ok")) {this.shutdown();}
        encryptionManager.setEncActive(true);
        handshakeInProgress = false;
    }


    private void println(String line){
        if(encryptionManager.aesActive()) {
            line = encMsg(line);
        }
        this.out().println(line);
    }

    private void print(String line){
        if(encryptionManager.aesActive()) {
            line = encMsg(line);
        }
        this.out().print(line);
    }

    private void printf(String line) {
        if(encryptionManager.aesActive()) {
            line = encMsg(line);
        }
        this.out().printf(line);
    }

    private String encMsg(String line){
        try {
            return encryptionManager.encryptWithAes(line);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.shutdown();
        return null;
    }


}
