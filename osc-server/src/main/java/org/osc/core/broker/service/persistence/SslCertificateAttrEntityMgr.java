package org.osc.core.broker.service.persistence;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.SslCertificateAttr;
import org.osc.core.broker.service.dto.SslCertificateAttrDto;
import org.osc.core.rest.client.crypto.X509TrustManagerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SslCertificateAttrEntityMgr extends EntityManager<SslCertificateAttr> {

    public static final String replaceTimestampPattern = "\\_([0-9]+)";
    private static final String genericAliasNamePattern = "^([a-zA-Z_]+)\\_([0-9]+)$";

    public SslCertificateAttrEntityMgr(Session session) {
        super(SslCertificateAttr.class, session);
    }

    public static SslCertificateAttr createEntity(SslCertificateAttrDto dto) throws Exception {
        SslCertificateAttr vc = new SslCertificateAttr();
        toEntity(vc, dto);
        return vc;
    }

    // transform from dto to entity
    public static void toEntity(SslCertificateAttr vc, SslCertificateAttrDto dto) {
        vc.setId(dto.getId());
        vc.setAlias(dto.getAlias());
        vc.setSha1(dto.getSha1());
    }

    // transform from entity to dto
    public static void fromEntity(SslCertificateAttr vc, SslCertificateAttrDto dto) {
        dto.setId(vc.getId());
        dto.setAlias(vc.getAlias());
        dto.setSha1(vc.getSha1());
    }

    public Set<SslCertificateAttr> create(Set<SslCertificateAttr> requestSslCertificateAttrs, Set<SslCertificateAttr> storedSslCertificateAttrs) {
        HashSet<SslCertificateAttr> populatedCerts = new HashSet<>();

        for (SslCertificateAttr sslRequestEntry : requestSslCertificateAttrs) {
            if (!storedSslCertificateAttrs.contains(sslRequestEntry)) {
                populatedCerts.add(create(sslRequestEntry));
            } else {
                populatedCerts.add(sslRequestEntry);
            }
        }

        populatedCerts.addAll(storedSslCertificateAttrs);

        return populatedCerts;
    }

    private void updateCertificates(Set<SslCertificateAttr> sslCertificateAttrSet) {
        if (sslCertificateAttrSet != null) {
            sslCertificateAttrSet.forEach(this::update);
        }
    }

    public Set<SslCertificateAttr> storeSSLEntries(Set<SslCertificateAttr> sslCertificateAttrSet, Long connectorObjId) throws Exception {

        int i = 1;
        for (SslCertificateAttr sslCertificateAttr : sslCertificateAttrSet) {
            String newAlias = sslCertificateAttr.getAlias().replaceFirst(replaceTimestampPattern, "_" + connectorObjId + "_" + i);
            X509TrustManagerFactory.getInstance().updateAlias(sslCertificateAttr.getAlias(), newAlias);
            sslCertificateAttr.setAlias(newAlias);
            i++;
        }

        updateCertificates(sslCertificateAttrSet);

        return sslCertificateAttrSet;
    }

    public Set<SslCertificateAttr> storeSSLEntries(Set<SslCertificateAttr> requestCertsSet, Long requestObjId,
                                                   Set<SslCertificateAttr> persistentSslCertificatesSet) throws Exception {
        if (requestCertsSet.containsAll(persistentSslCertificatesSet)) {
            return requestCertsSet;
        }

        X509TrustManagerFactory trustManagerFactory = X509TrustManagerFactory.getInstance();

        Set<SslCertificateAttr> filteredSslCerts = requestCertsSet.stream()
                .filter(item -> item.getAlias().matches(genericAliasNamePattern)).collect(Collectors.toSet());

        int i = 1;
        for (SslCertificateAttr sslCertificateAttr : filteredSslCerts) {
            String newAlias = sslCertificateAttr.getAlias().replaceFirst(replaceTimestampPattern, "_" + requestObjId + "_" + i);
            trustManagerFactory.updateAlias(sslCertificateAttr.getAlias(), newAlias);
            sslCertificateAttr.setAlias(newAlias);
            i++;
        }

        Set<SslCertificateAttr> sslCertificateAttrs = create(requestCertsSet, persistentSslCertificatesSet);
        updateCertificates(sslCertificateAttrs);
        return sslCertificateAttrs;
    }

    public List<SslCertificateAttrDto> getSslEntriesList() {
        List<SslCertificateAttrDto> list = new ArrayList<>();

        for (SslCertificateAttr attribute : listAll()) {
            SslCertificateAttrDto dto = new SslCertificateAttrDto();
            SslCertificateAttrEntityMgr.fromEntity(attribute, dto);
            list.add(dto);
        }

        return list;
    }

    public void removeCertificateList(Set<SslCertificateAttr> sslCertificateAttrs) throws Exception {
        for (SslCertificateAttr sslCertificateAttr : sslCertificateAttrs) {
            removeCertificateEntry(sslCertificateAttr);
        }
    }

    public boolean removeAlias(String alias) throws Exception {
        Optional<SslCertificateAttr> foundObject = Optional.ofNullable(this.findByFieldName("alias", alias));

        boolean isInTruststore = X509TrustManagerFactory.getInstance().exists(alias);

        if (foundObject.isPresent()) {
            delete(foundObject.get().getId());
            X509TrustManagerFactory.getInstance().removeEntry(alias);
        } else {
            X509TrustManagerFactory.getInstance().removeEntry(alias);
        }

        boolean isRemovedFromTruststore = X509TrustManagerFactory.getInstance().exists(alias);
        return isInTruststore && !isRemovedFromTruststore;
    }

    private void removeCertificateEntry(SslCertificateAttr certificateEntry) throws Exception {
        X509TrustManagerFactory.getInstance().removeEntry(certificateEntry.getAlias());
        delete(certificateEntry.getId());
    }
}