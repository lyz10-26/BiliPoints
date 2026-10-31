package cn.pi.bilipoints;
import android.app.*;import android.os.*;import android.view.*;import android.widget.*;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.content.*;import android.webkit.CookieManager;import org.json.*;import java.util.*;import java.util.concurrent.*;import java.text.SimpleDateFormat;

public class MainActivity extends Activity {
 final int PINK=0xFFFB7299,INK=0xFF18191C,MUTED=0xFF858A92,GREEN=0xFF16886A;
 Handler h=new Handler();ExecutorService io=Executors.newSingleThreadExecutor();FrameLayout root;LinearLayout content,tasks;TextView total,month,deadline,account,notice,sync,logView,vipBadge,vipExpiry,summary,levelLabel,experienceLabel,uidLabel;LinearLayout exchangeGrid;TextView exchangeBalance,exchangeStatus,exchangeResult;String exchangeResultText="";boolean checkingUpdate=false;Button exchangeRefresh;ArrayList<ExchangeSku> exchangeItems=new ArrayList<>();String exchangeMessage="登录后同步官方兑换商品";TextView walletLabel,executionLabel;ProgressBar executionBar;int executionPercent=0;TaskProgress taskProgress=new TaskProgress();Runnable pendingAccountAction;TapBurst updateBurst=new TapBurst();boolean updateScheduled=false;long updateRun=0;ImageView avatar,vipIcon;ProfileInfo profile=new ProfileInfo();ProfileImages images=new ProfileImages();String imageOwner="",badgeAsset="";ProgressBar progress;Button run,claim,refresh,cancel;final HashMap<String,SilentBrowser> browsers=new HashMap<>();final Object mutations=new Object();
 volatile boolean cancelled=false,destroyed=false;boolean busy=false,active=false,verified=false,showCompleted=false;volatile long generation=0;long lastSyncMs=0;String user="未登录",mid="",lastSync="尚未同步";VipInfo vip=new VipInfo();int points=-1,monthly=-1,limit=-1;
 ConcurrentHashMap<String,JSONArray> evidence=new ConcurrentHashMap<>();ArrayList<TaskItem> items=new ArrayList<>();HashMap<String,String> outcomes=new HashMap<>();HashMap<String,String> details=new HashMap<>();StringBuilder log=new StringBuilder();
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xFFF5F6F7);getWindow().setNavigationBarColor(0xFFF5F6F7);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);build();h.post(clock);h.postDelayed(()->{if(api().authenticated())refresh();},350);}
 int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}LinearLayout col(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);return c;}
 TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(android.text.Html.fromHtml(s.replace("\n","<br>"),android.text.Html.FROM_HTML_MODE_COMPACT));v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));v.setIncludeFontPadding(false);v.setLineSpacing(dp(2),1);v.setPadding(0,dp(2),0,dp(2));return v;}
 GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 Button button(String title,boolean solid){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(solid?0xFFFFFFFF:INK);b.setBackground(bg(solid?PINK:0xFFF5F6F7,8));b.setMinHeight(dp(44));b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(12),dp(4),dp(12),dp(4));b.setElevation(0);b.setStateListAnimator(null);return b;}
 LinearLayout card(){LinearLayout c=col();c.setPadding(dp(16),dp(12),dp(16),dp(12));c.setBackground(bg(0xFFFFFFFF,12));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(12),0,0);content.addView(c,p);return c;}
 void pair(LinearLayout row,Button b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(dp(4),dp(10),dp(4),0);row.addView(b,p);}
 void line(LinearLayout parent){View v=new View(this);v.setBackgroundColor(0xFFF1F2F3);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1));p.setMargins(0,dp(12),0,dp(12));parent.addView(v,p);}
 void build(){
  root=new FrameLayout(this);root.setBackgroundColor(0xFFF5F6F7);setContentView(root);root.setOnApplyWindowInsetsListener((v,w)->{v.setPadding(w.getSystemWindowInsetLeft(),w.getSystemWindowInsetTop(),w.getSystemWindowInsetRight(),w.getSystemWindowInsetBottom());return w;});root.requestApplyInsets();
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);content=col();content.setPadding(dp(16),dp(4),dp(16),dp(20));scroll.addView(content);root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
  FrameLayout top=new FrameLayout(this);TextView title=text("大积分助手",18,INK);title.setTypeface(null,Typeface.BOLD);title.setGravity(Gravity.CENTER);top.addView(title,new FrameLayout.LayoutParams(-1,dp(52)));Button settings=button("账号",false);settings.setBackgroundColor(android.graphics.Color.TRANSPARENT);FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(dp(56),dp(44),Gravity.END|Gravity.CENTER_VERTICAL);top.addView(settings,sp);settings.setOnClickListener(v->accountMenu());content.addView(top,new LinearLayout.LayoutParams(-1,dp(52)));
  LinearLayout member=card();LinearLayout identity=new LinearLayout(this);identity.setGravity(Gravity.CENTER_VERTICAL);
  avatar=new ImageView(this);avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);avatar.setBackground(bg(0xFFFDEAF0,28));avatar.setClipToOutline(true);avatar.setImageResource(android.R.drawable.ic_menu_myplaces);avatar.setContentDescription("当前账号头像");identity.addView(avatar,new LinearLayout.LayoutParams(dp(48),dp(48)));avatar.setOnClickListener(v->accountMenu());
  LinearLayout names=col();LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);np.setMargins(dp(12),0,0,0);identity.addView(names,np);
  LinearLayout nameRow=new LinearLayout(this);nameRow.setGravity(Gravity.CENTER_VERTICAL);account=text("未登录",15,PINK);account.setTypeface(null,Typeface.BOLD);account.setSingleLine(true);account.setEllipsize(android.text.TextUtils.TruncateAt.END);nameRow.addView(account,new LinearLayout.LayoutParams(0,-2,1));vipBadge=text("待同步",10,PINK);nameRow.addView(vipBadge);vipIcon=new ImageView(this);vipIcon.setContentDescription("B站官方会员徽标");vipIcon.setScaleType(ImageView.ScaleType.FIT_END);vipIcon.setVisibility(View.GONE);nameRow.addView(vipIcon,new LinearLayout.LayoutParams(dp(86),dp(18)));names.addView(nameRow);
  LinearLayout idRow=new LinearLayout(this);idRow.setGravity(Gravity.CENTER_VERTICAL);levelLabel=text("LV —",11,PINK);idRow.addView(levelLabel);uidLabel=text("登录后同步账号",11,MUTED);uidLabel.setPadding(dp(9),dp(4),0,0);idRow.addView(uidLabel,new LinearLayout.LayoutParams(0,-2,1));names.addView(idRow);
  walletLabel=text("B币 —   硬币 —",11,MUTED);names.addView(walletLabel);experienceLabel=text("",11,MUTED);experienceLabel.setVisibility(View.GONE);names.addView(experienceLabel);vipExpiry=text("",11,MUTED);vipExpiry.setVisibility(View.GONE);names.addView(vipExpiry);member.addView(identity);line(member);
  LinearLayout balances=new LinearLayout(this);balances.setGravity(Gravity.BOTTOM);LinearLayout available=col();available.addView(text("可用大积分",12,MUTED));total=text("—",34,INK);total.setTypeface(null,Typeface.BOLD);available.addView(total);balances.addView(available,new LinearLayout.LayoutParams(0,-2,1));LinearLayout monthlyBox=col();monthlyBox.setGravity(Gravity.END);month=text("本月 — / —",11,MUTED);monthlyBox.addView(month);sync=text(lastSync,10,MUTED);monthlyBox.addView(sync);balances.addView(monthlyBox);member.addView(balances);
  progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF1F2F3));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(3));pp.topMargin=dp(10);member.addView(progress,pp);
  LinearLayout actions=card();LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);TextView taskTitle=text("今日任务",16,INK);taskTitle.setTypeface(null,Typeface.BOLD);heading.addView(taskTitle,new LinearLayout.LayoutParams(0,-2,1));refresh=button("刷新",false);refresh.setBackgroundColor(android.graphics.Color.TRANSPARENT);heading.addView(refresh,new LinearLayout.LayoutParams(dp(56),dp(40)));refresh.setOnClickListener(v->refresh());actions.addView(heading);
  tasks=col();actions.addView(tasks);drawTasks();run=button("一键领取积分",true);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(44));rp.topMargin=dp(12);actions.addView(run,rp);run.setOnClickListener(v->startAssist());
  LinearLayout resultRow=new LinearLayout(this);resultRow.setGravity(Gravity.CENTER_VERTICAL);summary=text("任务完成后自动核验到账",11,MUTED);resultRow.addView(summary,new LinearLayout.LayoutParams(0,-2,1));claim=button("领取待领",false);claim.setBackgroundColor(android.graphics.Color.TRANSPARENT);resultRow.addView(claim,new LinearLayout.LayoutParams(dp(82),dp(40)));claim.setOnClickListener(v->claimAll());actions.addView(resultRow);
  cancel=button("停止执行",false);actions.addView(cancel,new LinearLayout.LayoutParams(-1,dp(40)));cancel.setVisibility(View.GONE);cancel.setOnClickListener(v->stop());executionLabel=text("",11,MUTED);actions.addView(executionLabel);executionLabel.setVisibility(View.GONE);executionBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);executionBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));executionBar.setMax(100);actions.addView(executionBar,new LinearLayout.LayoutParams(-1,dp(3)));executionBar.setVisibility(View.GONE);
  notice=text("",11,MUTED);notice.setMaxLines(2);actions.addView(notice);notice.setVisibility(View.GONE);
  Button more=button("执行记录  ›",false);more.setTextColor(MUTED);more.setBackgroundColor(android.graphics.Color.TRANSPARENT);actions.addView(more,new LinearLayout.LayoutParams(-1,dp(36)));logView=text("暂无记录",11,MUTED);logView.setMaxLines(100);logView.setVisibility(View.GONE);actions.addView(logView);more.setOnClickListener(v->{boolean show=logView.getVisibility()!=View.VISIBLE;logView.setVisibility(show?View.VISIBLE:View.GONE);more.setText(show?"收起记录  ⌃":"执行记录  ›");});
  buildExchange();deadline=text("",10,MUTED);deadline.setGravity(Gravity.CENTER);deadline.setPadding(0,dp(16),0,dp(4));content.addView(deadline);TextView foot=text("开发者 π · Powered by GPT\n非哔哩哔哩官方应用",10,MUTED);foot.setGravity(Gravity.CENTER);content.addView(foot);
 }
 void buildExchange(){
  LinearLayout c=card();LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
  TextView title=text("积分兑换",16,INK);title.setTypeface(null,Typeface.BOLD);head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  exchangeRefresh=button("刷新",false);exchangeRefresh.setBackgroundColor(android.graphics.Color.TRANSPARENT);head.addView(exchangeRefresh,new LinearLayout.LayoutParams(dp(56),dp(40)));exchangeRefresh.setOnClickListener(v->execute((a,id)->loadExchange(a,id)));c.addView(head);
  exchangeBalance=text("可用大积分 —",12,PINK);c.addView(exchangeBalance);
  exchangeStatus=text(exchangeMessage,11,MUTED);exchangeStatus.setVisibility(View.GONE);c.addView(exchangeStatus);
  exchangeResult=text("",12,INK);exchangeResult.setPadding(dp(10),dp(8),dp(10),dp(8));exchangeResult.setBackground(bg(0xFFF5F6F7,12));exchangeResult.setVisibility(View.GONE);LinearLayout.LayoutParams resultLayout=new LinearLayout.LayoutParams(-1,-2);resultLayout.setMargins(0,dp(8),0,dp(2));c.addView(exchangeResult,resultLayout);
  exchangeGrid=col();c.addView(exchangeGrid);
  TextView rules=text("兑换结果以官方订单为准",10,MUTED);rules.setPadding(dp(3),dp(10),dp(3),0);c.addView(rules);
 }
 void loadExchange(BiliApi a,long id)throws Exception{
  post(()->exchangeStatus.setText("正在同步官方兑换商品…"));
  try{
   ExchangeCatalog catalog=ExchangeCatalog.load(a,()->check(id));ArrayList<ExchangeSku> all=catalog.items;
   final String message=(all.isEmpty()?"官方当前未返回大会员兑换商品":"已同步官方会员卡 · "+all.size()+"项 · "+new SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(new Date()))+(catalog.detailFailures>0?"；部分详情未获取，请刷新重试":"");
   post(()->{exchangeItems=all;exchangeMessage=message;drawExchange();});
  }catch(Exception e){check(id);if(e instanceof BiliApi.ApiError&&((BiliApi.ApiError)e).code==-101)throw e;
   post(()->{exchangeItems.clear();exchangeMessage="兑换商品同步失败："+safeError(e)+"。可前往官方商城查看";drawExchange();});
  }
 }
 void drawExchange(){
  if(exchangeGrid==null)return;exchangeBalance.setText("可用 "+(points<0?"—":String.valueOf(points))+" 大积分");exchangeStatus.setText(exchangeMessage);exchangeStatus.setVisibility(exchangeItems.isEmpty()||exchangeMessage.contains("部分")?View.VISIBLE:View.GONE);exchangeGrid.removeAllViews();exchangeResult.setText(exchangeResultText);exchangeResult.setVisibility(exchangeResultText.isEmpty()?View.GONE:View.VISIBLE);
  for(int n=0;n<exchangeItems.size();n++){
   ExchangeSku sku=exchangeItems.get(n);if(n>0)line(exchangeGrid);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(12),0,dp(6));
   ImageView picture=new ImageView(this);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);picture.setBackground(bg(0xFFFDEAF0,8));picture.setClipToOutline(true);picture.setContentDescription(sku.title+"官方商品图");row.addView(picture,new LinearLayout.LayoutParams(dp(68),dp(68)));images.load(sku.picture,picture,null);
   LinearLayout info=col();LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1);ip.setMargins(dp(12),0,dp(8),0);row.addView(info,ip);TextView name=text(sku.title,14,INK);name.setTypeface(null,Typeface.BOLD);info.addView(name);TextView price=text(sku.price.isEmpty()?"积分见详情":sku.price+" 大积分",16,PINK);info.addView(price);String state=sku.soldOut?"已售罄":sku.temporarySoldOut?"暂兑完":sku.restriction;info.addView(text(state,11,MUTED));
   Button action=button(sku.soldOut?"已售罄":sku.temporarySoldOut?"暂兑完":"兑换",!sku.soldOut&&!sku.temporarySoldOut);if(!exchangeStore().get(mid+":"+sku.token).isEmpty())action.setText("核验订单");action.setTextSize(12);row.addView(action,new LinearLayout.LayoutParams(dp(80),dp(36)));action.setOnClickListener(v->exchangeClicked(sku));picture.setOnClickListener(v->exchangeClicked(sku));exchangeGrid.addView(row);
   TextView schedule=text(sku.saleInfo(System.currentTimeMillis()),11,MUTED);schedule.setPadding(0,dp(4),0,dp(4));schedule.setVisibility(View.GONE);Button rules=button("兑换时间与规则  ›",false);rules.setTextColor(MUTED);rules.setBackgroundColor(android.graphics.Color.TRANSPARENT);rules.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);rules.setPadding(0,0,0,0);exchangeGrid.addView(rules,new LinearLayout.LayoutParams(-1,dp(36)));exchangeGrid.addView(schedule);rules.setOnClickListener(v->{boolean show=schedule.getVisibility()!=View.VISIBLE;schedule.setVisibility(show?View.VISIBLE:View.GONE);rules.setText(show?"收起规则  ⌃":"兑换时间与规则  ›");});
   // Show a short official schedule by default; preserve the full text in the disclosure.
   String shortTime=!sku.nextSoldText.isEmpty()?sku.nextSoldText:!sku.scheduleNotice.isEmpty()?sku.scheduleNotice.split("[，,。；;]")[0]:"兑换时间以官方实时状态为准";
   if(sku.nextSoldAt>0){SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));shortTime=f.format(new Date(sku.nextSoldAt))+" 开售（北京时间）";}
   TextView timing=text(shortTime,11,MUTED);exchangeGrid.addView(timing,Math.max(0,exchangeGrid.getChildCount()-2));
  }
 }
 void pinkGradient(TextView v){v.getPaint().setShader(null);v.setTextColor(PINK);}
 void accountMenu(){
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("账号与登录 · "+versionName()).setItems(new String[]{"查看当前账号资料","登录 / 切换账号","查看官方积分中心","退出并清除本机会话"},(d,n)->{if(n==0)new AlertDialog.Builder(this).setTitle(user).setMessage("UID "+mid+"\n"+profile.levelText+" · "+profile.experience+"\nB币 "+profile.bcoins+" · 硬币 "+profile.coins+"\n"+vip.state+"\n"+vip.expiry).setPositiveButton("关闭",null).show();else if(n==1)accountAction(()->login());else if(n==2)open(BiliApi.CENTER);else accountAction(()->logout());}).setPositiveButton("检查更新",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->updateClicked()));dialog.show();
 }
 String versionName(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception e){return "未知版本";}}
 void updateClicked(){
  if(updateBurst.tap(SystemClock.elapsedRealtime())){h.removeCallbacks(updateRequest);updateScheduled=false;updateRun++;Toast.makeText(this,"爱你哟，作者微信号lyz--00",Toast.LENGTH_LONG).show();return;}
  if(checkingUpdate||updateScheduled)return;updateScheduled=true;h.postDelayed(updateRequest,2100);
 }
 final Runnable updateRequest=()->{
  updateScheduled=false;if(checkingUpdate||destroyed)return;checkingUpdate=true;final long runId=++updateRun;Toast.makeText(this,"正在检查更新…",Toast.LENGTH_SHORT).show();
  new Thread(()->{try{
   AppUpdates latest=AppUpdates.fetch();int current=getPackageManager().getPackageInfo(getPackageName(),0).versionCode;
   post(()->{checkingUpdate=false;if(runId!=updateRun)return;if(latest.code>current)new AlertDialog.Builder(this).setTitle("发现新版本 "+latest.name).setMessage("当前版本："+versionName()).setPositiveButton("前往仓库下载",(d,w)->openRepository(latest.apk)).show();
    else Toast.makeText(this,latest.statusText(current),Toast.LENGTH_LONG).show();});
  }catch(Exception e){post(()->{checkingUpdate=false;if(runId!=updateRun)return;new AlertDialog.Builder(this).setTitle(e instanceof AppUpdates.AccessRequired?"更新需要仓库访问权限":"检查更新失败").setMessage(AppUpdates.describe(e)).setPositiveButton(e instanceof AppUpdates.AccessRequired?"登录仓库查看":"打开仓库",(d,w)->openRepository(AppUpdates.REPO)).show();});}},"check-update").start();
 };
 void openRepository(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(Exception e){new AlertDialog.Builder(this).setMessage("无法打开浏览器，请访问："+AppUpdates.REPO).setPositiveButton("关闭",null).show();}}
 ExchangeFlow.Store exchangeStore(){
  android.content.SharedPreferences p=getSharedPreferences("exchange-orders",MODE_PRIVATE);
  return new ExchangeFlow.Store(){public String get(String key){return p.getString(key,"");}public boolean put(String key,String order){return p.edit().putString(key,order).commit();}public void remove(String key){p.edit().remove(key).commit();}};
 }
 void showExchangeResult(ExchangeFlow.Result result){exchangeResultText=result.text();drawExchange();note(result.state);new AlertDialog.Builder(this).setTitle(result.state).setMessage(result.reason+(result.order.isEmpty()?"":"\n订单号："+result.order)).setPositiveButton("知道了",null).show();}
 void exchangeClicked(ExchangeSku selected){
  if(busy){note("当前任务执行中，请结束后再兑换");return;}
  execute((a,id)->{
   try{
    JSONObject p=data(a.profile());String uid=p.optString("mid","");if(!p.optBoolean("isLogin")||uid.isEmpty())throw new Exception("登录失效，请重新登录");String key=uid+":"+selected.token,pending=exchangeStore().get(key);
    if(!pending.isEmpty()){ExchangeFlow.Result result=ExchangeFlow.resume(a,key,pending,exchangeStore());post(()->showExchangeResult(result));return;}
    ExchangeFlow.Offer offer=ExchangeFlow.prepare(a,selected.token);check(id);
    post(()->new AlertDialog.Builder(this).setTitle("确认积分兑换").setMessage(offer.sku.title+"\n消耗 "+offer.sku.price+" 大积分\n虚拟权益兑换后不可退换"+(offer.notice.isEmpty()?"":"\n\n"+offer.notice)).setNegativeButton("取消",null).setPositiveButton("确认兑换",(dialog,which)->{
     if(!a.sameSession(api())){note("账号已变化，请重新确认兑换");return;}
     execute((current,jobId)->{
      ExchangeFlow.Result result;
      try{check(jobId);result=ExchangeFlow.redeem(current,offer,exchangeStore(),()->check(jobId));}
      catch(Exception e){result=new ExchangeFlow.Result("兑换失败",safeError(e),"",true);}
      final ExchangeFlow.Result shown=result;post(()->showExchangeResult(shown));
      try{load(current,jobId);loadExchange(current,jobId);}catch(Exception e){post(()->note("兑换结果已保留，账号同步未完成"));}
     });
    }).show());
   }catch(Exception e){if(e instanceof CancellationException)throw e;final String reason=safeError(e);post(()->showExchangeResult(new ExchangeFlow.Result("兑换失败",reason,"",true)));}
  });
 }
 void accountAction(Runnable action){if(!busy){action.run();return;}new AlertDialog.Builder(this).setMessage("切换或退出账号会先停止当前任务，避免混用账号积分。").setPositiveButton("停止并继续",(d,w)->{pendingAccountAction=action;stop();}).setNegativeButton("继续执行",null).show();}

 void login(){if(busy)return;Intent i=new Intent(this,WebActivity.class);i.putExtra("url","https://passport.bilibili.com/login");i.putExtra("login_flow",true);startActivityForResult(i,10);}
 void open(String u){Intent i=new Intent(this,WebActivity.class);i.putExtra("url",u);startActivityForResult(i,10);}
 void logout(){if(busy)return;generation++;CookieManager.getInstance().removeAllCookies(ok->{CookieManager.getInstance().flush();android.webkit.WebStorage.getInstance().deleteAllData();verified=false;profile=new ProfileInfo();user="未登录";mid="";vip=new VipInfo();points=monthly=limit=-1;exchangeItems.clear();exchangeResultText="";exchangeMessage="登录后同步官方兑换商品";lastSyncMs=0;lastSync="尚未同步";items.clear();outcomes.clear();details.clear();evidence.clear();update();note("已退出账号");});}
 BiliApi api(){return new BiliApi(CookieManager.getInstance().getCookie("https://api.bilibili.com"));}
 interface Job{void run(BiliApi a,long id)throws Exception;}
 void execute(Job job){if(busy)return;BiliApi a=api();if(!a.authenticated()){note("请点右上角账号，在官方页面完成登录");return;}cancelled=false;executionPercent=0;taskProgress=new TaskProgress();executionBar.setProgress(0);executionLabel.setText("执行进度 0%");setBusy(true);long id=generation;io.execute(()->{boolean completed=false;try{job.run(a,id);completed=true;}catch(CancellationException e){post(()->note("已停止；已发出的请求请更新积分核验"));}catch(Exception e){post(()->{if(e instanceof BiliApi.ApiError&&((BiliApi.ApiError)e).code==-101){verified=false;profile=new ProfileInfo();vip=new VipInfo();user="登录失效";points=monthly=limit=-1;exchangeItems.clear();exchangeResultText="";exchangeMessage="登录后同步官方兑换商品";items.clear();outcomes.clear();details.clear();evidence.clear();lastSyncMs=0;lastSync="登录失效，请重新核验";update();}note(safeError(e));});}finally{final boolean finished=completed;post(()->{if(finished&&!cancelled){executionPercent=100;executionBar.setProgress(100);executionLabel.setText("流程完成 · 100%（到账见结果）");}else executionLabel.setText((cancelled?"已停止":"执行中断")+" · "+executionPercent+"%");setBusy(false);if(pendingAccountAction!=null){Runnable next=pendingAccountAction;pendingAccountAction=null;next.run();}});}});}
 void phase(int value,String stage){post(()->{if(!busy||cancelled)return;executionPercent=Math.max(executionPercent,Math.min(99,Math.max(0,value)));executionBar.setProgress(executionPercent);executionLabel.setText(stage+" · "+executionPercent+"%");});}
 void taskPhase(TaskItem t,double fraction,String stage){phase(taskProgress.update(t.code,fraction),stage);}
 void check(long id){if(cancelled||destroyed||generation!=id||Thread.currentThread().isInterrupted())throw new CancellationException();}
 void stop(){cancelled=true;cancelBrowsers();for(String k:new ArrayList<String>(outcomes.keySet())){String v=outcomes.get(k);if(v.equals("执行中")||v.equals("核验中")||v.contains("s")){outcomes.put(k,"已停止");details.put(k,"已发送的请求仍需更新积分核验");}}drawTasks();note("正在停止，请等待当前请求结束");}
 void setBusy(boolean on){busy=on;executionLabel.setVisibility(on||executionPercent>0?View.VISIBLE:View.GONE);executionBar.setVisibility(on?View.VISIBLE:View.GONE);exchangeRefresh.setEnabled(!on);run.setEnabled(!on);refresh.setEnabled(!on);claim.setEnabled(!on);cancel.setVisibility(on?View.VISIBLE:View.GONE);if(on)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
 String safeError(Exception e){if(e instanceof BiliApi.ApiError){BiliApi.ApiError x=(BiliApi.ApiError)e;return x.code==6007000?"[6007000] 此任务需最新版B站客户端处理":e.getMessage();}if(e instanceof java.net.SocketTimeoutException)return "请求超时，结果待确认";if(e instanceof java.io.IOException)return "网络异常，请更新积分核验";if(e instanceof JSONException)return "接口数据格式变化，结果待确认";return e.getMessage()==null?"操作失败":e.getMessage();}
 void post(Runnable r){h.post(()->{if(!destroyed)r.run();});}
 void note(String s){notice.setVisibility(View.VISIBLE);notice.setText(s);log.insert(0,new SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(new Date())+"  "+s+"\n");if(log.length()>3500)log.setLength(3500);logView.setText(log.toString());}
 void result(TaskItem t,String state,String detail){post(()->{if(cancelled)return;outcomes.put(t.code,state);details.put(t.code,detail);drawTasks();updateSummary();});}
 void updateSummary(){int yes=0,pending=0,fail=0;for(String s:outcomes.values()){if(s.startsWith("已到账"))yes++;else if(s.equals("待确认"))pending++;else if(s.equals("失败"))fail++;}summary.setText(outcomes.isEmpty()?"任务完成后自动核验到账":"到账 "+yes+" 项"+(pending>0?" · 待确认 "+pending:"")+(fail>0?" · 失败 "+fail:""));}
 JSONObject data(JSONObject j)throws JSONException{JSONObject d=j.optJSONObject("data");if(d==null)throw new JSONException("data缺失");return d;}
 void load(BiliApi a,long id)throws Exception{check(id);phase(15,"同步账号");JSONObject p=data(a.profile());if(!p.optBoolean("isLogin"))throw new BiliApi.ApiError(-101,"登录已失效");String name=p.optString("uname","B站用户"),uid=p.optString("mid","");VipInfo vi=VipInfo.parse(p,System.currentTimeMillis());ProfileInfo pr=ProfileInfo.parse(p);post(()->{if(!mid.isEmpty()&&!mid.equals(uid)){items.clear();outcomes.clear();details.clear();evidence.clear();points=monthly=limit=-1;exchangeItems.clear();exchangeResultText="";exchangeMessage="登录后同步官方兑换商品";lastSyncMs=0;lastSync="等待新账号同步";}user=name;mid=uid;vip=vi;profile=pr;verified=true;update();});check(id);phase(70,"同步积分与任务");JSONObject d=data(a.combine()),pi=d.optJSONObject("point_info"),ti=d.optJSONObject("task_info");if(pi==null||!pi.has("point"))throw new JSONException("积分字段变化");ArrayList<TaskItem> list=TaskItem.parse(d);int pt=pi.optInt("point",-1),mo=ti==null?-1:ti.optInt("score_month",-1),li=ti==null?-1:ti.optInt("score_limit",-1);check(id);post(()->{items=list;points=pt;monthly=mo;limit=li;lastSyncMs=System.currentTimeMillis();lastSync="更新于 "+new SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(new Date());update();});if(!evidence.isEmpty()){JSONArray ledger=records(a);check(id);for(TaskItem t:list){JSONArray before=evidence.get(t.code);int earned=PointEvidence.credited(before,ledger,t);if(t.state==3&&earned>0)result(t,"已到账 +"+earned,"已匹配此任务的新增积分记录");}}}
 void refresh(){execute((a,id)->{load(a,id);loadExchange(a,id);post(()->note("积分与会员状态已更新"));});}
 void update(){total.setText(points<0?"—":String.valueOf(points));month.setText("本月已获 "+(monthly<0?"—":monthly)+" / "+(limit<=0?"—":limit));progress.setMax(limit>0?limit:100);progress.setProgress(monthly>=0&&limit>0?Math.min(monthly,limit):0);sync.setText(lastSync);account.setText(user);pinkGradient(account);uidLabel.setText(mid.isEmpty()?"点击右上角账号登录":"UID "+mid);uidLabel.setTextColor(MUTED);levelLabel.setText(profile.levelText);walletLabel.setText("B币 "+profile.bcoins+"    硬币 "+profile.coins);experienceLabel.setText(profile.experience);vipBadge.setText(vip.state);vipIcon.setContentDescription(vip.state);vipExpiry.setText(vip.expiry.replace("（北京时间）","").replace("\n"," · "));
  if(!imageOwner.equals(mid)||!ProfileImages.officialUrl(profile.face).equals(avatar.getTag())){avatar.setImageResource(android.R.drawable.ic_menu_myplaces);avatar.setTag(null);imageOwner=mid;images.load(profile.face,avatar,null);}
  String badge=vip.active?vip.icon:"";if(!badge.equals(badgeAsset)){badgeAsset=badge;vipIcon.setTag(null);vipIcon.setImageDrawable(null);vipIcon.setVisibility(View.GONE);vipBadge.setVisibility(View.VISIBLE);if(!badge.isEmpty())images.load(badge,vipIcon,()->{vipIcon.setVisibility(View.VISIBLE);vipBadge.setVisibility(View.GONE);});}else if(badge.isEmpty()){vipIcon.setVisibility(View.GONE);vipBadge.setVisibility(View.VISIBLE);}
  drawTasks();updateSummary();drawExchange();
 }

 void drawTasks(){
  if(tasks==null)return;tasks.removeAllViews();if(items.isEmpty()){tasks.addView(text("登录后同步今日任务",12,MUTED));return;}
  int done=0,shown=0;for(TaskItem t:items)if(!t.purchase()&&!t.watch()&&t.state==3)done++;
  for(TaskItem t:items){
   if(t.purchase()||t.watch()||(!showCompleted&&t.state==3&&!outcomes.containsKey(t.code)))continue;
   if(shown++>0){View divider=new View(this);divider.setBackgroundColor(0xFFF1F2F3);tasks.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));}
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(12),0,dp(12));LinearLayout copy=col();row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));TextView title=text(t.title,14,INK);copy.addView(title);
   String state=outcomes.containsKey(t.code)?outcomes.get(t.code):t.state==3?"已完成":t.subtitle.isEmpty()?t.group:t.subtitle;TextView sub=text(state,11,state.startsWith("已到账")?GREEN:MUTED);sub.setMaxLines(2);copy.addView(sub);
   if(t.state!=3){Button action=button(t.reward?"领取":"去完成",false);row.addView(action,new LinearLayout.LayoutParams(dp(76),dp(34)));action.setOnClickListener(v->taskAction(t));}else{TextView complete=text("已完成",12,MUTED);row.addView(complete);}
   if(details.containsKey(t.code)){row.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(t.title).setMessage(details.get(t.code)).setPositiveButton("关闭",null).show());}
   tasks.addView(row);
  }
  if(shown==0)tasks.addView(text("今日任务已完成",12,MUTED));
  if(done>0){Button completed=button((showCompleted?"收起已完成":"已完成 "+done+" 项")+"  ›",false);completed.setTextColor(MUTED);completed.setBackgroundColor(android.graphics.Color.TRANSPARENT);tasks.addView(completed,new LinearLayout.LayoutParams(-1,dp(34)));completed.setOnClickListener(v->{showCompleted=!showCompleted;drawTasks();});}
 }

 void taskAction(TaskItem t){if(busy)return;if(t.purchase()||t.watch())return;execute((a,id)->{process(a,t,id);load(a,id);post(()->note("此项执行结束，请查看任务结果"));});}

 TaskItem find(JSONObject d,String code)throws JSONException{for(TaskItem t:TaskItem.parse(d))if(t.code.equals(code))return t;return null;}
 JSONArray records(BiliApi a){try{return PointEvidence.list(a.records());}catch(Exception e){return null;}}
 String destination(TaskItem t){switch(t.browseKind()){case "animatetab":return "https://www.bilibili.com/anime/";case "filmtab":return "https://www.bilibili.com/movie/";case "vipmallview":return "https://show.bilibili.com/platform/home.html";case "dressview":return "https://www.bilibili.com/h5/mall/home";default:return BiliApi.CENTER;}}
 void cancelBrowsers(){for(SilentBrowser b:new ArrayList<SilentBrowser>(browsers.values()))b.cancel();browsers.clear();}
 void page(TaskItem t,long id)throws Exception{
  CountDownLatch done=new CountDownLatch(1);boolean[] success={false};String[] reason={"任务页未执行"};
  post(()->{if(cancelled||generation!=id){done.countDown();return;}SilentBrowser page=new SilentBrowser(this,root);browsers.put(t.code,page);page.start(destination(t),t.browseKind(),t.browseSeconds(),new SilentBrowser.Listener(){int last=Integer.MIN_VALUE;
   public void progress(int left,String state){if(left==last||cancelled)return;last=left;taskPhase(t,left<0?.5:.2+.6*(12-left)/12.0,left<0?"打开装扮商城":"并行浏览");outcomes.put(t.code,left<0?state:state+" "+left+"s");details.put(t.code,left<0?"页面加载后立即核验实际到账":"本页执行，完成后独立核验到账");drawTasks();}
   public void finished(boolean ok,String why){success[0]=ok;reason[0]=why;browsers.remove(t.code);done.countDown();}
  });page.setActive(active);});
  try{long until=SystemClock.elapsedRealtime()+50000;while(!done.await(250,TimeUnit.MILLISECONDS)){check(id);if(SystemClock.elapsedRealtime()>until)throw new Exception("任务页执行超时");}check(id);if(!success[0])throw new Exception(reason[0]);}
  finally{post(()->{SilentBrowser page=browsers.remove(t.code);if(page!=null)page.cancel();});}
 }
 void waitCheck(long id)throws Exception{for(int n=0;n<8;n++){check(id);Thread.sleep(250);}}
 boolean process(BiliApi a,TaskItem original,long id)throws Exception{check(id);TaskItem t=find(data(a.combine()),original.code);if(t==null){result(original,"失败","任务已失效，请更新");return true;}if(t.purchase()||t.watch())return true;if(t.state==3){result(t,"已完成","服务端已完成，不重复执行");return true;}if(!t.browse()&&!t.reward){if(t.state==0)synchronized(mutations){check(id);a.receive(t);}result(t,"需手动","此类任务需要在B站官方客户端实际完成");return true;}taskPhase(t,.1,"准备任务");result(t,"执行中","正在接取任务并准备处理");JSONArray baseline=records(a);if(baseline!=null)evidence.put(t.code,baseline);check(id);
  if(t.state==0){synchronized(mutations){check(id);a.receive(t);}waitCheck(id);TaskItem accepted=find(data(a.combine()),t.code);if(accepted==null||accepted.state==0){result(t,"待确认","服务端尚未确认任务接取，不继续提交");return false;}}
  String submissionError="";check(id);if(t.browse()){page(t,id);check(id);boolean submit=true;if(t.browseSeconds()==0){TaskItem opened=find(data(a.combine()),t.code);check(id);if(opened==null){result(t,"待确认","页面已打开，但任务状态未能匹配，请更新积分核验");return false;}submit=opened.state!=3;}if(submit)try{synchronized(mutations){check(id);a.completeBrowse(t);}}catch(Exception e){if(t.browseSeconds()!=0)throw e;fatal(e);submissionError=safeError(e);}}else synchronized(mutations){check(id);a.claim(t);}
  taskPhase(t,.85,"核验到账");result(t,"核验中","正在查询服务端任务状态和新增积分记录");boolean completed=false;int earned=0;for(int n=0;n<3;n++){check(id);if(n>0)waitCheck(id);TaskItem after=find(data(a.combine()),t.code);completed=after!=null&&after.state==3;earned=PointEvidence.credited(baseline,records(a),t);if(completed&&earned>0)break;}
  check(id);if(completed&&earned>0){evidence.remove(t.code);result(t,"已到账 +"+earned,"已匹配此任务的新增积分记录");}else result(t,"待确认",completed?"服务端任务已完成，尚未查到对应新增积分记录":(submissionError.isEmpty()?"服务端尚未确认完成；页面处理结束不代表到账":"提交未确认："+submissionError+"；尚无对应到账记录"));return completed&&earned>0;
 }
 void fatal(Exception e)throws Exception{if(e instanceof CancellationException)throw e;if(e instanceof BiliApi.ApiError){int c=((BiliApi.ApiError)e).code;if(c==-101||c==-111||c==-352||c==-412||c==403||c==412||c==429)throw e;}}
 void startAssist(){execute((a,id)->{evidence.clear();post(()->{outcomes.clear();details.clear();drawTasks();note("开始执行，独立任务将并行处理；请保持本页运行");});check(id);try{JSONObject s=data(a.get("/x/vip/vip_center/sign_in/three_days_sign?csrf="+BiliApi.enc(a.csrf()))),today=s.optJSONObject("three_day_sign");if(today!=null&&today.has("signed")){if(!today.optBoolean("signed")){a.sign();check(id);}post(()->note("签到检查已完成，开始处理日常任务"));}}catch(Exception e){fatal(e);post(()->note("签到未确认："+safeError(e)));}
  DailyTaskPlan plan=new DailyTaskPlan(TaskItem.parse(data(a.combine())));
  taskProgress=new TaskProgress(plan.codes);ArrayList<TaskBatch.Work> work=new ArrayList<>();
  for(ArrayList<TaskItem> group:plan.groups.values())work.add(()->{boolean unresolved=false;for(TaskItem t:group){check(id);if(unresolved){result(t,"未执行","同类任务结果待确认，避免重复提交或混计积分");taskPhase(t,1,"等待核验");continue;}try{boolean settled=process(a,t,id);if(t.browse()&&!settled)unresolved=true;}catch(Exception e){fatal(e);result(t,"失败",safeError(e));if(t.browse())unresolved=true;}taskPhase(t,1,"任务核验");}});
  TaskBatch.run(work,3,()->{cancelled=true;post(()->cancelBrowsers());});check(id);phase(95,"最终同步");load(a,id);post(()->note("本轮执行结束，到账情况请查看各项结果"));});}
 void claimAll(){execute((a,id)->{ArrayList<TaskItem> list=TaskItem.parse(data(a.combine()));int n=0;for(TaskItem t:list){check(id);if(t.reward&&!t.purchase()&&!t.watch()){try{process(a,t,id);}catch(Exception e){fatal(e);result(t,"失败",safeError(e));}n++;}}load(a,id);final int count=n;post(()->note(count==0?"暂无服务端明确标记的待领奖励":"领取请求已处理，详情见各项核验结果"));});}
 @Override protected void onActivityResult(int r,int code,Intent value){super.onActivityResult(r,code,value);if(r==10&&!busy)refresh();}
 final Runnable clock=new Runnable(){public void run(){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));c.set(Calendar.HOUR_OF_DAY,23);c.set(Calendar.MINUTE,59);c.set(Calendar.SECOND,59);long left=Math.max(0,(c.getTimeInMillis()-System.currentTimeMillis())/1000);deadline.setText(String.format(Locale.CHINA,"今日剩余 %02d:%02d:%02d  ·  北京时间",left/3600,left%3600/60,left%60));if(lastSyncMs>0)sync.setText(lastSync+(System.currentTimeMillis()-lastSyncMs>120000?" · 待刷新":""));h.postDelayed(this,1000);}};
 final Runnable periodic=new Runnable(){public void run(){if(active&&!busy&&api().authenticated())refresh();if(active)h.postDelayed(this,60000);}};
 protected void onResume(){super.onResume();active=true;for(SilentBrowser b:browsers.values())b.setActive(true);h.removeCallbacks(periodic);h.postDelayed(periodic,60000);}
 protected void onPause(){active=false;for(SilentBrowser b:browsers.values())b.setActive(false);h.removeCallbacks(periodic);super.onPause();}
 protected void onDestroy(){cancelled=true;destroyed=true;generation++;cancelBrowsers();h.removeCallbacksAndMessages(null);io.shutdownNow();images.close();super.onDestroy();}
}
