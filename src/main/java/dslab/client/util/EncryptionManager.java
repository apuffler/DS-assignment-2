package dslab.client.util;

import dslab.util.Keys;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;

public class EncryptionManager {

    private final int IV_COUNT = 16;
    private final String ALGORITHM = "AES";
    private final String AES_CIPHER_TYPE = "AES/CTR/NoPadding";

    private KeyGenerator keyGenerator;
    private byte[] secretKey;
    private SecretKeySpec keySpec;
    private byte[] initializationVector;
    private boolean encReady;

    public EncryptionManager() {
        try {
            this.keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        this.initializationVector = new byte[0];
        this.secretKey = new byte[0];
        this.encReady = false;
    }

    public byte[] generateSecureRandom(int length) throws NoSuchAlgorithmException {
        SecureRandom randomSecureRandom = new SecureRandom();
        byte[] rand = new byte[length];
        randomSecureRandom.nextBytes(rand);
        return rand;
    }

    /** pre-cond: has to have secret key and iv set before usage. **/
    public String encryptWithAes(String data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(AES_CIPHER_TYPE);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
        byte[] encBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encBytes);
    }

    /** pre-cond: has to have secret key and iv set before usage. **/
    public String decryptWithAes(String encData) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(AES_CIPHER_TYPE);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
        byte[] encBytes = Base64.getDecoder().decode(encData);
        byte[] decBytes = cipher.doFinal(encBytes);
        return new String(decBytes);
    }

    public String decryptBase64WithRSA(Key key, String data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] encData = Base64.getDecoder().decode(data);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decData = cipher.doFinal(encData);
        return new String(decData);
    }

    public String encryptWithRSA(Key key, String data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encData = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encData);
    }

    public void clearAes() {
        if(this.initializationVector != null && this.initializationVector.length > 0) {
            this.initializationVector = new byte[0];
        }
        if(this.secretKey != null) {
            this.secretKey = null;
        }
    }

    public void setInitializationVector(byte[] initializationVector) {
        this.initializationVector = initializationVector;
    }

    public void setSecretKey(byte[] secretKey) {
        this.secretKey = secretKey;
        this.keySpec = new SecretKeySpec(secretKey, ALGORITHM);
    }

    public byte[] getAesSecretKey() {
        if(secretKey == null || secretKey.length == 0) {
            this.secretKey = keyGenerator.generateKey().getEncoded();
            this.keySpec = new SecretKeySpec(secretKey, ALGORITHM);
        }
        return secretKey;
    }

    public byte[] getAesInitVector() throws NoSuchAlgorithmException {
        if(initializationVector.length == 0) {
            this.initializationVector = generateSecureRandom(IV_COUNT);
        }

        return this.initializationVector;
    }

    public void setEncActive(boolean encActive) {this.encReady = encActive;}
    public boolean aesActive() {return encReady;}

}
