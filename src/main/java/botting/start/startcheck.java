package botting.start;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;

public class startcheck {
  public static void main(String[]args) throws IOException {
    BufferedReader f = new BufferedReader(new FileReader("starts.out"));
    PrintWriter out25=new PrintWriter(new BufferedWriter(new FileWriter("start25.out")));
    PrintWriter out24=new PrintWriter(new BufferedWriter(new FileWriter("start24.out")));
    PrintWriter out23=new PrintWriter(new BufferedWriter(new FileWriter("start23.out")));
    String l;
    int start;
    while((l=f.readLine())!=null){
      StringTokenizer st=new StringTokenizer(l.substring(1,l.length()-1)
          .replace(" ",""),",");
      int branches=st.countTokens()/2;
      start=1;
      int[] arr=new int[st.countTokens()];
      int index=0;
      for (Iterator<Object> it = st.asIterator(); it.hasNext(); ) {
        int i = Integer.parseInt((String)it.next());
        arr[index++]=i;
        i=Integer.parseInt((String)it.next());
        arr[index++]=i;
        start+=i;
      }
      if(start==25&&branches<=4){
        print(branches,arr,out25);
        continue;
      }
      if(branches>8){
        continue;
      }
      if(start==24){
        print(branches,arr,out24);
        continue;
      }
      if(start==23){
        print(branches,arr,out23);
      }
    }
    out25.close();
    out24.close();
    out23.close();
  }

  public static void print(int branches,int[] a,PrintWriter out){
    out.println(branches);
    if(a.length==0){
      return;
    }
    out.print(a[0]);
    for(int i=1; i<a.length;i++){
      out.print(" "+a[i]);
    }
    out.println();
  }
}
