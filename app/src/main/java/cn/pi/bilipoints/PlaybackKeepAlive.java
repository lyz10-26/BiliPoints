package cn.pi.bilipoints;
import android.app.*;import android.content.*;import android.os.*;
/** User-started, bounded media foreground service; never restarts itself. */
public class PlaybackKeepAlive extends Service {
 static PlaybackKeepAlive instance;static Runnable stopAction;PowerManager.WakeLock lock;static final String CHANNEL="playback";int last=-1;
 public IBinder onBind(Intent i){return null;}
 public void onCreate(){super.onCreate();instance=this;NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"后台播放",NotificationManager.IMPORTANCE_LOW));PowerManager pm=getSystemService(PowerManager.class);lock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"BiliPoints:Playback");lock.acquire(1200000);}
 Notification notice(String text){PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,PlaybackKeepAlive.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("大积分助手 · 后台播放").setContentText(text).setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"停止",stop).build()).build();}
 public int onStartCommand(Intent i,int f,int id){if(i!=null&&"STOP".equals(i.getAction())){if(stopAction!=null)stopAction.run();stopSelf();return START_NOT_STICKY;}startForeground(20,notice("正在尝试播放，仅按真实媒体进度计时"));return START_NOT_STICKY;}
 static void progress(int seconds){PlaybackKeepAlive s=instance;if(s!=null&&seconds/5!=s.last){s.last=seconds/5;s.getSystemService(NotificationManager.class).notify(20,s.notice(String.format(java.util.Locale.CHINA,"剩余 %02d:%02d · 实际播放核验",seconds/60,seconds%60)));}}
 public void onDestroy(){Runnable r=stopAction;stopAction=null;if(r!=null)r.run();if(lock!=null&&lock.isHeld())lock.release();stopForeground(true);instance=null;super.onDestroy();}
}
