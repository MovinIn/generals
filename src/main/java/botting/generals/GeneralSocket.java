package botting.generals;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import callback.DataListener;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

class GeneralSocket extends WebSocketClient {
  private String userid=Accounts.GAPPED.as();
  private static final int HEARTBEAT=500;
  private final Timer t;
  private final PriorityQueue<String> queue;
  private final TimerTask task;
  private static final String PUBLIC_ENDPOINT="//ws.generals.io/socket.io/?EIO=4";
  private static final String XHR_POLLING = "https:"+PUBLIC_ENDPOINT+"&transport=polling";
  private static final String CHANGE_ME="sd09fjd203i0ejwi_changeme";
  private String sid;
  private DataListener<SocketMessage> listener;
  private static final PrintWriter out;

  static{
    try {
      out = new PrintWriter(new BufferedWriter(new FileWriter("socket.out")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static GeneralSocket createGeneralSocket() {
    GeneralSocket socket;
    String data = sendXHR(XHR_POLLING+"&t=6969",false,"");
    if(data==null) {
      System.out.println("sendXHR("+XHR_POLLING+"&t=ObyKmaZ) failed.");
      return null;
    }
    JSONObject sidJSON=new JSONObject(data.substring(1));
    String sid = sidJSON.getString("sid");
    String okMsg = sendXHR(XHR_POLLING+"&t=6969&sid="+sid,true,"40");
    if(okMsg==null) {
      System.out.println("sendXHR("+XHR_POLLING+"&t=ObyKmbC&sid="+sid+") failed.");
      return null;
    }
    try {
      System.out.println(sid);
      sendXHR(XHR_POLLING+"&t=6969&sid="+sid,false,"");
      System.out.println("wss:"+PUBLIC_ENDPOINT+"&transport=websocket&sid="+sid);
      URI uri = new URI("wss:"+PUBLIC_ENDPOINT+"&transport=websocket&sid="+sid);
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null,null, null);
      SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
      socket = new GeneralSocket(uri);
      socket.setSocketFactory(sslSocketFactory);
      socket.sid=sid;
      socket.connect();
      return socket;
    } catch (URISyntaxException | KeyManagementException | NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static GeneralSocket createGeneralSocket(DataListener<SocketMessage> listener) {
    GeneralSocket socket=createGeneralSocket();
    if(socket==null) return null;
    socket.listener=listener;
    return socket;
  }

  public GeneralSocket(URI serverUri) {
    super(serverUri);
    queue=new PriorityQueue<>();
    t=new Timer();
    task=new TimerTask() {
      @Override
      public void run() {
        if(queue.isEmpty()) return;
        send(queue.remove());
      }
    };
  }

  @Override
  public void onOpen(ServerHandshake handshakedata) {
    System.out.println("Connected!");
    t.scheduleAtFixedRate(task, HEARTBEAT*2, HEARTBEAT);
    sendSlow("2probe");
    listener.call(new SocketMessage(SocketMessageType.OPEN));
  }

  @Override
  public void onMessage(String message) {
    if(message.equals("3probe")){
      send("5");
      try {Thread.sleep(1000);} catch (InterruptedException ignored) {}
      listener.call(new SocketMessage(SocketMessageType.CONNECTED));
      return;
    }
    if(message.equals("2")) {
      send("3");
      listener.call(new SocketMessage(SocketMessageType.PROBE));
      return;
    }
    out.println("received: " + message);
    out.flush();
    listener.call(new SocketMessage(SocketMessageType.MESSAGE,message));
  }

  @Override
  public void onClose(int code, String reason, boolean remote) {
    System.out.println("Disconnected!");
    listener.call(new SocketMessage(SocketMessageType.CLOSED));
  }

  @Override
  public void onError(Exception ex) {
    ex.printStackTrace();
  }

  public void sendSlow(String text) {
    queue.add(text);
  }

  public static String sendXHR(String url,boolean post,String extraPayload) {
    try {
      HttpClient httpclient = HttpClients.createDefault();
      URIBuilder builder;
      builder = new URIBuilder(url);

      HttpUriRequest postGet;
      if(post) {
        postGet = new HttpPost(builder.build());
        ((HttpPost)(postGet)).setEntity(new StringEntity(extraPayload));
      }
      else
        postGet = new HttpGet(builder.build());
      //Execute and get the response.
      HttpResponse response = httpclient.execute(postGet);
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        try (InputStream is = entity.getContent()) {
          //TODO: scanner is known to be slow
          Scanner s = new Scanner(is).useDelimiter("\\A");
          String result = s.hasNext() ? s.next() : "";
          System.out.println(result);
          return result;
        }
      }
    }
    catch(URISyntaxException | IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void setUserId(String userid) {
    this.userid=userid;
    sendSlow(XHRUtils.buildXHR("421",new JSONArray(new Object[]{"get_username",userid})));
    sendSlow(XHRUtils.buildXHR(new JSONArray(new Object[]{"stars_and_rank",userid})));
  }

  public void join1v1() {
    send(XHRUtils.buildXHR(new JSONArray(new Object[]{"join_1v1",userid,CHANGE_ME,null})));
  }

  public void joinCustom(String custom) {
    String msg=XHRUtils.buildXHR(new JSONArray(
        new Object[]{"join_private",custom,userid,CHANGE_ME,null}));
    send(msg);
  }

  public void forceStart(String custom) {
    send(XHRUtils.buildXHR(new JSONArray(new Object[]{"set_force_start",custom,true})));
  }

  public void surrender(){
    send("42[\"surrender\"]");
  }

  public void move(){
    //TODO: implement
  }
}
