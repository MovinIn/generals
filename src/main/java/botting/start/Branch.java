package botting.start;

import botting.generals.Attack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Branch {
  private List<Integer> branch;
  private int index;
  public Branch(List<Integer> branch) {
    this.branch=branch;
    index=0;
  }

  public boolean hasNext(){
    return branch.size()-1>index;
  }
  public Attack next(int turn) {
    if(!hasNext()) return null;
    return new Attack(branch.get(index),branch.get(++index),false,turn);
  }
  public List<Attack> toAttackList(int turn){
    index=0;
    Attack a;
    List<Attack> attackList=new ArrayList<>();
    while((a=next(turn++))!=null){
      attackList.add(a);
    }
    index=0;
    return attackList;
  }
  public List<Integer> getBranch(){
    return branch;
  }
  public String toString(){
    return branch.size()+" length branch: "+branch;
  }
  public int overlap(Branch other){
    if(other==null) return 0;
    int min=Math.min(branch.size(),other.getBranch().size());
    int overlap=0;
    for(int i=0; i<min; i++){
      if(Objects.equals(branch.get(i), other.getBranch().get(i))){
        overlap++;
      }
    }
    return overlap;
  }
}
