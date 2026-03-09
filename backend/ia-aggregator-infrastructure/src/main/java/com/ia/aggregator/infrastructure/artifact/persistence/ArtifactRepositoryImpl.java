package com.ia.aggregator.infrastructure.artifact.persistence;

import com.ia.aggregator.application.artifact.port.out.ArtifactRepository;
import com.ia.aggregator.domain.artifact.Artifact;
import com.ia.aggregator.domain.artifact.ArtifactVersion;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stub (no-op) implementation of ArtifactRepository.
 * TODO: Replace with real persistence implementation.
 */
@Component
public class ArtifactRepositoryImpl implements ArtifactRepository {

    @Override
    public void save(Artifact artifact) {
        // no-op
    }

    @Override
    public Optional<Artifact> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<Artifact> findByShareToken(String shareToken) {
        return Optional.empty();
    }

    @Override
    public List<Artifact> findByConversation(UUID conversationId) {
        return Collections.emptyList();
    }

    @Override
    public List<Artifact> findByOrg(UUID orgId, int offset, int limit) {
        return Collections.emptyList();
    }

    @Override
    public void saveVersion(ArtifactVersion version) {
        // no-op
    }

    @Override
    public List<ArtifactVersion> findVersions(UUID artifactId) {
        return Collections.emptyList();
    }

    @Override
    public Optional<ArtifactVersion> findVersion(UUID artifactId, int versionNumber) {
        return Optional.empty();
    }

    @Override
    public void delete(UUID id) {
        // no-op
    }
}
