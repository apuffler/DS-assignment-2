package dslab.client;

import dslab.protocols.Message;
import dslab.util.Keys;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class MessageVerifier {

    public static MessageVerifier getInstance() throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        return new MessageVerifier("keys/hmac.key");
    }

    private Mac hMac;

    public MessageVerifier(String keyFile) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec key = Keys.readSecretKey(new File(keyFile));
        hMac = Mac.getInstance("HmacSHA256");
        hMac.init(key);
    }

    public Message generateHash(Message m){
        String msg = String.join("\n",m.getFrom(),m.getTo(),m.getSubject(),m.getData());
        m.setHash(Base64.getEncoder().encodeToString(generateHash(msg)));
        return m;
    }

    private byte[] generateHash(String msg) {
        hMac.update(msg.getBytes());
        byte[] hash = hMac.doFinal();
        return hash;
    }

    public boolean verifyMessage(Message m){
        byte[] recievedHash = Base64.getDecoder().decode(m.getHash());
        String msg = String.join("\n",m.getFrom(),m.getTo(),m.getSubject(),m.getData());
        byte[] computedHash = generateHash(msg);
        //System.out.println(new String(recievedHash));
        //System.out.println(new String(computedHash));
        return MessageDigest.isEqual(computedHash,recievedHash);
    }
}
