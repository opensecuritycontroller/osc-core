package org.osc.core.broker.service.request;

import org.osc.core.rest.client.crypto.model.CertificateResolverModel;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class SslCertificatesExtendedException extends ErrorTypeException {

    private ArrayList<CertificateResolverModel> certificateResolverModels;

    public ArrayList<CertificateResolverModel> getCertificateResolverModels() {
        return this.certificateResolverModels;
    }

    public SslCertificatesExtendedException(ErrorTypeException errorTypeException, ArrayList<CertificateResolverModel> certificateResolverModels) {
        super(errorTypeException.getCause(), errorTypeException.getType());
        this.certificateResolverModels = certificateResolverModels;
    }
}