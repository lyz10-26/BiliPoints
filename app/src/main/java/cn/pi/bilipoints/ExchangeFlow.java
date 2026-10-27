package cn.pi.bilipoints;
import org.json.*;
/** Official verify/create/detail flow. Order creation is never retried automatically. */
public final class ExchangeFlow {
 public interface Store {String get(String key);boolean put(String key,String order);void remove(String key);}
 public static final class Offer {
  public final ExchangeSku sku;public final String key,notice;
  Offer(ExchangeSku s,String k,String n){sku=s;key=k;notice=n;}
 }
 public static final class Result {
  public final String state,reason,order;public final boolean terminal;
  Result(String s,String r,String o,boolean t){state=s;reason=r;order=o;terminal=t;}
  public String text(){return state+"\n"+reason+(order.isEmpty()?"":"\n订单号："+order);}
 }
 static JSONObject data(JSONObject root)throws JSONException {JSONObject d=root.optJSONObject("data");if(d==null)throw new JSONException("官方响应数据缺失");return d;}
 static String statusReason(String status){
  switch(status){case "point_not_enough":return "积分不足";case "sold_out":return "商品已售罄";case "exchange_limit":return "已达到兑换上限";case "not_logging":return "登录已失效";case "user_limit_vip":return "仅限大会员兑换";case "user_limit_annual_vip":return "仅限年度大会员兑换";case "user_limit_tvvip":return "仅限超级大会员兑换";case "vip_ban_limit":return "账号状态异常";case "system_rate_limit":return "官方限流，请稍后再试";case "pending_sale":return "商品尚未开售";default:return "官方未确认商品可兑换（"+(status.isEmpty()?"状态缺失":status)+"）";}
 }
 static void verify(BiliApi a,ExchangeSku sku)throws Exception{
  JSONObject v=data(a.verifyExchange(sku.token,sku.price));String reject=v.optString("reject_reason","");
  if(!reject.isEmpty())throw new Exception(reject);
  if(!Boolean.TRUE.equals(v.opt("can_purchase")))throw new Exception("官方未通过兑换资格核验");
 }
 public static Offer prepare(BiliApi a,String token)throws Exception{
  JSONObject p=data(a.profile());if(!p.optBoolean("isLogin")||p.optString("mid").isEmpty())throw new Exception("无法确认当前兑换账号，请重新登录");
  JSONObject detail=data(a.exchangeInfo(token));ExchangeSku sku=new ExchangeSku(detail);
  if(!sku.valid()||!sku.token.equals(token)||!sku.membershipCard())throw new Exception("商品信息变化，请刷新后重试");
  if(sku.price.isEmpty())throw new Exception("官方兑换积分缺失，未发起扣分");
  JSONObject button=detail.optJSONObject("purchase_button");String status=button==null?"":button.optString("status");
  if(!status.equals("available"))throw new Exception(status.equals("sold_out")?sku.saleInfo(System.currentTimeMillis()):statusReason(status));
  int delivery=detail.optInt("delivery_type",-1);if(delivery!=1&&delivery!=2)throw new Exception("此商品不属于当前支持的虚拟大会员权益，未发起兑换");
  verify(a,sku);StringBuilder notice=new StringBuilder();JSONArray rules=detail.optJSONArray("exchange_notice");
  if(rules!=null)for(int i=0;i<rules.length();i++){JSONObject rule=rules.optJSONObject(i);if(rule!=null&&"text".equals(rule.optString("type"))&&notice.length()<1800)notice.append(rule.optString("content")).append('\n');}
  return new Offer(sku,p.optString("mid")+":"+token,notice.toString());
 }
 public static Result fromDetail(JSONObject root,String expected)throws JSONException{
  JSONObject d=data(root),o=d.optJSONObject("order");
  if(o==null||!expected.equals(o.optString("order_no")))return waiting(expected,"官方订单信息缺失或订单号不匹配，尚未确认兑换结果");
  int state=o.optInt("state",-1);JSONObject good=d.optJSONObject("good"),type=good==null?null:good.optJSONObject("good");int delivery=type==null?-1:type.optInt("delivery_type",-1);
  if(state==4||(state==3&&delivery==4))return new Result("兑换成功",delivery==1?"官方订单已成功，权益通常在1～10分钟内到账；请以会员状态为准":delivery==2?"官方订单已成功，兑换码可在B站已兑换商品中查看":"官方订单已确认成功",expected,true);
  if(state==5){String why=d.optString("fail_reason","");if(why.isEmpty())why="官方订单处理失败，未提供详细原因";return new Result("兑换失败",why+(d.optBoolean("refund_point")?"；积分已退回":""),expected,true);}
  return waiting(expected,"官方订单仍在处理中（状态 "+state+"），不能据此认定兑换成功");
 }
 static Result waiting(String order,String reason){return new Result("兑换待确认",reason,order,false);}
 public static Result redeem(BiliApi a,Offer offer,Store store)throws Exception{return redeem(a,offer,store,()->{});}
 public static Result redeem(BiliApi a,Offer offer,Store store,Runnable gate)throws Exception{
  gate.run();
  String pending=store.get(offer.key);if(!pending.isEmpty())return resume(a,offer.key,pending,store);
  verify(a,offer.sku);gate.run();
  if(Thread.currentThread().isInterrupted())throw new InterruptedException();
  if(!store.put(offer.key,"creating"))throw new Exception("无法保存兑换核验记录，未发起扣分");
  String order="";
  try{
   JSONObject d=data(a.createExchange(offer.sku.token,offer.sku.price));String reject=d.optString("reject_reason","");
   if(!reject.isEmpty()){store.remove(offer.key);return new Result("兑换失败",reject,"",true);}
   JSONObject o=d.optJSONObject("order");order=o==null?"":o.optString("order_no","");
   if(!order.matches("[A-Za-z0-9_-]{1,128}"))return waiting("","提交已返回但订单号缺失，请先在B站核对已兑换商品；不会自动重复扣分");
   if(!store.put(offer.key,order))return waiting(order,"订单已创建但本地记录保存失败，请先在B站核对；不会自动重试");
  }catch(BiliApi.ApiError e){if(e.confirmedRejection){store.remove(offer.key);return new Result("兑换失败",e.getMessage(),"",true);}return waiting("",e.getMessage()+"；提交结果不明确，请先核对官方订单，不会自动重发");}
   catch(Exception e){return waiting(order,"兑换提交结果不明确："+safeError(e)+"。请先核对官方订单，不会自动重复扣分");}
  return resume(a,offer.key,order,store);
 }
 public static Result resume(BiliApi a,String key,String order,Store store){
  if(order.equals("creating"))return waiting("","上次提交未取得明确订单号，请先在B站已兑换商品核对；为避免重复扣分，本助手不会重发");
  Result last=waiting(order,"官方订单尚未确认");
  for(int i=0;i<6;i++){
   try{last=fromDetail(a.exchangeOrder(order),order);if(last.terminal){store.remove(key);return last;}if(i<5)Thread.sleep(1000);}
   catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();return waiting(order,"无法核验官方订单："+safeError(e)+"；可点击核验上次兑换，不会重新扣分");}
  }return last;
 }
 static String safeError(Exception e){if(e instanceof BiliApi.ApiError)return e.getMessage();if(e instanceof java.io.IOException)return "网络异常或超时";if(e instanceof JSONException)return "官方响应格式变化";if(e instanceof InterruptedException)return "核验已停止";return "订单核验异常";}
}
