package com.pdfpodcast.repository;

import com.pdfpodcast.model.PodcastJob;
import com.pdfpodcast.model.PodcastJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PodcastJobRepository extends JpaRepository<PodcastJob, String> {

    Optional<PodcastJob> findByIdAndOwnerUsername(String id, String ownerUsername);

    List<PodcastJob> findTop5ByStatusInAndNextAttemptAtBeforeOrderByCreatedAtAsc(
            List<PodcastJobStatus> statuses,
            LocalDateTime nextAttemptAt
    );
}
