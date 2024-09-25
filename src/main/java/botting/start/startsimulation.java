package botting.start;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class startsimulation {
  public static PrintWriter out;
  public static final int pathlength=51;

  public static void main(String[]args) throws IOException {
    out=new PrintWriter(new BufferedWriter(new FileWriter("starts.out")));
    start(11,21,new ArrayList<>());
  }

  public static void start(int generaltroops,int turn,List<Integer> path) {
    int allowance=(turn%2==0&&turn!=0)?1:0;
    if(turn==51) {
      out.println(path);
      out.flush();
      return;
    }
    start(generaltroops+allowance,turn+1,new ArrayList<>(path));
    if(generaltroops>1) {
      int limit=Math.min(generaltroops-1,pathlength-turn);
      allowance=0;
      path.add(turn);
      path.add(limit);
      for(int i=turn; i<limit+turn;i++) {
        if(i%2==0&&i!=0){
          allowance++;
        }
      }
      start(generaltroops-limit+allowance,
          turn+limit, new ArrayList<>(path));
    }
  }
}
