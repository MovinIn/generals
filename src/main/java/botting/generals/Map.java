package botting.generals;

import org.json.JSONArray;
import org.json.JSONObject;
import patcher.MapPatcher;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Map {
  /**raw is the raw data. rest is obvious.*/
  public int[] raw,troops,terrain,generals;
  /**useful for confirmed cities/deserts*/
  public Set<Integer> cities,mountains;
  public int[] cityarr;
  public boolean initialUpdate;
  public int h,w;
  public int generalme;
  private PrintWriter out;
  public int turn;

  //Tile constants
  public static final int EMPTY = -1;
  public static final int MOUNTAIN = -2;
  public static final int FOG = -3;
  public static final int FOG_OBSTACLE = -4;
  public static final int INVALID=-5;

  public static final int ENEMY = 0;
  public static final int FRIENDLY = 1;

  public Map() {
    fillDefault();
    try {
      out=new PrintWriter(new BufferedWriter(new FileWriter("map.out")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private int[] toIntArray(JSONArray json){
    int[]arr=new int[json.length()];
    for(int i=0; i<json.length();i++) {
      arr[i]=json.getInt(i);
    }
    return arr;
  }

  public void start(JSONObject gamedata) {
    fillDefault();
    System.out.println(gamedata);
    generalme=gamedata.getInt("playerIndex");
  }

  private void fillDefault(){
    initialUpdate=true;
    //TODO: only supporting 1v1s. by convention, our general should be at index generals[generalme]
    generals=new int[2];
    raw=new int[0];
    troops=new int[0];
    terrain=new int[0];
    turn=0;
    mountains=new HashSet<>();
    cities=new HashSet<>();
    generalme=-1;
  }

  public void update(JSONObject update) {
    int[]mappatch=toIntArray(update.getJSONArray("map_diff"));
    int[]citiespatch=toIntArray(update.getJSONArray("cities_diff"));
    int[] generals=toIntArray(update.getJSONArray("generals"));
    turn=update.getInt("turn");

    raw=MapPatcher.patch(mappatch,raw);

    out.println(Arrays.toString(raw));
    out.flush();

    cityarr=MapPatcher.patch(citiespatch,cityarr);
    if(initialUpdate) {
      w = raw[0];
      h = raw[1];
      initialUpdate = false;
    }
    troops=Arrays.copyOfRange(raw,2,w*h+2);
    terrain=Arrays.copyOfRange(raw,2+w*h,raw.length);

    //we could integrate this into the patch function, but this loop will take
    // max 600 cycles at worst case; negligible.
    for(int i:terrain){
      if(i==MOUNTAIN) {
        mountains.add(i);
      }
    }
    //we want to log all the cities regardless if they disappear, so we don't remove elements
    for(int city:cityarr) {
      cities.add(city);
    }

    //we want to log all the generals regardless if they disappear...
    for(int i=0; i<generals.length;i++) {
      if(generals[i]!=-1){
        this.generals[i]=generals[i];
      }
    }
  }

  public boolean withinBounds(int position){
    return position>=0&&position<terrain.length;
  }

  public boolean permeable(int position){
    return withinBounds(position)&&(terrain[position]==EMPTY||terrain[position]==FOG)
        &&!cities.contains(position);
  }

  public boolean legalMove(int pos1,int pos2){
    if(!withinBounds(pos1)||!withinBounds(pos2)||!permeable(pos2)){
      return false;
    }
    if(Math.abs(pos1-pos2)==w){
      return true;
    }
    return Math.abs(pos1-pos2)==1&&pos1/w==pos2/w;
  }

  public int terrain(int position) {
    if(!withinBounds(position)) return -5;
    return terrain[position];
  }

  public int height() {
    return h;
  }

  public int[] dxs(){
    return new int[]{1,-1,w,-w};
  }

  public int[] pt(int pos){
    return new int[]{pos%w,pos/w};
  }

  public double dist(int pos,int pos2) {
    int[] pt1=pt(pos);
    int[] pt2=pt(pos2);
    return Math.sqrt(Math.pow(pt1[0]-pt2[0],2)+Math.pow(pt1[1]-pt2[1],2));
  }
}
