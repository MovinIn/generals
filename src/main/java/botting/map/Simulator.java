package botting.map;

import botting.generals.Attack;
import botting.generals.Map;

import java.util.*;

public final class Simulator {
  private Simulator(){}

  public static SimResult sim(Collection<Attack> attacks, SimResult result,int turn,Map map,int end){
    Queue<Attack> attackQueue=new LinkedList<>(attacks.stream().filter(att->att.turn>map.turn)
        .sorted(Comparator.comparingInt(att->att.turn))
        .toList());
    int[] troops,terrain;
    troops=Arrays.copyOf(result.troops,result.troops.length);
    terrain=Arrays.copyOf(result.terrain,result.terrain.length);
    while(turn<end){
      if(attackQueue.peek()!=null&&attackQueue.peek().turn==turn){
        Attack a=attackQueue.remove();
//        System.out.println(troops[a.start]);
//        System.out.println(terrain[a.start]);
//        System.out.println(troops[a.end]);
//        System.out.println(terrain[a.end]);
        if(map.legalMove(a.start,a.end)&&troops[a.start]>1){
          int moved=troops[a.start]-1;
          troops[a.start]=1;
          if(terrain[a.end]==Map.FRIENDLY){
            troops[a.end]+=moved;
          }
          else{
            troops[a.end]-=moved;
            if(troops[a.end]<0){
              troops[a.end]*=-1;
              terrain[a.end]=Map.FRIENDLY;
            }
          }
        }
//        System.out.println(troops[a.start]);
//        System.out.println(terrain[a.start]);
//        System.out.println(troops[a.end]);
//        System.out.println(terrain[a.end]);
      }
      if(turn%2==0){
        troops[map.generals[map.generalme]]++;
      }
      turn++;
    }
    return new SimResult(troops,terrain);
  }

  public static SimResult sim(Collection<Attack> attacks, Map map,int end){
    return sim(attacks,new SimResult(map.troops,map.terrain),map.turn,map,end);
  }
}
