package cn.pi.bilipoints;
/** Elapsed-time clock. Never counts page loading, background time or blocked pages. */
public final class BrowseClock {
 private long remaining,last;
 public BrowseClock(int seconds,long now){remaining=Math.max(0,seconds)*1000L;last=now;}
 public void anchor(long now){last=now;}
 public long update(long now,boolean running){long delta=Math.max(0,now-last);last=now;if(running)remaining=Math.max(0,remaining-delta);return remaining;}
 public long seconds(){return (remaining+999)/1000;}
}
