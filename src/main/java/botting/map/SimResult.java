package botting.map;

public class SimResult {
  public int[]troops;
  public int[]terrain;
  public int land;
  public SimResult(int[]troops,int[]terrain){
    this.troops=new int[troops.length];
    this.terrain=new int[terrain.length];
    for(int i=0; i<troops.length;i++){
      this.troops[i]=troops[i];
      this.terrain[i]=terrain[i];
      if(terrain[i]==0){
        land++;
      }
    }
  }
}
