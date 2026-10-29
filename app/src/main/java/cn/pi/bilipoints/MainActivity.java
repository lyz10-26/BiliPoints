package cn.pi.bilipoints;
import android.app.*;import android.os.*;import android.view.*;import android.widget.*;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.content.*;import android.webkit.CookieManager;import org.json.*;import java.util.*;import java.util.concurrent.*;import java.text.SimpleDateFormat;

public class MainActivity extends Activity {
 final int PINK=0xFFEB4D87,INK=0xFF222330,MUTED=0xFF7F8293,GREEN=0xFF16886A;
 Handler h=new Handler();ExecutorService io=Executors.newSingleThreadExecutor();FrameLayout root;LinearLayout content,tasks;TextView total,month,deadline,account,notice,sync,logView,vipBadge,vipExpiry,summary,levelLabel,experienceLabel,uidLabel;LinearLayout exchangeGrid;TextView exchangeBalance,exchangeStatus,exchangeResult;String exchangeResultText="";boolean checkingUpdate=false;Button exchangeRefresh;ArrayList<ExchangeSku> exchangeItems=new ArrayList<>();String exchangeMessage="登录后同步官方兑换商品";TextView walletLabel,executionLabel;ProgressBar executionBar;int executionPercent=0;TaskProgress taskProgress=new TaskProgress();Runnable pendingAccountAction;TapBurst updateBurst=new TapBurst();boolean updateScheduled=false;long updateRun=0;ImageView avatar,vipIcon;ProfileInfo profile=new ProfileInfo();ProfileImages images=new ProfileImages();String imageOwner="",badgeAsset="";ProgressBar progress;Button run,claim,refresh,cancel;final HashMap<String,SilentBrowser> browsers=new HashMap<>();final Object mutations=new Object();
 volatile boolean cancelled=false,destroyed=false;boolean busy=false,active=false,verified=false,showCompleted=false;volatile long generation=0;long lastSyncMs=0;String user="未登录",mid="",lastSync="尚未同步";VipInfo vip=new VipInfo();int points=-1,monthly=-1,limit=-1;
 ConcurrentHashMap<String,JSONArray> evidence=new ConcurrentHashMap<>();ArrayList<TaskItem> items=new ArrayList<>();HashMap<String,String> outcomes=new HashMap<>();HashMap<String,String> details=new HashMap<>();StringBuilder log=new StringBuilder();
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xFFF7F6FA);getWindow().setNavigationBarColor(0xFFF7F6FA);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);build();h.post(clock);h.postDelayed(()->{if(api().authenticated())refresh();},350);}
 int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}LinearLayout col(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);return c;}
 TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(android.text.Html.fromHtml(s.replace("\n","<br>"),android.text.Html.FROM_HTML_MODE_COMPACT));v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));v.setIncludeFontPadding(false);v.setLineSpacing(dp(2),1);v.setPadding(0,dp(2),0,dp(2));return v;}
 GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 Button button(String title,boolean solid){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(solid?0xFFFFFFFF:PINK);b.setBackground(bg(solid?PINK:0xFFFFEAF1,22));b.setMinHeight(dp(44));b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(12),dp(4),dp(12),dp(4));b.setElevation(0);b.setStateListAnimator(null);return b;}
 LinearLayout card(){LinearLayout c=col();c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(bg(0xFFFFFFFF,22));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(8),0,0);content.addView(c,p);return c;}
 void pair(LinearLayout row,Button b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(dp(4),dp(10),dp(4),0);row.addView(b,p);}
 void build(){root=new FrameLayout(this);root.setBackgroundColor(0xFFF7F6FA);setContentView(root);root.setOnApplyWindowInsetsListener((v,w)->{v.setPadding(w.getSystemWindowInsetLeft(),w.getSystemWindowInsetTop(),w.getSystemWindowInsetRight(),w.getSystemWindowInsetBottom());return w;});root.requestApplyInsets();ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xFFF7F6FA);content=col();content.setPadding(dp(14),dp(10),dp(14),dp(16));scroll.addView(content);root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
  FrameLayout top=new FrameLayout(this);TextView title=text("大积分助手",22,INK);title.setTypeface(null,Typeface.BOLD);title.setGravity(Gravity.CENTER);top.addView(title,new FrameLayout.LayoutParams(-1,dp(48)));Button settings=button("账号",false);FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(dp(62),dp(40),Gravity.END|Gravity.CENTER_VERTICAL);top.addView(settings,sp);settings.setOnClickListener(v->accountMenu());content.addView(top,new LinearLayout.LayoutParams(-1,dp(48)));
  LinearLayout member=card();member.setPadding(dp(16),dp(14),dp(16),dp(14));LinearLayout badgeRow=new LinearLayout(this);badgeRow.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);vipBadge=text("待同步",12,PINK);badgeRow.addView(vipBadge);vipIcon=new ImageView(this);vipIcon.setScaleType(ImageView.ScaleType.FIT_END);vipIcon.setContentDescription("B站官方会员徽标");vipIcon.setVisibility(View.GONE);badgeRow.addView(vipIcon,new LinearLayout.LayoutParams(dp(108),dp(22)));
  LinearLayout identity=new LinearLayout(this);identity.setGravity(Gravity.CENTER_VERTICAL);avatar=new ImageView(this);avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);avatar.setBackground(bg(0xFFFFEAF1,40));avatar.setClipToOutline(true);avatar.setImageResource(android.R.drawable.ic_menu_myplaces);avatar.setContentDescription("当前账号头像");identity.addView(avatar,new LinearLayout.LayoutParams(dp(76),dp(76)));LinearLayout names=col();LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-2,1);np.setMargins(dp(10),0,0,0);identity.addView(names,np);account=text("未登录",16,INK);account.setTypeface(null,Typeface.BOLD);LinearLayout nameRow=new LinearLayout(this);nameRow.setGravity(Gravity.CENTER_VERTICAL);account.setSingleLine(true);account.setEllipsize(android.text.TextUtils.TruncateAt.END);nameRow.addView(account,new LinearLayout.LayoutParams(0,-2,1));badgeRow.removeAllViews();vipIcon.setScaleType(ImageView.ScaleType.FIT_END);LinearLayout status=new LinearLayout(this);status.setGravity(Gravity.END);status.addView(vipBadge);status.addView(vipIcon,new LinearLayout.LayoutParams(dp(88),dp(20)));nameRow.addView(status);names.addView(nameRow);uidLabel=text("点击右上角账号登录",11,MUTED);LinearLayout idRow=new LinearLayout(this);idRow.setGravity(Gravity.CENTER_VERTICAL);uidLabel.setTextSize(10);idRow.addView(uidLabel,new LinearLayout.LayoutParams(0,-2,1));levelLabel=text("等级 —",12,PINK);idRow.addView(levelLabel);names.addView(idRow);walletLabel=text("B币 —    硬币 —",12,MUTED);names.addView(walletLabel);experienceLabel=text("经验 —",11,MUTED);names.addView(experienceLabel);vipExpiry=text("登录后显示会员有效期",11,MUTED);vipExpiry.setMaxLines(2);names.addView(vipExpiry);member.addView(identity);
  View divider=new View(this);divider.setBackgroundColor(0xFFF0F0F4);LinearLayout.LayoutParams d=new LinearLayout.LayoutParams(-1,dp(1));d.setMargins(0,dp(12),0,dp(10));member.addView(divider,d);total=text("—",36,PINK);total.setGravity(Gravity.CENTER);total.setTypeface(null,Typeface.BOLD);member.addView(total,new LinearLayout.LayoutParams(-1,-2));TextView caption=text("可用大积分",12,MUTED);caption.setGravity(Gravity.CENTER);member.addView(caption);
  LinearLayout monthRow=new LinearLayout(this);monthRow.setGravity(Gravity.CENTER_VERTICAL);month=text("本月 — / —",12,INK);monthRow.addView(month,new LinearLayout.LayoutParams(0,-2,1));sync=text(lastSync,10,MUTED);monthRow.addView(sync);member.addView(monthRow);progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));member.addView(progress,new LinearLayout.LayoutParams(-1,dp(4)));

  LinearLayout actions=card();TextView actionTitle=text("任务执行",16,INK);actionTitle.setTypeface(null,Typeface.BOLD);actions.addView(actionTitle);summary=text("独立浏览任务最多 3 项并行，结果逐项核验",12,MUTED);actions.addView(summary);run=button("一键执行 · 并行核验",true);actions.addView(run,new LinearLayout.LayoutParams(-1,dp(44)));run.setOnClickListener(v->startAssist());LinearLayout ar=new LinearLayout(this);refresh=button("更新积分",false);claim=button("领取待领积分",false);pair(ar,refresh);pair(ar,claim);actions.addView(ar);refresh.setOnClickListener(v->refresh());claim.setOnClickListener(v->claimAll());cancel=button("停止执行",false);LinearLayout.LayoutParams stopLayout=new LinearLayout.LayoutParams(-1,dp(44));stopLayout.setMargins(dp(3),dp(10),dp(3),dp(8));actions.addView(cancel,stopLayout);cancel.setVisibility(View.GONE);cancel.setOnClickListener(v->stop());executionLabel=text("执行进度 0%",12,MUTED);actions.addView(executionLabel);executionBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);executionBar.setMax(100);executionBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));actions.addView(executionBar,new LinearLayout.LayoutParams(-1,dp(4)));notice=text("最多 3 项浏览并行 · 同类任务按顺序处理",11,MUTED);notice.setMaxLines(2);actions.addView(notice);
  LinearLayout logs=card();logs.addView(text("执行详情",15,INK));logView=text("暂无记录",12,MUTED);logView.setMaxLines(2);logs.addView(logView);Button more=button("展开 / 收起",false);logs.addView(more);more.setOnClickListener(v->logView.setMaxLines(logView.getMaxLines()==2?100:2));
  deadline=text("",11,MUTED);deadline.setPadding(0,dp(10),0,dp(3));content.addView(deadline);tasks=col();content.addView(tasks);drawTasks();buildExchange();TextView foot=text("© "+Calendar.getInstance().get(Calendar.YEAR)+" 开发者 π · Powered by GPT\n非哔哩哔哩官方应用 · v1.1.2 release\nBilibili相关商标与内容归各自权利人所有",11,MUTED);foot.setPadding(dp(6),dp(16),dp(6),dp(4));foot.setGravity(Gravity.CENTER);content.addView(foot);
 }
 void buildExchange(){
  LinearLayout c=card();LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
  TextView title=text("积分兑换大会员",16,INK);title.setTypeface(null,Typeface.BOLD);head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  exchangeRefresh=button("刷新商品",false);head.addView(exchangeRefresh,new LinearLayout.LayoutParams(dp(82),dp(44)));exchangeRefresh.setOnClickListener(v->execute((a,id)->loadExchange(a,id)));c.addView(head);
  exchangeBalance=text("可用大积分 —",12,PINK);c.addView(exchangeBalance);
  exchangeStatus=text(exchangeMessage,11,MUTED);exchangeStatus.setMaxLines(2);c.addView(exchangeStatus);
  exchangeResult=text("",12,INK);exchangeResult.setPadding(dp(10),dp(8),dp(10),dp(8));exchangeResult.setBackground(bg(0xFFF7F6FA,12));exchangeResult.setVisibility(View.GONE);LinearLayout.LayoutParams resultLayout=new LinearLayout.LayoutParams(-1,-2);resultLayout.setMargins(0,dp(8),0,dp(2));c.addView(exchangeResult,resultLayout);
  exchangeGrid=col();c.addView(exchangeGrid);
  TextView rules=text("兑换结果以官方订单为准",10,MUTED);rules.setPadding(dp(3),dp(10),dp(3),0);c.addView(rules);
 }
 void loadExchange(BiliApi a,long id)throws Exception{
  post(()->exchangeStatus.setText("正在同步官方兑换商品…"));
  try{
   ArrayList<ExchangeSku> all=new ArrayList<>();HashSet<String> seen=new HashSet<>();boolean more=false;int count=0;
   for(int page=1;page<=5;page++){
    check(id);JSONObject d=data(a.exchangeSkus(page));JSONArray skus=d.optJSONArray("skus");if(skus==null)throw new JSONException("官方兑换商品列表格式变化");
    for(ExchangeSku s:ExchangeSku.parse(d))if(seen.add(s.token))all.add(s);
    count+=skus.length();JSONObject pagination=d.optJSONObject("page");int total=pagination==null?-1:pagination.optInt("total",-1);
    more=skus.length()>=20&&(total<0||count<total);if(!more)break;
   }
   int lookedUp=0;
   for(int n=0;n<all.size()&&lookedUp<10;n++){
    ExchangeSku sku=all.get(n);if((sku.temporarySoldOut||sku.soldOut)&&sku.nextSoldAt==0){lookedUp++;check(id);try{JSONObject full=data(a.exchangeInfo(sku.token));if(sku.token.equals(full.optString("token")))all.set(n,sku.withInventory(full));}catch(Exception e){if(e instanceof BiliApi.ApiError&&((BiliApi.ApiError)e).code==-101)throw e;}}
   }
   check(id);final String message=(all.isEmpty()?"本次未获取到大会员兑换商品，可前往官方商城查看": "商品信息已同步 · 兑换结果在本页显示")+(more?"；已扫描前100项，更多见官方商城":"");
   post(()->{exchangeItems=all;exchangeMessage=message;drawExchange();});
  }catch(Exception e){check(id);if(e instanceof BiliApi.ApiError&&((BiliApi.ApiError)e).code==-101)throw e;
   post(()->{exchangeItems.clear();exchangeMessage="兑换商品同步失败："+safeError(e)+"。可前往官方商城查看";drawExchange();});
  }
 }
 void drawExchange(){
  if(exchangeGrid==null)return;exchangeBalance.setText("可用大积分 "+(points<0?"—":String.valueOf(points)));exchangeStatus.setText(exchangeMessage);exchangeGrid.removeAllViews();exchangeResult.setText(exchangeResultText);exchangeResult.setVisibility(exchangeResultText.isEmpty()?View.GONE:View.VISIBLE);
  LinearLayout row=null;
  for(int n=0;n<exchangeItems.size();n++){
   ExchangeSku sku=exchangeItems.get(n);
   if(n%2==0){row=new LinearLayout(this);row.setBaselineAligned(false);row.setGravity(Gravity.TOP);exchangeGrid.addView(row,new LinearLayout.LayoutParams(-1,-2));}
   LinearLayout tile=col();tile.setPadding(dp(9),dp(9),dp(9),dp(10));tile.setBackground(bg(0xFFFFF7FA,16));
   // The horizontal row gives both cards its maximum measured height. A flexible spacer
   // absorbs different title/schedule heights so every footer and action shares a baseline.
   LinearLayout.LayoutParams tileLayout=new LinearLayout.LayoutParams(0,-1,1);tileLayout.setMargins(dp(3),dp(8),dp(3),0);row.addView(tile,tileLayout);
   ImageView picture=new ImageView(this);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);picture.setBackground(bg(0xFFFFEAF1,12));picture.setClipToOutline(true);picture.setContentDescription(sku.title+"官方商品图");tile.addView(picture,new LinearLayout.LayoutParams(-1,dp(112)));images.load(sku.picture,picture,null);
   TextView name=text("",13,INK);name.setText(sku.title);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setTypeface(null,Typeface.BOLD);name.setPadding(0,dp(8),0,dp(3));tile.addView(name);
   String sale=sku.saleInfo(System.currentTimeMillis());String label=!sale.isEmpty()?sale:(!sku.promotion.isEmpty()?sku.promotion+" ":"")+sku.restriction;
   label=label.replace("暂兑完 · ","暂兑完\n").replace("已售罄 · ","已售罄\n");
   TextView tags=text("",10,MUTED);tags.setText(label);tags.setMaxLines(4);tags.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(tags);
   tile.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));
   LinearLayout footer=col();LinearLayout.LayoutParams footerLayout=new LinearLayout.LayoutParams(-1,-2);footerLayout.topMargin=dp(8);tile.addView(footer,footerLayout);
   TextView price=text("",18,PINK);String priceText=sku.price.isEmpty()?"积分见详情":sku.price+" 大积分";
   android.text.SpannableString amount=new android.text.SpannableString(priceText);if(!sku.price.isEmpty())amount.setSpan(new android.text.style.RelativeSizeSpan(.62f),sku.price.length(),priceText.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
   price.setText(amount);price.setSingleLine(true);price.setAutoSizeTextTypeUniformWithConfiguration(12,18,1,android.util.TypedValue.COMPLEX_UNIT_SP);price.setTypeface(null,Typeface.BOLD);footer.addView(price,new LinearLayout.LayoutParams(-1,dp(28)));
   boolean reserveOriginal=false;for(int peer=n-n%2;peer<Math.min(n-n%2+2,exchangeItems.size());peer++){ExchangeSku other=exchangeItems.get(peer);if(!other.origin.isEmpty()&&!other.price.equals(other.origin))reserveOriginal=true;}
   TextView old=text("",10,MUTED);old.setSingleLine(true);old.setText(sku.origin+" 大积分");old.setPaintFlags(old.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);old.setVisibility(!reserveOriginal?View.GONE:!sku.origin.isEmpty()&&!sku.price.equals(sku.origin)?View.VISIBLE:View.INVISIBLE);footer.addView(old,new LinearLayout.LayoutParams(-1,dp(16)));
   Button action=button(sku.soldOut?"已售罄":sku.temporarySoldOut?"暂兑完":"去兑换",!sku.soldOut&&!sku.temporarySoldOut);action.setSingleLine(true);action.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);
   if(!exchangeStore().get(mid+":"+sku.token).isEmpty())action.setText("核验上次兑换");LinearLayout.LayoutParams actionLayout=new LinearLayout.LayoutParams(-1,dp(44));actionLayout.topMargin=dp(7);footer.addView(action,actionLayout);action.setOnClickListener(v->exchangeClicked(sku));picture.setOnClickListener(v->exchangeClicked(sku));
  }
  if(row!=null&&exchangeItems.size()%2==1){LinearLayout.LayoutParams empty=new LinearLayout.LayoutParams(0,1,1);empty.setMargins(dp(3),dp(8),dp(3),0);row.addView(new View(this),empty);}
 }
 void pinkGradient(TextView v){v.post(()->{float width=Math.max(1,v.getPaint().measureText(v.getText().toString()));v.getPaint().setShader(new android.graphics.LinearGradient(0,0,width,0,new int[]{0xFFFF82B3,0xFFE63B83},null,android.graphics.Shader.TileMode.CLAMP));v.invalidate();});}
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
 void setBusy(boolean on){busy=on;exchangeRefresh.setEnabled(!on);run.setEnabled(!on);refresh.setEnabled(!on);claim.setEnabled(!on);cancel.setVisibility(on?View.VISIBLE:View.GONE);if(on)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
 String safeError(Exception e){if(e instanceof BiliApi.ApiError){BiliApi.ApiError x=(BiliApi.ApiError)e;return x.code==6007000?"[6007000] 此任务需最新版B站客户端处理":e.getMessage();}if(e instanceof java.net.SocketTimeoutException)return "请求超时，结果待确认";if(e instanceof java.io.IOException)return "网络异常，请更新积分核验";if(e instanceof JSONException)return "接口数据格式变化，结果待确认";return e.getMessage()==null?"操作失败":e.getMessage();}
 void post(Runnable r){h.post(()->{if(!destroyed)r.run();});}
 void note(String s){notice.setText(s);log.insert(0,new SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(new Date())+"  "+s+"\n");if(log.length()>3500)log.setLength(3500);logView.setText(log.toString());}
 void result(TaskItem t,String state,String detail){post(()->{if(cancelled)return;outcomes.put(t.code,state);details.put(t.code,detail);drawTasks();updateSummary();});}
 void updateSummary(){int yes=0,pending=0,fail=0;for(String s:outcomes.values()){if(s.startsWith("已到账"))yes++;else if(s.equals("待确认"))pending++;else if(s.equals("失败"))fail++;}summary.setText("已到账 "+yes+" 项   ·   待确认 "+pending+" 项   ·   失败 "+fail+" 项");}
 JSONObject data(JSONObject j)throws JSONException{JSONObject d=j.optJSONObject("data");if(d==null)throw new JSONException("data缺失");return d;}
 void load(BiliApi a,long id)throws Exception{check(id);phase(15,"同步账号");JSONObject p=data(a.profile());if(!p.optBoolean("isLogin"))throw new BiliApi.ApiError(-101,"登录已失效");String name=p.optString("uname","B站用户"),uid=p.optString("mid","");VipInfo vi=VipInfo.parse(p,System.currentTimeMillis());ProfileInfo pr=ProfileInfo.parse(p);post(()->{if(!mid.isEmpty()&&!mid.equals(uid)){items.clear();outcomes.clear();details.clear();evidence.clear();points=monthly=limit=-1;exchangeItems.clear();exchangeResultText="";exchangeMessage="登录后同步官方兑换商品";lastSyncMs=0;lastSync="等待新账号同步";}user=name;mid=uid;vip=vi;profile=pr;verified=true;update();});check(id);phase(70,"同步积分与任务");JSONObject d=data(a.combine()),pi=d.optJSONObject("point_info"),ti=d.optJSONObject("task_info");if(pi==null||!pi.has("point"))throw new JSONException("积分字段变化");ArrayList<TaskItem> list=TaskItem.parse(d);int pt=pi.optInt("point",-1),mo=ti==null?-1:ti.optInt("score_month",-1),li=ti==null?-1:ti.optInt("score_limit",-1);check(id);post(()->{items=list;points=pt;monthly=mo;limit=li;lastSyncMs=System.currentTimeMillis();lastSync="更新于 "+new SimpleDateFormat("HH:mm:ss",Locale.CHINA).format(new Date());update();});if(!evidence.isEmpty()){JSONArray ledger=records(a);check(id);for(TaskItem t:list){JSONArray before=evidence.get(t.code);int earned=PointEvidence.credited(before,ledger,t);if(t.state==3&&earned>0)result(t,"已到账 +"+earned,"已匹配此任务的新增积分记录");}}}
 void refresh(){execute((a,id)->{load(a,id);loadExchange(a,id);post(()->note("积分与会员状态已更新"));});}
 void update(){total.setText(points<0?"—":String.valueOf(points));month.setText("本月已获 "+(monthly<0?"—":monthly)+" / "+(limit<=0?"—":limit));progress.setMax(limit>0?limit:100);progress.setProgress(monthly>=0&&limit>0?Math.min(monthly,limit):0);sync.setText(lastSync);account.setText(user);pinkGradient(account);uidLabel.setText(mid.isEmpty()?"点击右上角账号登录":"UID "+mid);pinkGradient(uidLabel);levelLabel.setText(profile.levelText);walletLabel.setText("B币 "+profile.bcoins+"    硬币 "+profile.coins);experienceLabel.setText(profile.experience);vipBadge.setText(vip.state);vipIcon.setContentDescription(vip.state);vipExpiry.setText(vip.expiry.replace("（北京时间）","").replace("\n"," · "));
  if(!imageOwner.equals(mid)||!ProfileImages.officialUrl(profile.face).equals(avatar.getTag())){avatar.setImageResource(android.R.drawable.ic_menu_myplaces);avatar.setTag(null);imageOwner=mid;images.load(profile.face,avatar,null);}
  String badge=vip.active?vip.icon:"";if(!badge.equals(badgeAsset)){badgeAsset=badge;vipIcon.setTag(null);vipIcon.setImageDrawable(null);vipIcon.setVisibility(View.GONE);vipBadge.setVisibility(View.VISIBLE);if(!badge.isEmpty())images.load(badge,vipIcon,()->{vipIcon.setVisibility(View.VISIBLE);vipBadge.setVisibility(View.GONE);});}else if(badge.isEmpty()){vipIcon.setVisibility(View.GONE);vipBadge.setVisibility(View.VISIBLE);}
  drawTasks();updateSummary();drawExchange();
 }

 void drawTasks(){if(tasks==null)return;tasks.removeAllViews();if(items.isEmpty()){tasks.addView(text("登录并更新后显示任务",13,MUTED));return;}int done=0;for(TaskItem t:items)if(!t.purchase()&&!t.watch()&&t.state==3)done++;if(done>0){Button b=button((showCompleted?"收起":"显示")+"已完成（"+done+"）",false);tasks.addView(b,new LinearLayout.LayoutParams(-1,dp(40)));b.setOnClickListener(v->{showCompleted=!showCompleted;drawTasks();});}
  TextView head=text("日常任务",15,INK);head.setTypeface(null,Typeface.BOLD);head.setPadding(0,dp(10),0,dp(4));tasks.addView(head);int shown=0;
  for(TaskItem t:items){if(t.purchase()||t.watch()||(!showCompleted&&t.state==3&&!outcomes.containsKey(t.code)))continue;shown++;LinearLayout c=col();c.setPadding(dp(12),dp(10),dp(12),dp(10));c.setBackground(bg(0xFFFFFFFF,16));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(4),0,dp(4));tasks.addView(c,cp);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView title=text(t.title,14,INK);title.setTypeface(null,Typeface.BOLD);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));String state=outcomes.containsKey(t.code)?outcomes.get(t.code):t.state==3?"已完成":"待执行";TextView badge=text(state,11,state.startsWith("已到账")?GREEN:state.equals("失败")?0xFFC14646:PINK);row.addView(badge);c.addView(row);TextView sub=text(t.subtitle.isEmpty()?t.group:t.subtitle,12,MUTED);sub.setMaxLines(2);c.addView(sub);
   if(details.containsKey(t.code)){TextView dt=text(details.get(t.code),11,MUTED);dt.setMaxLines(2);c.addView(dt);}if(t.state!=3){LinearLayout foot=new LinearLayout(this);foot.setGravity(Gravity.END);Button action=button(t.browse()?"执行并核验":t.reward?"领取积分":"检查任务",false);foot.addView(action,new LinearLayout.LayoutParams(dp(112),dp(44)));c.addView(foot);action.setOnClickListener(v->taskAction(t));}
  }if(shown==0)tasks.addView(text("暂无待处理的日常任务",12,MUTED));
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
