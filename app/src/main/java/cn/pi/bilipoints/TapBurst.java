package cn.pi.bilipoints;
/** Five taps within an inclusive two-second monotonic window. */
public final class TapBurst {
 private final long[] taps=new long[5];private int count=0;private long last=-1;
 public boolean tap(long now){
  if(last>=0&&now<last)count=0;last=now;
  if(count==5){System.arraycopy(taps,1,taps,0,4);count=4;}
  taps[count++]=now;
  if(count==5&&now-taps[0]<=2000){count=0;return true;}
  return false;
 }
}
