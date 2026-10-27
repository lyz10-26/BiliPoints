package cn.pi.bilipoints;
/** Accumulates observed played ranges; waiting, buffering and implausible jumps do not count. */
public final class PlaybackClock {
 private double earned=0,lastPlayed=-1;private long lastWall=-1,lastAdvance=-1;private String source="";
 public void start(long now){if(lastAdvance<0)lastAdvance=now;}
 public boolean stalled(long now){return lastAdvance>=0&&now-lastAdvance>60000;}
 public int samplePlayed(long now,double played,double duration,String src){start(now);if(lastWall>=0&&src.equals(source)&&duration>=600&&Double.isFinite(duration)&&Double.isFinite(played)&&played>=lastPlayed){double wall=Math.max(0,(now-lastWall)/1000.0),delta=played-lastPlayed;if(delta<=wall*2+.5){double advance=Math.min(wall,delta);earned+=advance;if(advance>0)lastAdvance=now;}}lastWall=now;lastPlayed=played;source=src;return remaining();}
 public int remaining(){return (int)Math.ceil(Math.max(0,600-earned));}
}
