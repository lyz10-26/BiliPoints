package cn.pi.bilipoints;
import cn.pi.bilipoints.*;import org.json.*;
public class ExchangeCatalogTest {
 static void ok(boolean b,String message){if(!b)throw new AssertionError(message);}
 static JSONObject j(String value)throws Exception{return new JSONObject(value);}
 static class Api extends BiliApi {
  int cards=2,calls=0;boolean fail=false,logout=false;Api(){super("");}
  public JSONObject exchangeSkus(int page)throws Exception{calls++;return j("{\"data\":{\"skus\":[{\"token\":\""+page+"\",\"title\":\"大会员"+(page==1?"3":"7")+"天卡\",\"price\":{\"origin\":900,\"sale\":720}}],\"page\":{\"total\":"+cards+"}}}");}
  public JSONObject exchangeInfo(String token)throws Exception{if(logout)throw new ApiError(-101,"登录失效");if(fail)throw new java.io.IOException();return j("{\"data\":{\"token\":\""+token+"\",\"inventory\":{\"status\":2},\"exchange_notice\":[{\"type\":\"text\",\"content\":\"每天12:00刷新当日库存，每月限兑1次，次日12：00补充。\"}]}}");}
 }
 public static void main(String[] args)throws Exception{
  Api a=new Api();ExchangeCatalog c=ExchangeCatalog.load(a,()->{});ok(c.items.size()==2&&a.calls==2,"read all pages even when short but total says more");
  ok(c.items.get(0).title.contains("3天")&&c.items.get(1).title.contains("7天"),"both official variants are dynamic");
  ok(c.items.get(0).saleInfo(0).contains("12:00")&&c.items.get(1).saleInfo(0).contains("每月限兑1次"),"detail notices and times on every card");
  ok(c.items.get(0).price.equals("720"),"official sale price authoritative");
  a.cards=1;c=ExchangeCatalog.load(a,()->{});ok(c.items.size()==1&&c.items.get(0).title.contains("3天"),"removed seven day card never persists");
  a.fail=true;c=ExchangeCatalog.load(a,()->{});ok(c.detailFailures==1&&c.items.get(0).saleInfo(0).contains("暂未获取"),"detail failure explicit without invented schedule");
  a.logout=true;boolean denied=false;try{ExchangeCatalog.load(a,()->{});}catch(BiliApi.ApiError e){denied=true;}ok(denied,"login expiration is not hidden");
  boolean stopped=false;try{ExchangeCatalog.load(new Api(),()->{throw new java.util.concurrent.CancellationException();});}catch(java.util.concurrent.CancellationException e){stopped=true;}ok(stopped,"stop cancels catalog sync");
  if(args.length>0){JSONObject list=new JSONObject(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args[0])),"UTF-8"));JSONObject detail=new JSONObject(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args[1])),"UTF-8"));java.util.ArrayList<ExchangeSku> live=ExchangeSku.parse(list.getJSONObject("data"));ok(live.size()==1&&live.get(0).title.equals("大会员3天卡"),"current official list contains one actual card");ExchangeSku sku=live.get(0).withDetail(detail.getJSONObject("data"));ok(sku.price.equals("720")&&sku.saleInfo(0).contains("每天12:00")&&sku.saleInfo(0).contains("每月限兑换1次"),"live official detail price and recurring schedule");}
  System.out.println("PASS: dynamic 3/7-day catalog, pagination, recurring schedules, refresh/removal, errors and official response fixtures");
 }
}
