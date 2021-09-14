# Get Wildcard certificate from Let's Encrypt

[Let's Encrypt](https://letsencrypt.org/getting-started/) can issue a certificate for your website’s domain. 

When you want to get an SSL certificate from Let's Encrypt, you have a number of tools to choose from. The most common tool is Certbot, but the other day I ran into some trouble when I wanted to generate a wildcard type certificate for my website.

If you wish to generate a wildcard type certificate, Let's Encrypt will ask you to configure a special subdomain under your domain name and set it to a TXT type value for Let's Encrypt to determine that you do have the rights to use the domain. For example, generate a certificate for the domain *.example.com so that you can use the same certificate and support many subdomains, which is very useful in some business scenarios.

I started out using Certbot to generate this and it was easy to use, but still required me to manually log into my domain service provider to add TXT type domain values for the Let's Encrypt server to initiate a challenge. The problem with this is that I still have to repeat the process of manually adding challenge values when the certificate is about to expire.

Obviously, I don't want to have to repeat this operation every three months. Therefore, I started looking for an automated solution for my domain service provider. But, unfortunately, I didn't search for the solution I wanted. So, there is this Repository.

The purpose of this Repository is to give those whose domain name service provider is Alibaba Cloud a convenient tool to generate wildcard type certificates. If you are familiar with Java, you can also extend it very easily to support your domain name service provider.

# RUN

## Run with docker

Refer to [DockerHub: kennylee2008/letsencrypt-alibaba](https://hub.docker.com/r/kennylee2008/letsencrypt-alibaba) .

## Run with docker-compose

Refer to [GitHub: kennylee2008/letsencrypt-alibaba-docker](https://github.com/kennylee2008/letsencrypt-alibaba-docker) .



