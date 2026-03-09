package com.ia.aggregator.application.artifact.port.out;

import com.ia.aggregator.domain.artifact.Artifact;
import com.ia.aggregator.domain.artifact.ArtifactVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for artifact persistence.
 */
public interface ArtifactRepository {

    void save(Artifact artifact);

    Optional<Artifact> findById(UUID id);

    Optional<Artifact> findByShareToken(String shareToken);

    List<Artifact> findByConversation(UUID conversationId);

    List<Artifact> findByOrg(UUID orgId, int offset, int limit);

    void saveVersion(ArtifactVersion version);

    List<ArtifactVersion> findVersions(UUID artifactId);

    Optional<ArtifactVersion> findVersion(UUID artifactId, int versionNumber);

    void delete(UUID id);
}
