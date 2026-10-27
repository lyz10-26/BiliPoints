import cn.pi.bilipoints.*;
public class PlaybackClockTest {
 static void ok(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args){PlaybackClock c=new PlaybackClock();c.samplePlayed(0,0,1500,"episode");c.samplePlayed(60000,60,1500,"episode");ok(c.remaining()==540,"verified media ranges count across delayed callbacks");c.samplePlayed(120000,60,1500,"episode");ok(c.remaining()==540,"background waiting without playback does not count");c.samplePlayed(121000,1060,1500,"episode");ok(c.remaining()==540,"implausible jump cannot count");PlaybackClock complete=new PlaybackClock();complete.samplePlayed(0,0,1500,"episode");complete.samplePlayed(600000,600,1500,"episode");ok(complete.remaining()==0,"actual 600 second playback finishes");PlaybackClock shortClip=new PlaybackClock();shortClip.samplePlayed(0,0,30,"preview");shortClip.samplePlayed(1000,1,30,"preview");ok(shortClip.remaining()==600,"short preview excluded");
  PlaybackClock running=new PlaybackClock();running.start(0);running.samplePlayed(0,0,1500,"episode");for(int sec=1;sec<=600;sec++){running.samplePlayed(sec*1000L,sec,1500,"episode");ok(!running.stalled(sec*1000L),"continuous playback must not time out at one minute");}ok(running.remaining()==0,"ten minute session completes");
  ok(!running.stalled(660000),"exactly 60 seconds is still within grace");ok(running.stalled(660001),"stalled playback times out after grace");
  PlaybackClock missing=new PlaybackClock();missing.start(500);ok(missing.stalled(60501),"no video also times out");
  PlaybackClock preview=new PlaybackClock();preview.start(0);preview.samplePlayed(0,0,30,"ad");preview.samplePlayed(61000,30,30,"ad");ok(preview.stalled(61000),"preview must not reset stall timeout");
  PlaybackClock seek=new PlaybackClock();seek.start(0);seek.samplePlayed(0,0,1500,"ep");seek.samplePlayed(61000,1000,1500,"ep");ok(seek.stalled(61000),"implausible seek must not reset timeout");
  System.out.println("PASS: actual playback, ten minute session, stall timeout, seek and preview exclusions");}
}
