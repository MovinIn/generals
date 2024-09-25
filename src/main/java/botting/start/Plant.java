package botting.start;

import java.util.ArrayList;
import java.util.List;

//TODO: have a list of branches
public class Plant {
  private final List<Branch> branches;
  private double score;

  public Plant(double score,Branch... branches) {
    this.branches=new ArrayList<>();
    this.branches.addAll(List.of(branches));
    this.score=score;
  }
  public Plant() {
    this(0);
  }
  public double compare(Plant other) {
    return Double.compare(score,other.score);
  }

  public List<Branch> getBranches(){
    return branches;
  }

  public double getScore(){
    return score;
  }

  public String toString(){
    StringBuilder b=new StringBuilder();
    for(Branch branch:branches){
      b.append(branch.toString()).append("\n");
    }
    return b.append("score: ").append(score).toString();
  }
}
