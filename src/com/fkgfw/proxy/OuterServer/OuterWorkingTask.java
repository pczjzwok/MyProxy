package com.fkgfw.proxy.OuterServer;

import com.fkgfw.proxy.*;
import com.fkgfw.proxy.Secure.TransSecuritySupport;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.*;
import java.net.Socket;

import static com.fkgfw.proxy.Utils.*;
import static com.fkgfw.proxy.Utils.hexStringToByteArray;


public class OuterWorkingTask extends BaseServerTask {

    Socket targetSocket;
    TransSecuritySupport mSecurity;

    private ADDR_TYPE address_type;

    public OuterWorkingTask(Socket socket) {
        this.targetSocket = socket;
        this.mSecurity = new TransSecuritySupport();
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
    }

    private void startImpl(Socket socket) throws IOException, InterruptedException {

//        Socket localSender = new Socket(Config.ip, Config.ServerPort);
        final byte[] buffer = new byte[Config.BufferSize];


        int readCount = 0;


       final CipherInputStream InnerIN = new CipherInputStream(socket.getInputStream(),mSecurity.getDecryptCipher());
        final CipherOutputStream InnerOut = new CipherOutputStream(socket.getOutputStream(),mSecurity.getEncryptCipher());
 //       final InputStream InnerIN =socket.getInputStream();
 //       final OutputStream InnerOut =socket.getOutputStream();
//todo
        readCount = InnerIN.read(buffer);
        if (readCount <= 0) {
            System.out.println("connection stop ...");
            return;
        }
        String msgShake = byteArray2HexString(buffer,readCount);

        if (!msgShake.equals(SHAKE_RECEIVE)) {
            return;
        }
        System.out.println(msgShake);


        InnerOut.write(hexStringToByteArray(SHAKE_SEND));
        InnerOut.flush();
        System.out.println("sending shake��" + SHAKE_SEND);


        readCount = InnerIN.read(buffer);
        if (readCount <= 0) {
            System.out.println("connection stop ...");
            return;
        }
        String request = byteArray2HexString(buffer,readCount);

        System.out.println(request);
        if (!request.startsWith(REQUEST_HEADER)) {
            System.out.println("not a proper sock5 request");
            return;
        }





        String request_addr = null;
        int request_port = -1;


        //GET ADDR By diffrent type ,ipv4 or hostname ..ignore ipv6
        if (request.substring(6, 8).equals(ADDR_TYPE_HOSTNAME)) {
            //hostname typee
//            System.out.println("it's an url request");
            int addrLength = Integer.parseInt(request.substring(8, 10), 16);//hex STRING to int
            //substring ���ұ�
            request_addr = byteArray2DecimalStr(buffer, 10 / 2, addrLength);
            request_port = getPortFromByteArrayAtIPtype(buffer, REQUEST_HEADER_BYTE_OFFSET + addrLength);
            address_type = ADDR_TYPE.HOSTNAME;

        } else if (request.substring(6, 8).equals(ADDR_TYPE_IPV4)) {
//            System.out.println("it's an ip request");
            request_addr = getIpAddrFromByteArray(buffer, 8);
            request_port = getPortFromByteArrayAtIPtype(buffer, 8 + 4);
            address_type = ADDR_TYPE.IPV4;
        }

        System.out.println("address:" + request_addr + " port:" + request_port);





        //write back sccuss replya
        //todo check whether the proxy you can proxy
        switch (address_type) {
            case HOSTNAME:
                InnerOut.write(hexStringToByteArray(RESPONSE_SUCCESS_HEADER
                                + request.substring(6)
                ));//ͷ��+����ԭ�����ء�
                break;
            case IPV4:
                InnerOut.write(hexStringToByteArray(RESPONSE_SUCCESS_HEADER
                        + request.substring(6)));
                break;
            case IPV6:
                //todo ipv6
                break;
        }

        InnerOut.flush();



        final Socket websiteSocket = new Socket(request_addr, request_port);
        final BufferedOutputStream websiteOut = new BufferedOutputStream(websiteSocket.getOutputStream());
        final BufferedInputStream websiteIn = new BufferedInputStream(websiteSocket.getInputStream());


        Thread threadWriteOut = new Thread(new Runnable() {
            @Override
            public void run() {
                forwarding(InnerIN,websiteOut,buffer);
            }
        });


        Thread threadWriteBack = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] bufferAno = new byte[Config.BufferSize];
                forwarding(websiteIn,InnerOut,bufferAno);
            }
        });

        threadWriteOut.start();
        threadWriteBack.start();

        threadWriteOut.join();
        threadWriteBack.join();
        System.out.println("Outer connection exit");


    }


}
