package cn.pi.bilipoints;
import org.json.*;import java.math.BigDecimal;
/** Account stats from nav; coins never fall back to the unrelated B-coin wallet. */
public final class ProfileInfo {
 public String face="",coins="—",bcoins="—",levelText="等级 —",experience="经验 —";public int expPercent=0;public int level=-1;
 public static ProfileInfo parse(JSONObject p){ProfileInfo v=new ProfileInfo();v.face=p.optString("face","");try{if(p.has("money")&&!p.isNull("money")){BigDecimal n=new BigDecimal(p.optString("money"));if(n.signum()>=0)v.coins=n.stripTrailingZeros().toPlainString();}}catch(Exception ignored){}
  JSONObject wallet=p.optJSONObject("wallet");if(wallet!=null){try{if(wallet.has("bcoin_balance")&&!wallet.isNull("bcoin_balance")){BigDecimal bc=new BigDecimal(wallet.optString("bcoin_balance"));if(bc.signum()>=0)v.bcoins=bc.stripTrailingZeros().toPlainString();}}catch(Exception ignored){}}
  JSONObject li=p.optJSONObject("level_info");if(li==null)return v;v.level=li.optInt("current_level",-1);if(v.level>=0)v.levelText="Lv."+v.level;
  long exp=li.optLong("current_exp",-1),min=li.optLong("current_min",-1);String next=li.optString("next_exp","");long target=-1;try{target=Long.parseLong(next);}catch(Exception ignored){}
  if(exp>=0){if(v.level>=6&&next.equals("--")){v.experience="经验 "+exp+" · 已达最高等级";v.expPercent=100;}else{v.experience="经验 "+exp+" / "+(target>0?String.valueOf(target):"—");if(target>min&&min>=0)v.expPercent=(int)Math.max(0,Math.min(100,Math.round((exp-min)*100.0/(target-min))));}}
  return v;
 }
}
