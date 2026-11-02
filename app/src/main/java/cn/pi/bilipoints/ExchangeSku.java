package cn.pi.bilipoints;
import org.json.*;
import java.math.BigDecimal;
import java.util.*;
/** Read-only product projection of the official bigPoint sku-card fields. */
public final class ExchangeSku {
 public static final String MALL="https://big.bilibili.com/mobile/bigPoint?jump=point";
 public final String token,title,picture,price,origin,promotion,stock,restriction,scheduleNotice;
 private final JSONObject raw;
 public final boolean soldOut,temporarySoldOut;public final long nextSoldAt;public final String nextSoldText;
 public ExchangeSku(JSONObject j){
  raw=j;
  token=j.optString("token","");title=j.optString("title","");picture=j.optString("picture","");
  JSONObject p=j.optJSONObject("price"),offer=p==null?null:p.optJSONObject("promotion"),inv=j.optJSONObject("inventory");
  origin=number(p==null?null:p.opt("origin"));String sale=number(offer==null?null:offer.opt("price"));
  String current=number(p==null?null:p.opt("sale"));price=!current.isEmpty()?current:sale.isEmpty()?origin:sale;promotion=offer==null?"":offer.optString("label","");
  soldOut=inv!=null&&inv.optBoolean("is_sold_out");temporarySoldOut=inv!=null&&inv.optInt("status")==2;
  nextSoldText=inv==null?"":inv.optString("next_sold_text","");long timestamp=inv==null?0:inv.optLong("next_sold_time",0);
  nextSoldAt=timestamp<=0?0:timestamp<100000000000L?timestamp*1000:timestamp;
  scheduleNotice=schedule(j.optJSONArray("exchange_notice"));
  stock=soldOut?"已售罄":temporarySoldOut?"暂兑完":inv==null?"库存以官方详情为准":inv.optString("next_sold_text","");
  int type=j.optInt("user_type");restriction=type==2?"大会员专享":type==3?"年度大会员专享":type==4?"超级大会员专享":"";
 }
 static String number(Object v){try{if(v==null||v==JSONObject.NULL)return "";BigDecimal n=new BigDecimal(v.toString());return n.signum()<0?"":n.stripTrailingZeros().toPlainString();}catch(Exception e){return "";}}
 public ExchangeSku withInventory(JSONObject detail)throws JSONException{JSONObject inv=detail.optJSONObject("inventory");if(inv==null)return this;JSONObject merged=new JSONObject(raw.toString());merged.put("inventory",inv);return new ExchangeSku(merged);}
 public ExchangeSku withDetail(JSONObject detail)throws JSONException{
  if(!token.equals(detail.optString("token")))throw new JSONException("官方商品详情标识不匹配");
  JSONObject merged=new JSONObject(raw.toString());Iterator<String> keys=detail.keys();
  while(keys.hasNext()){String key=keys.next();if(!detail.isNull(key))merged.put(key,detail.get(key));}
  ExchangeSku fresh=new ExchangeSku(merged);if(!fresh.valid()||!fresh.membershipCard())throw new JSONException("官方商品信息已变化");return fresh;
 }
 static String schedule(JSONArray notices){
  if(notices==null)return "";StringBuilder out=new StringBuilder();
  for(int i=0;i<notices.length();i++){JSONObject n=notices.optJSONObject(i);if(n==null||!"text".equals(n.optString("type")))continue;
   String content=n.optString("content","").trim();
   // Keep the official wording, including recurring inventory refresh times.
   if(content.matches("(?s).*(开售|开抢|补充|补货|刷新|[0-9一二三四五六七八九十]+[：:][0-9]+|[0-9一二三四五六七八九十]+点).*")){if(out.length()>0)out.append('\n');out.append(content);}
  }return out.toString();
 }
 public String saleInfo(long now){
  String state=soldOut?"已售罄":temporarySoldOut?"暂兑完":"以官方资格核验为准";String timing="";
  if(nextSoldAt>0){java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
   timing=f.format(new Date(nextSoldAt))+"（北京时间）开售"+(nextSoldAt<=now?" · 开售时间已到，请刷新库存":"");
  }
  if(!nextSoldText.isEmpty())timing+=(timing.isEmpty()?"":"\n")+nextSoldText;
  if(!scheduleNotice.isEmpty())timing+=(timing.isEmpty()?"":"\n")+"官方兑换须知（北京时间）："+scheduleNotice;
  if(timing.isEmpty())timing="兑换时间暂未获取，请以官方实时状态为准";
  return state+"\n"+timing;
 }
 public boolean membershipCard(){return title.contains("大会员")&&!title.matches(".*(优惠|折扣|立减|抵扣|满减|代金|装扮|周边).*");}
 public boolean valid(){return !title.isEmpty()&&token.matches("[A-Za-z0-9_-]{1,200}");}
 public String url(){return valid()?"https://big.bilibili.com/mobile/bigPoint/sku/"+token:MALL;}
 public static ArrayList<ExchangeSku> parse(JSONObject data)throws JSONException{
  JSONArray list=data.optJSONArray("skus");if(list==null)throw new JSONException("官方兑换商品列表格式变化");
  ArrayList<ExchangeSku> out=new ArrayList<>();HashSet<String> seen=new HashSet<>();
  for(int i=0;i<list.length();i++){JSONObject j=list.optJSONObject(i);if(j==null)continue;ExchangeSku s=new ExchangeSku(j);if(s.valid()&&s.membershipCard()&&seen.add(s.token))out.add(s);}return out;
 }
}
