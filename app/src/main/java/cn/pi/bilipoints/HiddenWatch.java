package cn.pi.bilipoints;
import android.app.*;
import android.os.*;
import android.webkit.*;
import android.widget.*;
import android.view.*;
import org.json.*;
/** Official-page playback; only observed media ranges count towards the local timer. */
public final class HiddenWatch {
 public interface Listener {void progress(int seconds,String state);void finished(boolean ok,String message);}
 Activity owner;FrameLayout host;WebView web;Handler h=new Handler();Listener listener;
 PlaybackClock clock=new PlaybackClock();boolean closed=false,loaded=false,clientRequested=false;long deadline;
 String diagnostic="未找到网页视频，官方页面可能要求使用客户端";
 static final String SAMPLE="(function(){\nvar candidates=[];\nfunction collect(d,depth){try{var vs=d.querySelectorAll('video');for(var i=0;i<vs.length;i++)candidates.push(vs[i]);if(depth>0){var fs=d.querySelectorAll('iframe');for(var k=0;k<fs.length;k++){try{collect(fs[k].contentDocument,depth-1);}catch(e){}}}}catch(e){}}\ncollect(document,2);\nif(!candidates.length)return JSON.stringify({found:false});\ncandidates.sort(function(a,b){return (isFinite(b.duration)?b.duration:0)-(isFinite(a.duration)?a.duration:0);});\nvar v=candidates[0];v.muted=true;v.volume=0;\nif(v.paused&&!v.ended&&(!v.__bpAttempt||Date.now()-v.__bpAttempt>5000)){\nv.__bpAttempt=Date.now();try{var p=v.play();if(p&&p.then)p.then(function(){v.__bpPlayError='';},function(e){v.__bpPlayError=e&&e.name||'PlaybackError';});}catch(e){v.__bpPlayError=e.name||'PlaybackError';}}\nvar played=0;for(var n=0;n<v.played.length;n++)played+=Math.max(0,v.played.end(n)-v.played.start(n));\nreturn JSON.stringify({found:true,played:played,duration:isFinite(v.duration)?v.duration:0,playing:!v.paused&&!v.ended&&!v.seeking&&v.readyState>=3,source:v.currentSrc||v.src||'',mediaError:v.error?v.error.code:0,playError:v.__bpPlayError||'',ended:!!v.ended});\n})()";
 public HiddenWatch(Activity a,FrameLayout f){owner=a;host=f;}
 public void start(String url,Listener l){
  listener=l;PlaybackKeepAlive.stopAction=()->cancel();
  try{owner.startForegroundService(new android.content.Intent(owner,PlaybackKeepAlive.class));}
  catch(Exception e){finish(false,"后台播放服务启动失败，请在前台启动后再切后台");return;}
  long now=SystemClock.elapsedRealtime();deadline=now+1200000;clock.start(now);
  web=new WebView(owner);host.addView(web,0,new FrameLayout.LayoutParams(-1,-1));
  web.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setMediaPlaybackRequiresUserGesture(false);web.getSettings().setAllowFileAccess(false);
  web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){
   public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){
    String u=r.getUrl().toString();if(WebActivity.allowed(u))return false;
    // A client-launch request (including an iframe) does not prove webpage playback failed.
    if(u.startsWith("bilibili://")||u.startsWith("bilibilii://")||u.startsWith("intent:"))clientRequested=true;
    return true;
   }
   public void onPageStarted(WebView w,String u,android.graphics.Bitmap b){if(closed)return;loaded=false;if(!WebActivity.allowed(u))finish(false,"目标页面不受支持");}
   public void onPageFinished(WebView w,String u){
    if(closed)return;
    if(u.contains("passport.bilibili.com")||u.contains("/login")){finish(false,"播放页需要重新登录");return;}
    loaded=true;CookieManager.getInstance().flush();
   }
   public void onReceivedError(WebView w,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())finish(false,"播放页加载失败（WebView "+e.getErrorCode()+"）");}
   public void onReceivedHttpError(WebView w,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame())finish(false,"播放页响应异常（HTTP "+e.getStatusCode()+"）");}
   public boolean onRenderProcessGone(WebView w,RenderProcessGoneDetail d){finish(false,"网页播放器进程已退出，请重试");return true;}
  });
  web.loadUrl(url);h.post(tick);
 }
 String state(JSONObject j){
  if(!j.optBoolean("found"))return clientRequested?"官方页面请求打开客户端，尚未找到可播放正片":"未找到网页视频，官方页面可能要求使用客户端";
  switch(j.optInt("mediaError")){
   case 1:return "媒体播放被中止";
   case 2:return "媒体网络加载失败";
   case 3:return "媒体解码失败";
   case 4:return "网页媒体格式或来源不受支持";
  }
  String error=j.optString("playError");
  if("NotAllowedError".equals(error))return "网页拒绝自动播放，需要手动播放";
  if("NotSupportedError".equals(error))return "网页播放器不支持此媒体来源";
  double duration=j.optDouble("duration",0);
  if(duration>0&&duration<600)return "当前视频不足10分钟，未计入正片观看时长";
  if(j.optBoolean("ended"))return "视频已结束，实际观看不足10分钟";
  return j.optBoolean("playing")?"静音播放中":"缓冲或暂停，等待实际播放";
 }
 final Runnable tick=new Runnable(){public void run(){
  if(closed)return;long now=SystemClock.elapsedRealtime();
  if(now>deadline){finish(false,"播放核验超时，未满足实际观看时长");return;}
  if(!loaded){listener.progress(clock.remaining(),"加载播放器");if(clock.stalled(now)){finish(false,"官方播放器未能加载（60秒无有效进度）");return;}h.postDelayed(this,1000);return;}
  // Bound the callback wait too: a frozen renderer must not leave the job running forever.
  h.postDelayed(sampleTimeout,65000);
  web.evaluateJavascript(SAMPLE,raw->{
   h.removeCallbacks(sampleTimeout);if(closed)return;
   try{
    Object value=new JSONTokener(raw).nextValue();JSONObject j=new JSONObject(String.valueOf(value));
    int left=clock.remaining();diagnostic=state(j);
    if(j.optBoolean("found"))left=clock.samplePlayed(SystemClock.elapsedRealtime(),j.optDouble("played",0),j.optDouble("duration",0),j.optString("source",""));
    PlaybackKeepAlive.progress(left);listener.progress(left,diagnostic);
    if(left==0){finish(true,"已观测到10分钟实际播放，等待积分核验");return;}
    if(clock.stalled(SystemClock.elapsedRealtime())){finish(false,diagnostic+"（连续60秒无有效进度）");return;}
   }catch(Exception e){if(clock.stalled(SystemClock.elapsedRealtime())){finish(false,"无法读取真实播放进度（60秒无有效进度）");return;}}
   h.postDelayed(this,1000);
  });
 }};
 final Runnable sampleTimeout=()->finish(false,"网页播放器未响应进度读取，请重试");
 public void resume(){if(web!=null)web.onResume();}
 public void cancel(){finish(false,"播放已停止");}
 void finish(boolean ok,String msg){
  if(closed)return;closed=true;h.removeCallbacksAndMessages(null);
  if(web!=null){WebView old=web;web=null;old.stopLoading();host.removeView(old);old.destroy();}
  PlaybackKeepAlive.stopAction=null;owner.stopService(new android.content.Intent(owner,PlaybackKeepAlive.class));
  if(listener!=null)listener.finished(ok,msg);
 }
}
