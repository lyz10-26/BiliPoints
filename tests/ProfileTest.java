import cn.pi.bilipoints.*;import org.json.*;
public class ProfileTest {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  ProfileInfo normal=ProfileInfo.parse(new JSONObject("{\"face\":\"https://i0.hdslb.com/a.jpg\",\"money\":123.5,\"level_info\":{\"current_level\":5,\"current_min\":4500,\"current_exp\":7650,\"next_exp\":10800}}"));
  ok(normal.coins.equals("123.5")&&normal.level==5&&normal.expPercent==50,"coin precision and progress within current level");
  ProfileInfo max=ProfileInfo.parse(new JSONObject("{\"money\":0,\"level_info\":{\"current_level\":6,\"current_exp\":28800,\"next_exp\":\"--\"}}"));ok(max.coins.equals("0")&&max.expPercent==100&&max.experience.contains("最高等级"),"Lv6 string next_exp and zero coins");
  ProfileInfo missing=ProfileInfo.parse(new JSONObject("{\"wallet\":{\"bcoin_balance\":99}}"));ok(missing.coins.equals("—")&&missing.level==-1,"B币 must never be displayed as coins and missing level must stay unknown");
  VipInfo vip=VipInfo.parse(new JSONObject("{\"vipStatus\":1,\"vipType\":2,\"vip_label\":{\"text\":\"年度大会员\"},\"vip\":{\"label\":{\"text\":\"年度超大会员\",\"img_label_uri_hans_static\":\"https://i0.hdslb.com/badge.png\"}}}"),1800000000000L);
  ok(vip.state.equals("年度超大会员")&&vip.icon.equals("https://i0.hdslb.com/badge.png"),"explicit current super membership and original badge asset");
  VipInfo ordinary=VipInfo.parse(new JSONObject("{\"vipStatus\":1,\"vipType\":2,\"vip_label\":{\"text\":\"年度大会员\"}}"),1800000000000L);ok(ordinary.state.equals("年度大会员"),"ordinary annual member cannot be promoted to super by the UI");
  System.out.println("PASS: coin accuracy, normal/max/missing experience, explicit super membership and original badge URL");
 }
}
