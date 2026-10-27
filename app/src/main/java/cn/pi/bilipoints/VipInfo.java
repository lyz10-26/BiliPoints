package cn.pi.bilipoints;
import org.json.*;import java.text.*;import java.util.*;
/** Parses only the current authenticated profile; missing fields remain unknown. */
public class VipInfo {
 public int type=-1,status=-1;public long due=0;public String label="",icon="";public boolean active;public String state="待同步",expiry="登录后查看会员有效期";
 public static VipInfo parse(JSONObject profile,long now){
  VipInfo v=new VipInfo();JSONObject nested=profile.optJSONObject("vip");
  v.type=profile.optInt("vipType",nested==null?-1:nested.optInt("type",-1));v.status=profile.optInt("vipStatus",nested==null?-1:nested.optInt("status",-1));
  v.due=profile.optLong("vipDueDate",nested==null?0:nested.optLong("due_date",0));if(v.due>0&&v.due<100000000000L)v.due*=1000;
  JSONObject l=profile.optJSONObject("vip_label"),nl=nested==null?null:nested.optJSONObject("label");
  if(l==null||(nl!=null&&(nl.optString("text").contains("超大会员")||nl.optString("text").contains("超级大会员"))))l=nl;
  if(l!=null){v.label=l.optString("text","");v.icon=l.optString("img_label_uri_hans_static","");if(v.icon.isEmpty())v.icon=l.optString("img_label_uri_hans","");}
  if(v.icon.isEmpty()&&nl!=null)v.icon=nl.optString("img_label_uri_hans_static","");
  if(v.type==2&&(v.label.equals("超大会员")||v.label.equals("超级大会员")))v.label="年度超大会员";
  v.active=v.status==1&&(v.due==0||v.due>now);
  if(v.status<0)v.state="会员状态未知";
  else if(v.active)v.state=v.label.isEmpty()?(v.type==1?"月度大会员":v.type==2?"年度及以上大会员":"大会员生效中"):v.label;
  else if(v.due>0&&v.due<=now)v.state="大会员已到期";else v.state="当前未开通大会员";
  if(v.due>0){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.CHINA);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));v.expiry=(v.active?"有效期至 ":"到期时间 ")+f.format(new Date(v.due))+"（北京时间）";
   if(v.active)v.expiry+="\n剩余约 "+Math.max(1,(v.due-now+86399999)/86400000)+" 天";
  }else v.expiry=v.status<0?"接口未返回会员信息":v.active?"接口未返回到期时间":"可在B站会员中心查看权益";
  return v;
 }
}
