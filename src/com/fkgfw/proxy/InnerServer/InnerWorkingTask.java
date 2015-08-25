package com.fkgfw.proxy.InnerServer;

import com.fkgfw.proxy.*;
import com.fkgfw.proxy.Config.ConfigPojo;
import com.fkgfw.proxy.Secure.TransSecuritySupport;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static com.fkgfw.proxy.Utils.isByteArrayEqual;


public class InnerWorkingTask extends BaseServerTask {

    private ADDR_TYPE address_type;
    private Socket targetSocket;
    private TransSecuritySupport mSecurity;
    private ConfigPojo configObj;

    public InnerWorkingTask(Socket socket, ConfigPojo configObj) {
        this.targetSocket = socket;
        this.mSecurity =new TransSecuritySupport(configObj);
        this.configObj=configObj;
    }

    @Override
    public void run() {

        try {
            startImpl(targetSocket);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            System.out.println("inner close connection");
        }

    }

    private void startImpl(Socket socket) throws IOException, InterruptedException {
        final byte[] buffer = new byte[configObj.getBufferSize()];
        final InputStream broswerIn = socket.getInputStream();
        final OutputStream broswerOut = socket.getOutputStream();

        int  bufferReadCount;

        bufferReadCount = broswerIn.read(buffer);
        if (bufferReadCount <= 0) {
            return;
        }

        if (!isByteArrayEqual(SHAKE_RECEIVE, buffer, 0)) {
            return;
        }

        broswerOut.write(SHAKE_SEND);
        broswerOut.flush();
//        System.out.println("sending shake��" + SHAKE_SEND);


        Socket OuterSocket = new Socket(configObj.getOuterServerIP(), configObj.getOuterServerPort());
        System.out.println("connected to Outer server");
        final CipherOutputStream OuterOUT = new CipherOutputStream(OuterSocket.getOutputStream(), mSecurity.getEncryptCipher());
        final CipherInputStream OuterIN = new CipherInputStream(OuterSocket.getInputStream(), mSecurity.getDecryptCipher());
        //TODO
//        final OutputStream OuterOUT=OuterSocket.getOutputStream();
//        final InputStream OuterIN=OuterSocket.getInputStream();


        Thread tSendOuterServer = new Thread(new Runnable() {
            @Override
            public void run() {
                forwarding(broswerIn, OuterOUT, buffer);
            }
        });


        Thread tGetFromOuterServer = new Thread(new Runnable() {
            @Override
            public void run() {
                final byte[] bufferAnother = new byte[configObj.getBufferSize()];
                forwarding(OuterIN, broswerOut, bufferAnother);

            }
        });

        tSendOuterServer.start();
        tGetFromOuterServer.start();
//        System.out.println("inner transmission started");

        tSendOuterServer.join();
        tGetFromOuterServer.join();
        socket.close();
        OuterSocket.close();


    }


}
