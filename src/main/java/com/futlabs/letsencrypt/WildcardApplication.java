package com.futlabs.letsencrypt;

import java.io.File;
import java.io.FileWriter;
import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class WildcardApplication implements ApplicationRunner{
	
	@Autowired
	private ApplicationContext context;

	// File name of the Domain Key Pair
	private static final String DOMAIN_KEY_FILE_NAME = "domain.key";

	// File name of the signed certificate
	private static final String DOMAIN_CHAIN_FILE_NAME = "domain-chain.crt";

	// RSA key size of generated key pairs
	private static final int KEY_SIZE = 2048;

	private enum STAGE {
		TEST, PROD
	};
	
	private static final Logger LOG = LoggerFactory.getLogger(WildcardApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(WildcardApplication.class, args);
	}

	/**
	 * --domain The top level domain name, etc: example.com
	 * --dnsProvider alibaba
	 * --accessKey DNS Provider API accessKey
	 * --accessSecret DNS Provider API accessSecret
	 * --stage TEST or PROD
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		STAGE stage = STAGE.TEST;
		if(!args.containsOption("domain") ) {
			LOG.error("The Option --domain=<domain> is a must!");
			System.exit(0);
		}
		if(!args.containsOption("dnsProvider") ) {
			LOG.error("The Option --dnsProvider=<dnsProvider> is a must!");
			System.exit(0);
		}
		if(!args.containsOption("accessKey") ||  !args.containsOption("accessSecret")) {
			LOG.error("The Options --accessKey=<accessKey> and --accessSecret=<accessSecret> are must!");
			System.exit(0);
		}
		if(!args.containsOption("stage")) {
			LOG.warn("The Option --stage=<TEST|PROD> is not set, will use the default stage: TEST!");
		}else {
			if("PROD".equals(args.getOptionValues("stage").get(0))) {
				stage = STAGE.PROD;
			}
		}
		
		String domain = args.getOptionValues("domain").get(0);
		String dnsProviderName = args.getOptionValues("dnsProvider").get(0);
		String accessKey = args.getOptionValues("accessKey").get(0);
		String accessSecret = args.getOptionValues("accessSecret").get(0);
		
		//example.com, *.example.com
		String[] domains = new String[] {domain, "*."+domain};
		
		LOG.info("Starting up...");
		LOG.info("domain="+domain);
		LOG.info("dnsProvider="+dnsProviderName);
		LOG.info("stage="+stage);
		Security.addProvider(new BouncyCastleProvider());
		try {
			
			// Create a session for Let's Encrypt.
			// Use "acme://letsencrypt.org" for production server
			// Use "acme://letsencrypt.org/staging" for test server
			Session session = null;
			if(stage == STAGE.PROD) {
				session = new Session("acme://letsencrypt.org");
			}else {
				session = new Session("acme://letsencrypt.org/staging");
			}
			//Create user account key
			KeyPair userKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);

			//Create an account
			Account account = new AccountBuilder().agreeToTermsOfService().useKeyPair(userKeyPair).create(session);
			
			//Create domain key
			KeyPair domainKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
			
			// Order the certificate
			Order order = account.newOrder().domains(domains).create();
			
			// Perform all required authorizations
			for (Authorization auth : order.getAuthorizations()) {
				//authorize(auth,accessKey,accessSecret);
				Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.TYPE);
				
				if (challenge == null) {
					throw new AcmeException("Found no " + Dns01Challenge.TYPE + " challenge, don't know what to do...");
				}
				
				if (auth.getStatus() == Status.VALID ||
						challenge.getStatus() == Status.VALID) {
					continue;
				}
				
				LOG.info("Challenge for domain " + auth.getIdentifier().getDomain() + " ...");
				
				DnsProvider dnsProvider = (DnsProvider)context.getBean(dnsProviderName);
				if(dnsProvider == null) {
					throw new RuntimeException("No DnsProvider["+dnsProviderName+"] found");
				}
				
				dnsProvider.setAccessKey(accessKey);
				dnsProvider.setAccessSecret(accessSecret);
				
				dnsProvider.addTxtValueToDomain(
						auth.getIdentifier().getDomain(), 
						challenge.getDigest(), 
						"_acme-challenge");

				try {
					int attempts = 50;
					while(!DnsLookupHelper.isValid(auth.getIdentifier().getDomain(), challenge.getDigest())
							&& attempts-- > 0) {

						LOG.info("Your DNS settings are not yet in effect, wait an additional 30 seconds...");
						Thread.sleep(30000L);
					}
				} catch (InterruptedException e) {
					LOG.error("interrupted", e);
					Thread.currentThread().interrupt();
				}
				
				// Now trigger the challenge.
				challenge.trigger();

				// Poll for the challenge to complete.
				try {
					int attempts = 50;
					while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
						// Did the authorization fail?
						if (challenge.getStatus() == Status.INVALID) {
							LOG.error("Challenge has failed, reason: {}", challenge.getError());
							throw new AcmeException("Challenge failed... Giving up.");
						}

						// Wait for a few seconds
						Thread.sleep(3000L);

						// Then update the status
						challenge.update();
					}
				} catch (InterruptedException ex) {
					LOG.error("interrupted", ex);
					Thread.currentThread().interrupt();
				}
				
				// All reattempts are used up and there is still no valid authorization?
				if (challenge.getStatus() != Status.VALID) {
					throw new AcmeException(
							"Failed to pass the challenge for domain " + auth.getIdentifier().getDomain() + ", ... Giving up.");
				}

				LOG.info("Challenge for domain " + auth.getIdentifier().getDomain() + " has been completed.");
				
				dnsProvider.removeTxtValueFromDomain(
						auth.getIdentifier().getDomain(), 
						challenge.getDigest(), 
						"_acme-challenge");
				
			}
			
			//Generate a CSR for the domains
			CSRBuilder csrb = new CSRBuilder();
			csrb.addDomains(domains);
			csrb.sign(domainKeyPair);
			
			order.execute(csrb.getEncoded());
			
			// Wait for the order to complete
			try {
				int attempts = 50;
				while (order.getStatus() != Status.VALID && attempts-- > 0) {
					// Did the order fail?
					if (order.getStatus() == Status.INVALID) {
						LOG.error("Order has failed, reason: {}", order.getError());
						throw new AcmeException("Order failed... Giving up.");
					}

					// Wait for a few seconds
					Thread.sleep(3000L);

					// Then update the status
					order.update();
				}
			} catch (InterruptedException ex) {
				LOG.error("interrupted", ex);
				Thread.currentThread().interrupt();
			}
			
			// Get the certificate
			Certificate certificate = order.getCertificate();

			LOG.info("Success! The certificate for domains {} has been generated!", domains);
			LOG.info("Certificate URL: {}", certificate.getLocation());
			
			File dir = new File("certificate/"+domain + "/");
			
			// Write a combined file containing the certificate and chain.
			if(!dir.exists()) {
				dir.mkdirs();
			}
			File domainChainFile = new File("certificate/"+domain + "/" + DOMAIN_CHAIN_FILE_NAME);
			
			try (FileWriter fw = new FileWriter(domainChainFile)) {
				certificate.writeCertificate(fw);
			}
			
			// Write the domain key to a file
			File domainKeyFile = new File("certificate/"+domain + "/" + DOMAIN_KEY_FILE_NAME);
			try (FileWriter fw = new FileWriter(domainKeyFile)) {
				KeyPairUtils.writeKeyPair(domainKeyPair, fw);
			}
			
		} catch (Exception ex) {
			LOG.error("Failed to get a certificate for domain " + domain, ex);
		}
	}

}
