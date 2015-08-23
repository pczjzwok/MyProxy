package com.fkgfw.proxy.Secure;


import com.fkgfw.proxy.Config.ConfigManager;
import com.fkgfw.proxy.Config.ConfigPojo;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.Arrays;


public class TransSecuritySupport {
    //KeyGenerator�ṩ�Գ���Կ�������Ĺ��ܣ�֧�ָ����㷨
    KeyGenerator keygen;
    //SecretKey���𱣴�Գ���Կ
    SecretKey deskey;
    //Cipher������ɼ��ܻ���ܹ���
    private static Cipher mEncryptCipher;
    private static Cipher mDecryptCipher;
    byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    IvParameterSpec ivspec = new IvParameterSpec(iv);

    ConfigPojo configObj;

    public TransSecuritySupport(ConfigPojo configObj) {
        Security.addProvider(new com.sun.crypto.provider.SunJCE());
        try {
            deskey = getKeyFromString(configObj.getENCRYPT_SEED());
            //����Cipher����ָ����֧��AES�㷨


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        this.configObj=configObj;


    }

    public Cipher getEncryptCipher() {

        try {
            mEncryptCipher = Cipher.getInstance("AES/CTR/NoPadding/");//It's very very important for networking.
            //if it's just "AES" ,it always block when read() invoke,because it expect more padding,but there is none

            mEncryptCipher.init(Cipher.ENCRYPT_MODE, getKeyFromString(configObj.getENCRYPT_SEED()), ivspec);


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return mEncryptCipher;

    }

    public Cipher getDecryptCipher() {
        try {
            mDecryptCipher = Cipher.getInstance("AES/CTR/NoPadding/");
            mDecryptCipher.init(Cipher.DECRYPT_MODE, getKeyFromString(configObj.getENCRYPT_SEED()), ivspec);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return mDecryptCipher;


    }

    private static SecretKeySpec getKeyFromString(String keyseed) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] key = keyseed.getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); //128bit

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        return secretKeySpec;
    }


}
