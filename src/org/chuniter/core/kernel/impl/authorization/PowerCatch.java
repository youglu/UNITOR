package org.chuniter.core.kernel.impl.authorization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerCatch {
	
	protected static Map<String,List<String[]>> powers = new HashMap<String,List<String[]>>();	
	public static  Map<String,List<String[]>> getCatchedPower(){
		return powers;
	}
	public static void catchPower(String pid,String[] pinfos){
		List<String[]> tlis = powers.get(pid);
		if(null == tlis){
			tlis = new ArrayList<String[]>();
			tlis.add(pinfos);
			powers.put(pid, tlis);
			return;
		}
		for(String[] tp:tlis){
			if(equalsArray(tp,pinfos)){
				return;
			}
		}		
		tlis.add(pinfos);		
		powers.put(pid, tlis);
	}
	private static boolean equalsArray(String[] eqa,String[] eqb){
		if(eqa.length==eqb.length){				
			for(int i=0;i<eqa.length;i++){
				if(null != eqa[i]&&!eqa[i].equals(eqb[i])){					
					return false;
				}
			}
			return true;
		}else
			return false;
	}
	public static void catchPower(String pid,String pname,String category,String moduleName){
		catchPower(pid, new String[]{pname,category,moduleName});
	}	
	public static void catchPower(String pid,String pname,String category,String moduleName,String iconUrl){
		catchPower(pid, new String[]{pname,category,moduleName,iconUrl});
	}
	public static void removePowerFormCatch(String pid){
		powers.remove(pid);
	}	
}
