package cn.pi.bilipoints;
import android.app.*;import android.webkit.*;import android.os.*;import android.view.*;import android.widget.*;import android.net.Uri;
/** Loads the official task page behind the dashboard. Never reports or awards points itself. */
public final class SilentBrowser {
 public interface Listener {void progress(int remaining,String status);void finished(boolean ok,String reason);}
 private final Activity owner;private final FrameLayout host;private final Handler handler=new Handler();private WebView web;private Listener listener;private BrowseClock clock;private boolean active=true,loaded=false,failed=false,closed=false;private long timeout;private String kind,targetHost,targetPath;
 public SilentBrowser(Activity a,FrameLayout root){owner=a;host=root;}
 public void start(String url,String taskKind,Listener l){listener=l;kind=taskKind;clock=new BrowseClock(12,SystemClock.elapsedRealtime());timeout=SystemClock.elapsedRealtime()+45000;
  Uri u=Uri.parse(url);targetHost=u.getHost();targetPath=u.getPath()==null?"/":u.getPath();web=new WebView(owner);host.addView(web,0,new FrameLayout.LayoutParams(-1,-1));web.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);web.getSettings().setMediaPlaybackRequiresUserGesture(true);CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){
   public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){if(WebActivity.allowed(r.getUrl().toString()))return false;finish(false,"此任务需要在官方客户端继续");return true;}
   public void onPageStarted(WebView w,String url,android.graphics.Bitmap icon){loaded=false;failed=false;clock.anchor(SystemClock.elapsedRealtime());if(!WebActivity.allowed(url))finish(false,"目标页面不受支持");}
   public void onPageFinished(WebView w,String url){if(closed)return;Uri p=Uri.parse(url);String h=p.getHost(),path=p.getPath()==null?"/":p.getPath();if(h==null||h.startsWith("passport.")||path.contains("login")){finish(false,"任务页面需要重新登录");return;}
    boolean correct=WebActivity.allowed(url)&&(("vipmallview".equals(kind)&&(h.equals("show.bilibili.com")||h.equals("mall.bilibili.com")))||(targetHost.equals(h)&&path.startsWith(targetPath)));
    loaded=!failed&&correct;clock.anchor(SystemClock.elapsedRealtime());if(!correct)finish(false,"任务页跳转未匹配，请在官方客户端完成");
   }
   public void onReceivedError(WebView w,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){failed=true;finish(false,"任务页面加载失败");}}
   public void onReceivedHttpError(WebView w,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame()){failed=true;finish(false,"任务页面响应异常");}}
  });web.loadUrl(url);handler.post(tick);
 }
 private final Runnable tick=new Runnable(){public void run(){if(closed)return;if(SystemClock.elapsedRealtime()>timeout){finish(false,"任务页执行超时，请保持应用前台后重试");return;}long left=clock.update(SystemClock.elapsedRealtime(),active&&loaded&&!failed);listener.progress((int)clock.seconds(),!active?"已暂停":loaded?"处理中":"加载中");if(left==0){finish(true,"页面处理结束，等待服务端核验");return;}handler.postDelayed(this,250);}};
 public void setActive(boolean on){active=on;if(clock!=null)clock.anchor(SystemClock.elapsedRealtime());if(web!=null){if(on)web.onResume();else web.onPause();}}
 public void cancel(){finish(false,"操作已停止");}
 private void finish(boolean ok,String reason){if(closed)return;closed=true;handler.removeCallbacksAndMessages(null);if(web!=null){web.stopLoading();host.removeView(web);web.destroy();web=null;}if(listener!=null)listener.finished(ok,reason);}
}
