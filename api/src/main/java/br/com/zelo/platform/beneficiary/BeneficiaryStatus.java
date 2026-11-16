package br.com.zelo.platform.beneficiary;

public enum BeneficiaryStatus {
	ACTIVE,
	INACTIVE,
	// Personal data removed on request (LGPD); only the id remains, linked to history.
	ANONYMIZED
}
