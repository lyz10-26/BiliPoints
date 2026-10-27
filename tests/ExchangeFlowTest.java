import cn.pi.bilipoints.*;import org.json.*;import java.util.*;import java.io.*;
public class ExchangeFlowTest {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 static JSONObject j(String s)throws Exception{return new JSONObject(s);}
 static class Memory implements ExchangeFlow.Store {Map<String,String> map=new HashMap<>();public String get(String k){return map.containsKey(k)?map.get(k):"";}public boolean put(String k,String o){map.put(k,o);return true;}public void remove(String k){map.remove(k);}}
 static class Api extends BiliApi {
  int creates=0;boolean lost=false,reject=false,stop=false;String state="4",can="true",button="available";
  Api(){super("SESSDATA=test; bili_jct=csrf");}
  public JSONObject profile()throws Exception{return j("{\"data\":{\"isLogin\":true,\"mid\":123}}");}
  public JSONObject exchangeInfo(String token)throws Exception{return j("{\"data\":{\"token\":\"vip1\",\"title\":\"大会员月卡\",\"delivery_type\":1,\"price\":{\"origin\":3000,\"promotion\":{\"price\":2500}},\"purchase_button\":{\"status\":\""+button+"\"}}}");}
  public JSONObject verifyExchange(String t,String p)throws Exception{return j("{\"data\":{\"can_purchase\":"+can+"}}");}
  public JSONObject createExchange(String t,String p)throws Exception{creates++;if(lost)throw new IOException("timeout");return reject?j("{\"data\":{\"reject_reason\":\"库存已兑完\"}}"):j("{\"data\":{\"order\":{\"order_no\":\"ORDER1\"}}}");}
  public JSONObject exchangeOrder(String order)throws Exception{return detail(state,1,order);}
 }
 static class Protocol extends BiliApi {
  String path,body;Protocol(){super("SESSDATA=test; bili_jct=a+b");}
  protected JSONObject call(String p,String b,boolean json){path=p;body=b;return new JSONObject();}
 }
 static JSONObject detail(String state,int type,String order)throws Exception{return j("{\"code\":0,\"data\":{\"order\":{\"order_no\":\""+order+"\",\"state\":"+state+"},\"good\":{\"good\":{\"delivery_type\":"+type+"}},\"fail_reason\":\"发放失败\"}}");}
 public static void main(String[] args)throws Exception{
  Api a=new Api();Memory store=new Memory();ExchangeFlow.Offer offer=ExchangeFlow.prepare(a,"vip1");ok(offer.sku.price.equals("2500"),"fresh price shown before consent");ok(a.creates==0,"preparation cannot spend");
  ExchangeFlow.Result success=ExchangeFlow.redeem(a,offer,store);ok(success.state.equals("兑换成功")&&success.terminal&&a.creates==1&&store.get(offer.key).isEmpty(),"only terminal official success completes");
  ok(!ExchangeFlow.fromDetail(detail("3",1,"O"),"O").terminal,"state3 virtual delivery is still processing");ok(ExchangeFlow.fromDetail(detail("3",4,"O"),"O").terminal,"official physical packing rule");
  ok(ExchangeFlow.fromDetail(detail("5",1,"O"),"O").reason.contains("发放失败"),"official fail reason");ok(!ExchangeFlow.fromDetail(detail("4",1,"OTHER"),"O").terminal,"wrong order must not report success");ok(!ExchangeFlow.fromDetail(j("{\"data\":{}}"),"O").terminal,"missing state remains pending");
  a=new Api();offer=ExchangeFlow.prepare(a,"vip1");a.lost=true;store=new Memory();ok(!ExchangeFlow.redeem(a,offer,store).terminal,"lost create response unknown");ExchangeFlow.redeem(a,offer,store);ok(a.creates==1,"unknown submit is never repeated");
  a=new Api();offer=ExchangeFlow.prepare(a,"vip1");a.reject=true;store=new Memory();ok(ExchangeFlow.redeem(a,offer,store).reason.equals("库存已兑完"),"business rejection preserved");ok(store.get(offer.key).isEmpty(),"rejected create has no pending order");
  a=new Api();a.can="false";boolean denied=false;try{ExchangeFlow.prepare(a,"vip1");}catch(Exception e){denied=true;}ok(denied&&a.creates==0,"qualification denial cannot spend");
  a=new Api();offer=ExchangeFlow.prepare(a,"vip1");boolean canceled=false;try{ExchangeFlow.redeem(a,offer,new Memory(),()->{throw new java.util.concurrent.CancellationException();});}catch(java.util.concurrent.CancellationException e){canceled=true;}ok(canceled&&a.creates==0,"cancel before submission cannot spend");
  Protocol protocol=new Protocol();protocol.verifyExchange("token_1","2500");ok(protocol.path.startsWith("/x/vip_point/trade/order/verify?")&&protocol.body.contains("token=token_1")&&protocol.body.contains("price=2500")&&protocol.body.contains("csrf=a%2Bb"),"official verify form with CSRF");protocol.createExchange("token_1","2500");ok(protocol.path.contains("/trade/order/create?"),"one explicit creation endpoint");protocol.exchangeOrder("O+1");ok(protocol.path.startsWith("/x/vip_point/trade/order/detail?order_no=O%2B1")&&protocol.body==null,"query is read-only and encodes order number");
  System.out.println("PASS: preflight without spending, actual order states, rejection, unknown submit deduplication and cancellation");
 }
}
