package com.ia.aggregator.application.artifact.usecase;

import com.ia.aggregator.application.artifact.port.in.ArtifactUseCase;
import com.ia.aggregator.application.artifact.port.out.ArtifactRepository;
import com.ia.aggregator.domain.artifact.Artifact;
import com.ia.aggregator.domain.artifact.ArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class ArtifactUseCaseImpl implements ArtifactUseCase {

    private static final Logger log = LoggerFactory.getLogger(ArtifactUseCaseImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ArtifactRepository artifactRepository;

    public ArtifactUseCaseImpl(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    @Override
    public Artifact create(UUID orgId, UUID userId, String title, Artifact.ArtifactType type,
                           String content, String mimeType) {
        Instant now = Instant.now();
        Artifact artifact = new Artifact(
                UUID.randomUUID(), null, orgId, userId, title, type, content,
                mimeType != null ? mimeType : inferMimeType(type),
                1, Artifact.ArtifactStatus.DRAFT, null, false,
                Map.of(), now, now
        );
        artifactRepository.save(artifact);
        artifactRepository.saveVersion(new ArtifactVersion(
                UUID.randomUUID(), artifact.id(), 1, content, "Initial version", userId, now
        ));
        log.info("Artifact created: id={}, title={}, type={}", artifact.id(), title, type);
        return artifact;
    }

    @Override
    public Artifact getById(UUID artifactId) {
        return artifactRepository.findById(artifactId)
                .orElseThrow(() -> new NoSuchElementException("Artifact not found: " + artifactId));
    }

    @Override
    public List<Artifact> listByConversation(UUID conversationId) {
        return artifactRepository.findByConversation(conversationId);
    }

    @Override
    public List<Artifact> listByOrg(UUID orgId, int page, int size) {
        return artifactRepository.findByOrg(orgId, page * size, size);
    }

    @Override
    public Artifact update(UUID artifactId, String content, String changeDescription, UUID userId) {
        Artifact existing = getById(artifactId);
        int newVersion = existing.version() + 1;
        Instant now = Instant.now();

        Artifact updated = new Artifact(
                existing.id(), existing.conversationId(), existing.orgId(), existing.createdBy(),
                existing.title(), existing.type(), content, existing.mimeType(),
                newVersion, existing.status(), existing.shareToken(), existing.isPublic(),
                existing.metadata(), existing.createdAt(), now
        );
        artifactRepository.save(updated);
        artifactRepository.saveVersion(new ArtifactVersion(
                UUID.randomUUID(), artifactId, newVersion, content,
                changeDescription != null ? changeDescription : "Update v" + newVersion,
                userId, now
        ));
        log.info("Artifact updated: id={}, version={}", artifactId, newVersion);
        return updated;
    }

    @Override
    public List<ArtifactVersion> getVersionHistory(UUID artifactId) {
        return artifactRepository.findVersions(artifactId);
    }

    @Override
    public ArtifactVersion getVersion(UUID artifactId, int versionNumber) {
        return artifactRepository.findVersion(artifactId, versionNumber)
                .orElseThrow(() -> new NoSuchElementException(
                        "Version " + versionNumber + " not found for artifact: " + artifactId));
    }

    @Override
    public Artifact publish(UUID artifactId) {
        Artifact existing = getById(artifactId);
        Artifact published = new Artifact(
                existing.id(), existing.conversationId(), existing.orgId(), existing.createdBy(),
                existing.title(), existing.type(), existing.content(), existing.mimeType(),
                existing.version(), Artifact.ArtifactStatus.PUBLISHED,
                existing.shareToken(), existing.isPublic(),
                existing.metadata(), existing.createdAt(), Instant.now()
        );
        artifactRepository.save(published);
        return published;
    }

    @Override
    public Artifact archive(UUID artifactId) {
        Artifact existing = getById(artifactId);
        Artifact archived = new Artifact(
                existing.id(), existing.conversationId(), existing.orgId(), existing.createdBy(),
                existing.title(), existing.type(), existing.content(), existing.mimeType(),
                existing.version(), Artifact.ArtifactStatus.ARCHIVED,
                existing.shareToken(), existing.isPublic(),
                existing.metadata(), existing.createdAt(), Instant.now()
        );
        artifactRepository.save(archived);
        return archived;
    }

    @Override
    public ShareResult share(UUID artifactId, boolean isPublic) {
        Artifact existing = getById(artifactId);
        String token = existing.shareToken() != null ? existing.shareToken() : generateShareToken();

        Artifact shared = new Artifact(
                existing.id(), existing.conversationId(), existing.orgId(), existing.createdBy(),
                existing.title(), existing.type(), existing.content(), existing.mimeType(),
                existing.version(), existing.status(), token, isPublic,
                existing.metadata(), existing.createdAt(), Instant.now()
        );
        artifactRepository.save(shared);
        return new ShareResult("/artifacts/shared/" + token, token, isPublic);
    }

    @Override
    public byte[] export(UUID artifactId, ExportFormat format) {
        Artifact artifact = getById(artifactId);
        return switch (format) {
            case TXT, MARKDOWN -> artifact.content().getBytes();
            case HTML -> wrapHtml(artifact.title(), artifact.content()).getBytes();
            case PDF -> ("PDF export placeholder for: " + artifact.title()).getBytes();
        };
    }

    @Override
    public void delete(UUID artifactId) {
        artifactRepository.delete(artifactId);
        log.info("Artifact deleted: id={}", artifactId);
    }

    private String generateShareToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String inferMimeType(Artifact.ArtifactType type) {
        return switch (type) {
            case CODE, SCRIPT -> "text/plain";
            case MARKDOWN -> "text/markdown";
            case HTML, REACT_COMPONENT -> "text/html";
            case RICH_TEXT -> "text/rich";
            case MERMAID_DIAGRAM -> "text/x-mermaid";
            case TABLE, CSV -> "text/csv";
            case FAQ -> "application/json";
            case JSON -> "application/json";
        };
    }

    private String wrapHtml(String title, String content) {
        return "<!DOCTYPE html><html><head><title>" + title +
                "</title></head><body>" + content + "</body></html>";
    }
}
