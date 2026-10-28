package cn.pi.bilipoints;
import android.app.*;import android.os.*;import android.view.*;import android.widget.*;import android.webkit.*;import android.net.Uri;import android.content.*;import java.util.concurrent.*;
public class WebActivity extends Activity {
 final ExecutorService loginIo=Executors.newSingleThreadExecutor();boolean loginFlow,checkingLogin,closed;String initialCookie="";
 WebView web;TextView status,timer;ProgressBar bar;Handler h=new Handler();String task,url,expectedHost,expectedPath;boolean resumed,loaded,finished,failed;int seconds;BrowseClock clock;String message="正在加载官方页面，加载成功后开始计时";
 public static boolean allowed(String u){try{Uri v=Uri.parse(u);String host=v.getHost();return "https".equals(v.getScheme())&&host!=null&&(host.equals("bilibili.com")||host.endsWith(".bilibili.com"));}catch(Exception e){return false;}}
 boolean contentPage(String u){if(!allowed(u))return false;Uri p=Uri.parse(u);String host=p.getHost(),path=p.getPath()==null?"/":p.getPath();if(host.startsWith("passport.")||path.contains("login"))return false;
  if("vipmallview".equals(task))return host.equals("show.bilibili.com")||host.equals("mall.bilibili.com");
  return expectedHost.equals(host)&&path.startsWith(expectedPath);
 }
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setFlags(8192,8192);loginFlow=getIntent().getBooleanExtra("login_flow",false);initialCookie=sessionCookie();task=getIntent().getStringExtra("task");seconds=getIntent().getIntExtra("seconds",0);clock=new BrowseClock(seconds,SystemClock.elapsedRealtime());
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xFFFFFFFF);root.setOnApplyWindowInsetsListener((v,w)->{v.setPadding(w.getSystemWindowInsetLeft(),w.getSystemWindowInsetTop(),w.getSystemWindowInsetRight(),w.getSystemWindowInsetBottom());return w;});setContentView(root);root.requestApplyInsets();
  Button back=new Button(this);back.setText("返回助手并同步");root.addView(back);back.setOnClickListener(v->{setResult(RESULT_OK);finish();});
  timer=new TextView(this);timer.setTextSize(26);timer.setTypeface(null,android.graphics.Typeface.BOLD);timer.setTextColor(0xFFEF5289);timer.setGravity(17);timer.setPadding(dp(12),dp(12),dp(12),dp(8));root.addView(timer);
  bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(Math.max(1,seconds*1000));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFEF5289));root.addView(bar,new LinearLayout.LayoutParams(-1,dp(6)));
  status=new TextView(this);status.setPadding(dp(16),dp(8),dp(16),dp(8));status.setTextSize(13);status.setTextColor(0xFF666666);root.addView(status);Button reload=new Button(this);reload.setText("重新加载页面");root.addView(reload);reload.setOnClickListener(v->{failed=false;loaded=false;message="重新加载中，计时暂停";web.loadUrl(url);});
  web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);web.getSettings().setMediaPlaybackRequiresUserGesture(true);
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){
   public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){String u=r.getUrl().toString();if(allowed(u))return false;
    if(u.startsWith("bilibili://")||u.startsWith("bilibilii://")){new AlertDialog.Builder(WebActivity.this).setMessage("此页面请求打开B站客户端。切换到客户端后，本助手的网页计时会暂停；返回后以服务端结果为准。").setPositiveButton("打开B站",(d,i)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(Exception e){message="未能打开B站，请确认已安装官方客户端";}}).setNegativeButton("继续浏览网页",null).show();}
    else message="外部跳转已拦截，请在官方客户端继续";return true;
   }
   public void onPageStarted(WebView w,String u,android.graphics.Bitmap icon){loaded=false;failed=false;clock.anchor(SystemClock.elapsedRealtime());message="正在加载官方页面，计时暂停";if(!allowed(u)){failed=true;w.stopLoading();}}
   public void onPageFinished(WebView w,String u){loaded=!failed&&(seconds==0?allowed(u):contentPage(u));message=loaded?"页面已加载，请保持前台浏览":"当前不是目标内容页，计时暂停；请先完成登录或返回目标页";CookieManager.getInstance().flush();clock.anchor(SystemClock.elapsedRealtime());}
   public void onReceivedError(WebView w,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){failed=true;loaded=false;message="页面加载失败，计时暂停；可点击重新加载";}}
   public void onReceivedHttpError(WebView w,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame()){failed=true;loaded=false;message="页面响应异常，计时暂停；请重试或在官方客户端继续";}}
  });
  url=getIntent().getStringExtra("url");if(!allowed(url)){finish();return;}Uri target=Uri.parse(url);expectedHost=target.getHost();expectedPath=target.getPath()==null?"/":target.getPath();timer.setText(loginFlow?"B站官方登录":seconds>0?"浏览倒计时 00:"+seconds:"B站官方页面");bar.setVisibility(seconds>0?View.VISIBLE:View.GONE);web.loadUrl(url);h.post(tick);if(loginFlow)h.post(loginCheck);
 }
 final Runnable tick=new Runnable(){public void run(){long ms=clock.update(SystemClock.elapsedRealtime(),resumed&&loaded&&web.hasWindowFocus());
  if(seconds>0){long s=clock.seconds();timer.setText(String.format(java.util.Locale.CHINA,"浏览倒计时 %02d:%02d",s/60,s%60));bar.setProgress(seconds*1000-(int)ms);status.setText(!resumed?"已暂停：请返回前台继续浏览":message);
   if(ms==0&&!finished){finished=true;Intent result=new Intent();result.putExtra("task",task);setResult(RESULT_OK,result);finish();return;}
  }else status.setText(loginFlow?"请在官方页面完成登录，验证成功后自动返回首页":message+" · 完成后点击顶部返回");h.postDelayed(this,200);
 }};
 String sessionCookie(){String value=CookieManager.getInstance().getCookie("https://api.bilibili.com");return value==null?"":value;}
 final Runnable loginCheck=new Runnable(){public void run(){
  if(closed||!loginFlow||isFinishing())return;
  if(resumed&&!checkingLogin){final String snapshot=sessionCookie();if(LoginSession.shouldProbe(initialCookie,snapshot,web.getUrl())){
   checkingLogin=true;loginIo.execute(()->{boolean verified=false;try{verified=LoginSession.verified(new BiliApi(snapshot).profile());}catch(Exception ignored){}final boolean success=verified;
    h.post(()->{checkingLogin=false;if(closed||isFinishing()||!resumed||!success||!new BiliApi(snapshot).sameSession(new BiliApi(sessionCookie())))return;
     CookieManager.getInstance().flush();Intent result=new Intent();result.putExtra("login_success",true);setResult(RESULT_OK,result);Toast.makeText(WebActivity.this,"登录成功，正在同步账号",Toast.LENGTH_SHORT).show();finish();
    });
   });
  }}
  h.postDelayed(this,2000);
 }};
 protected void onResume(){super.onResume();resumed=true;clock.anchor(SystemClock.elapsedRealtime());if(seconds>0)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);web.onResume();}
 protected void onPause(){resumed=false;clock.anchor(SystemClock.elapsedRealtime());web.onPause();CookieManager.getInstance().flush();super.onPause();}
 protected void onDestroy(){closed=true;h.removeCallbacksAndMessages(null);loginIo.shutdownNow();if(web!=null)web.destroy();super.onDestroy();}
 public void onBackPressed(){setResult(RESULT_OK);finish();}
 int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
}
