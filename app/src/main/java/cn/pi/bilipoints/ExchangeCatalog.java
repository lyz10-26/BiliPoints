package cn.pi.bilipoints;
import org.json.*;
import java.util.*;
/** A fresh snapshot from the official catalog; never synthesizes card variants. */
public final class ExchangeCatalog {
 public final ArrayList<ExchangeSku> items;public final int detailFailures;
 ExchangeCatalog(ArrayList<ExchangeSku> list,int failures){items=list;detailFailures=failures;}
 public static ExchangeCatalog load(BiliApi api,Runnable gate)throws Exception{
  ArrayList<ExchangeSku> items=new ArrayList<>();HashSet<String> seen=new HashSet<>(),pages=new HashSet<>();int count=0;
  for(int page=1;;page++){
   gate.run();if(page>50)throw new JSONException("官方商品分页超出范围，请稍后刷新");
   JSONObject d=ExchangeFlow.data(api.exchangeSkus(page));JSONArray list=d.optJSONArray("skus");
   if(list==null)throw new JSONException("官方兑换商品列表格式变化");
   if(list.length()>0&&!pages.add(list.toString()))throw new JSONException("官方重复返回同一页商品，请稍后刷新");
   for(ExchangeSku sku:ExchangeSku.parse(d))if(seen.add(sku.token))items.add(sku);
   count+=list.length();JSONObject pagination=d.optJSONObject("page");int total=pagination==null?-1:pagination.optInt("total",-1);
   if(total>=0&&count>=total)break;
   if(list.length()==0){if(total>count)throw new JSONException("官方商品列表不完整，请刷新");break;}
   if(total<0&&list.length()<20)break;
  }
  int failures=0;
  for(int i=0;i<items.size();i++){
   gate.run();ExchangeSku sku=items.get(i);
   try{items.set(i,sku.withDetail(ExchangeFlow.data(api.exchangeInfo(sku.token))));}
   catch(Exception e){if(e instanceof java.util.concurrent.CancellationException||e instanceof InterruptedException||(e instanceof BiliApi.ApiError&&((BiliApi.ApiError)e).code==-101))throw e;failures++;}
  }
  gate.run();return new ExchangeCatalog(items,failures);
 }
}
