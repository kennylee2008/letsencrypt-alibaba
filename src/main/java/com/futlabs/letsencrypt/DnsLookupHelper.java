package com.futlabs.letsencrypt;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

public class DnsLookupHelper {
	
	public static boolean isValid(String domain,String digest) {
		
		try {
			SimpleResolver res = new SimpleResolver();
			res.setTCP(true);
			Lookup l = new Lookup("_acme-challenge."+domain, Type.TXT, DClass.IN);
			
			l.run();
			if (l.getResult() == Lookup.SUCCESSFUL) {
			    String r = l.getAnswers()[0].rdataToString();
			    if(r != null) {
			    	if(r.contains("\"")) {
			    		r = r.replaceAll("\"", "");
			    	}
			    }
			    if(digest.equals(r)) {
			    	return true;
			    }
			}
		} catch (Exception ignore) {
		}
		return false;
	}
}
