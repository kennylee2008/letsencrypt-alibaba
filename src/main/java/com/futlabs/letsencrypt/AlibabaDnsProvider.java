package com.futlabs.letsencrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.AddDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.AddDomainRecordResponse;
import com.aliyuncs.alidns.model.v20150109.DeleteDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.DeleteDomainRecordResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;

@Component("alibaba")
@Scope( ConfigurableBeanFactory.SCOPE_PROTOTYPE )
public class AlibabaDnsProvider implements DnsProvider {
	private static final Logger LOG = LoggerFactory.getLogger(AlibabaDnsProvider.class);
	private String recordId;
	private String accessKey;
	private String accessSecret;
	
	@Override
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	@Override
	public void setAccessSecret(String accessSecret) {
		this.accessSecret = accessSecret;
	}

	@Override
	public void addTxtValueToDomain(String domainName, String txtValue, String rR) {
		IClientProfile profile =  DefaultProfile.getProfile(
	            "cn-hangzhou",                     //Region ID
	            this.accessKey,             // AccessKey ID
	            this.accessSecret);        // AccessKey Secret
		
		AddDomainRecordRequest request = new AddDomainRecordRequest();
		//DescribeSubDomainRecordsRequest request = new DescribeSubDomainRecordsRequest();
		request.setActionName("AddDomainRecord");
		request.setDomainName(domainName);
		request.setRR(rR);
		request.setType("TXT");
		request.setValue(txtValue);
		
		IAcsClient client = new DefaultAcsClient(profile);
		try {
			AddDomainRecordResponse response = client.getAcsResponse(request);
			this.recordId = response.getRecordId();
			LOG.info("Set TXT value to " + rR + "." + domainName + " has been completed.");
		} catch (Exception e) {
			throw new RuntimeException("Failed to set TXT value to " + rR + "." + domainName,e);
		}
	}

	@Override
	public void removeTxtValueFromDomain(String domainName, String txtValue, String rR) {
		IClientProfile profile =  DefaultProfile.getProfile(
	            "cn-hangzhou",                     //Region ID
	            this.accessKey,             // AccessKey ID
	            this.accessSecret);        // AccessKey Secret
		
		DeleteDomainRecordRequest request = new DeleteDomainRecordRequest();
		request.setActionName("DeleteDomainRecord");
		request.setRecordId(recordId);
		
		IAcsClient client = new DefaultAcsClient(profile);
		try {
			DeleteDomainRecordResponse response = client.getAcsResponse(request);
			LOG.info("Remove TXT value from " + rR + "." + domainName + " has been completed.");
		} catch (Exception e) {
			e.printStackTrace();
			LOG.warn("Failed to remove TXT value from " + rR + "." + domainName + ", Please remove it manually!",e);
		}
	}

}
