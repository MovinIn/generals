package botting.generals;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class App
{
  private static GeneralSocket socket;
  public static void main(String[] args) throws InterruptedException {
    socket=GeneralSocket.createGeneralSocket();
    Thread.sleep(2000); //TODO: should be replaced by onOpenCallback.
    try(BufferedReader br=new BufferedReader(new FileReader("precommand.in"))) {
      String command;
      while((command=br.readLine())!=null){
        processMessage(command);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Scanner s=new Scanner(System.in);
    while(s.hasNextLine()){
      processMessage(s.nextLine());
    }
  }

  private static void processMessage(String msg){
    String[] split=msg.split(" ");
    String mode=split[0];
    System.out.println(Arrays.toString(split));
    switch(mode) {
      case "1v1":
        socket.join1v1();
        break;
      case "custom":
        socket.joinCustom(split[1]);
        break;
      case "setid":
        socket.setUserId(split[1]);
        break;
      case "defaultids":
        Arrays.stream(Accounts.values())
            .filter(acc->acc.name().equalsIgnoreCase(split[1]))
            .findFirst()
            .ifPresentOrElse(accounts-> socket.setUserId(accounts.as()),
                ()-> System.out.println("account name not found."));
        break;
      case "forcestart":
        socket.forceStart(split[1]);
    }
  }
}
