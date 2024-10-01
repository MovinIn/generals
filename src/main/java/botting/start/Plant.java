package botting.start;

import java.util.ArrayList;
import java.util.List;

public class Plant {
  private final List<Branch> branches;

  public Plant(Branch... branches) {
    this.branches=new ArrayList<>();
    this.branches.addAll(List.of(branches));
  }

  public List<Branch> getBranches(){
    return branches;
  }


  public String toString(){
    StringBuilder b=new StringBuilder();
    for(Branch branch:branches){
      b.append(branch.toString()).append("\n");
    }
    return b.toString();
  }
}
