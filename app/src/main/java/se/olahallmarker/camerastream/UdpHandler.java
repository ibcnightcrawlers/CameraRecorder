package se.olahallmarker.camerastream;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.json.*;



import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by olahallmarker on 10/09/16.
 */
public class UdpHandler {

    private int mRequest = 0;
    private int mResponse = 0;
    private int mMoney = 0;
    private String mTitle;
    private String mBuyer;

    private Thread mThread;
    private DatagramSocket senderSocket = null;

    private InetAddress mAddress;
    private Integer mPort;
    private String mContributor;

    private ReentrantLock lock = new ReentrantLock();

    public UdpHandler(InetAddress address, Integer port, String contributor_id)
    {
        mAddress = address;
        mPort = port;
        mContributor = contributor_id;

        // Create send socket
        try
        {
            senderSocket = new DatagramSocket();
            senderSocket.setSoTimeout(500);
        }
        catch(java.net.SocketException e)
        {
            e.printStackTrace();
        }
        catch(java.io.IOException e)
        {
            e.printStackTrace();
        }

        // Start server thread
        mThread = new Thread() {
            @Override
            public void run() {
                Log.i("Cam", "UDP thread starts");

                try
                {
                    while (true) {
                        Thread.sleep(1000);


                        // Send data
                        JSONObject requestObj = new JSONObject();
                        try {
                            if (response() == 1)
                            {
                                requestObj.put("type", "topic-accept");
                                requestObj.put("topic", mTitle);
                                setResponse(0);
                            }
                            else if (response() == 2)
                            {
                                requestObj.put("type", "topic-denied");
                                setResponse(0);
                            }
                            else if (response() == 3)
                            {
                                Log.d("JSON", "sending offer accept");
                                requestObj.put("type", "offer-accept");
                                setResponse(0);
                            }
                            else if (response() == 4)
                            {
                                requestObj.put("type", "offer-denied");
                                setResponse(0);
                            }
                            else
                            {
                                requestObj.put("type", "ping");
                            }
                            requestObj.put("id", mContributor);

                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        String requestStr = requestObj.toString();
                        byte[] binOut = requestStr.getBytes();

                        //Log.e("UDP", "Send data: " + requestStr);
                        //byte[] buf = new byte[]{(byte) 0xab, (byte) 0xba};
                        DatagramPacket packet = new DatagramPacket(binOut, binOut.length, mAddress, mPort);
                        senderSocket.send(packet);

                        // Receive data
                        try
                        {
                            byte[] receivedata = new byte[1024];
                            DatagramPacket recv_packet = new DatagramPacket(receivedata, receivedata.length);
                            //Log.d("UDP", "Receiving...");
                            senderSocket.receive(recv_packet);

                            String rec_str = new String(recv_packet.getData());
                            //Log.d(" Received String ",rec_str);

                            JSONObject reader = new JSONObject(rec_str);
                            String typeStr = reader.getString("type");

                            Log.d("JSON", "type=" + typeStr);
                            if (typeStr.equals("topic"))
                            {
                                Log.d("UDP", "Got topic request");
                                String titleStr = reader.getString("title");
                                mTitle = titleStr;
                                setRequest(1);
                            }
                            else if (typeStr.equals("offer"))
                            {
                                Log.d("UDP", "Got offer request");
                                String buyerStr = reader.getString("buyer");
                                setBuyer(buyerStr);
                                setRequest(2);
                            }
                            else if (typeStr.equals("stop"))
                            {
                                Log.d("UDP", "Got stop request");
                                setRequest(3);
                            }
                            else if (typeStr.equals("money"))
                            {
                                int money = reader.getInt("money");
                                setMoney(money);
                                setRequest(4);
                            }

                        }
                        catch(java.net.SocketTimeoutException e)
                        {

                        }
                        catch(org.json.JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }

                }
                catch (java.lang.InterruptedException e) {
                    e.printStackTrace();
                }
                catch(java.io.IOException e)
                {
                    e.printStackTrace();
                }
            }
        };
        mThread.start();
    }


    public int request() {
        lock.lock();
        int request = mRequest;
        lock.unlock();
        return request;
    }

    public void setRequest(int request)
    {
        lock.lock();
        mRequest = request;
        lock.unlock();
    }

    public int response() {
        lock.lock();
        int result = mResponse;
        lock.unlock();
        return mResponse;
    }

    public void setResponse(int response)
    {
        lock.lock();
        mResponse = response;
        lock.unlock();
    }

    // Money
    public int money() {
        lock.lock();
        int lMoney = mMoney;
        lock.unlock();
        return lMoney;
    }

    public void setMoney(int aMoney)
    {
        lock.lock();
        mMoney = aMoney;
        lock.unlock();
    }

    // Buyer
    public String buyer() {
        lock.lock();
        String lBuyer = mBuyer;
        lock.unlock();
        return lBuyer;
    }

    public void setBuyer(String buyer)
    {
        lock.lock();
        mBuyer = buyer;
        lock.unlock();
    }
}
