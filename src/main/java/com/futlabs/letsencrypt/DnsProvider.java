package com.futlabs.letsencrypt;

public interface DnsProvider {
	public void setAccessKey(String accessKey);
	public void setAccessSecret(String accessSecret);
	public void addTxtValueToDomain(String domainName, String txtValue, String rR);
	public void removeTxtValueFromDomain(String domainName, String txtValue, String rR);
}
