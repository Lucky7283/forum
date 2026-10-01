package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ExternalLinkRepository extends JpaRepository<ExternalLink, Long> {

}
