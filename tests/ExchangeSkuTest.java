import cn.pi.bilipoints.*;import org.json.*;import java.util.*;
public class ExchangeSkuTest {
 static void ok(boolean b,String message){if(!b)throw new AssertionError(message);}
 static ExchangeSku sku(String extra)throws Exception{return new ExchangeSku(new JSONObject("{\"token\":\"vip-card_1\",\"title\":\"大会员月卡\","+extra+"}"));}
 static class Capture extends BiliApi {String path,body;Capture(){super("");}protected JSONObject call(String p,String b,boolean j){path=p;body=b;return new JSONObject();}}
 public static void main(String[] args)throws Exception{
  ExchangeSku sale=sku("\"price\":{\"origin\":3000,\"promotion\":{\"price\":2500,\"label\":\"限时优惠\"}},\"inventory\":{\"is_sold_out\":false,\"status\":1},\"user_type\":3");
  ok(sale.price.equals("2500")&&sale.origin.equals("3000")&&sale.restriction.equals("年度大会员专享"),"official promotion and membership fields");
  ok(sale.url().equals("https://big.bilibili.com/mobile/bigPoint/sku/vip-card_1"),"official token detail route");
  ok(sku("\"inventory\":{\"is_sold_out\":true}").stock.equals("已售罄"),"total sold out");
  ok(sku("\"inventory\":{\"status\":2}").stock.equals("暂兑完"),"temporary sold out");
  ok(sku("\"inventory\":{\"next_sold_text\":\"明日开放\"}").stock.equals("明日开放"),"official next sale label");
  ExchangeSku missing=sku("\"price\":{}");ok(missing.price.isEmpty()&&missing.stock.contains("官方详情"),"unknown price/stock is not free or in stock");
  ok(sku("\"price\":{\"origin\":3000,\"promotion\":{\"price\":-1}}").price.equals("3000"),"invalid promotion falls back to origin");
  ok(sku("\"price\":{\"origin\":0}").price.equals("0"),"explicit zero is preserved");
  JSONObject data=new JSONObject("{\"skus\":[{\"token\":\"one\",\"title\":\"7天大会员\"},{\"token\":\"one\",\"title\":\"7天大会员\"},{\"token\":\"two\",\"title\":\"大会员优惠券\"},{\"token\":\"../oops\",\"title\":\"大会员年卡\"},{\"token\":\"three\",\"title\":\"漫画券\"}]}");
  ok(ExchangeSku.parse(data).size()==1,"deduplication, coupon exclusion, route validation");
  long epoch=1800000000L;
  ExchangeSku timed=sku("\"inventory\":{\"status\":2,\"next_sold_time\":"+epoch+"}");
  ok(timed.nextSoldAt==epoch*1000L&&timed.saleInfo(0).contains("01-15 16:00:00")&&timed.saleInfo(0).contains("北京时间"),"official seconds converted to Beijing sale time");
  ExchangeSku millis=sku("\"inventory\":{\"status\":2,\"next_sold_time\":"+(epoch*1000L)+"}");ok(millis.nextSoldAt==timed.nextSoldAt,"millisecond timestamp supported");
  ok(timed.saleInfo(epoch*1000L+1).contains("请刷新库存"),"past schedule is not an available-stock claim");
  ok(sku("\"inventory\":{\"status\":2,\"next_sold_text\":\"每日18点开售\"}").saleInfo(0).contains("每日18点开售"),"official sale wording retained without timestamp");
  ok(sku("\"inventory\":{\"status\":2}").saleInfo(0).contains("暂未获取"),"unknown timing is explicit");
  ExchangeSku merged=sale.withInventory(new JSONObject("{\"inventory\":{\"status\":2,\"next_sold_time\":1800000000}}"));ok(merged.title.equals(sale.title)&&merged.price.equals(sale.price)&&merged.temporarySoldOut,"detail inventory enrichment preserves listing identity and price");
  boolean rejected=false;try{ExchangeSku.parse(new JSONObject());}catch(JSONException e){rejected=true;}ok(rejected,"missing schema must not mean an empty shop");
  Capture a=new Capture();a.exchangeSkus(2);ok(a.path.startsWith("/x/vip_point/sku/list?pn=2&ps=20&in_review=0")&&a.body==null,"read-only official listing protocol");
  System.out.println("PASS: official SKU fields, promotions, inventory states, missing data, safe routes and read-only request");
 }
}
