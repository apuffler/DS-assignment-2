package dslab.client.commands;

import dslab.client.INBOXManager;
import dslab.client.connection.Connection;
import dslab.client.util.EncryptionManager;
import dslab.util.Keys;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.util.Base64;

public class StartSecureCommand implements Command{

    private Connection con;
    private INBOXManager manager;
    private PublicKey serverPubKey;
    private final int CHALLENGE_LEN = 32;
    private String challenge;

    @Override
    public void execute(Connection con, INBOXManager manager) {
        this.con = con;
        this.manager = manager;
        this.run();
    }

    @Override
    public void run() {
        this.con.writeCon("startsecure");
        try {
            handleComponentResponse(this.con.readCon());
            sendEncryptedChallengeResponse();
            checkChallengeResponseValidity(this.con.readCon());
            con.getEncryptionManager().setEncActive(true);
            this.con.writeCon("ok");
        } catch (Exception e) {
            this.con.stop();
        }
    }

    private void handleComponentResponse(String response) {
        String[] parts = response.split(" ");
        if(!componentHandshakeValid(parts)) { this.con.stop(); }
        String componentId = parts[1];
        findPublicKeyPerComponentId(componentId);
    }

    private boolean componentHandshakeValid(String[] responseParts) {
        if(responseParts.length != 2) { return false; }// kill connection on error
        if(!responseParts[0].equals("ok")) { return false; }
        return true;
    }

    private void findPublicKeyPerComponentId(String componentId) {
        String filepath = "keys/client/" + componentId + "_pub.der";
        try {
            this.serverPubKey = Keys.readPublicKey(new File(filepath));
        } catch (IOException e) {
            this.con.stop();
        }
    }

    private void sendEncryptedChallengeResponse() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        this.con.getEncryptionManager().clearAes();
        String response = createEncryptedChallengeResponse();
        String encryptedResponse = this.con.getEncryptionManager().encryptWithRSA(serverPubKey, response);
        this.con.writeCon(encryptedResponse);
    }

    private String createEncryptedChallengeResponse() throws NoSuchAlgorithmException {
        Base64.Encoder encoder = Base64.getEncoder();
        String challenge = encoder.encodeToString(this.con.getEncryptionManager().generateSecureRandom(CHALLENGE_LEN));
        this.challenge = challenge;
        String secretKey = encoder.encodeToString(this.con.getEncryptionManager().getAesSecretKey());
        String iv = encoder.encodeToString(this.con.getEncryptionManager().getAesInitVector());
        return new String(String.format("ok %s %s %s", challenge,secretKey,iv).getBytes());
    }

    private void checkChallengeResponseValidity(String encResponse) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String dec = con.getEncryptionManager().decryptWithAes(encResponse);
        String[] parts = dec.split(" ");
        if(parts.length != 2 || !parts[0].equals("ok")) {this.con.stop();}
        if(!parts[1].equals(challenge)) {this.con.stop();}
    }

}
