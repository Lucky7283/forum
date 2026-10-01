package az.animeazerbaycan;

import az.animeazerbaycan.entity.*;
import az.animeazerbaycan.repository.*;
import az.animeazerbaycan.mapper.ResponseMapper;
import az.animeazerbaycan.service.Implementation.AnimeGenreServiceImpl;
import az.animeazerbaycan.service.Implementation.CatalogServiceImpl;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class AnimeGenrePersistenceTest {
    private SessionFactory factory;
    private EntityManager em;
    private AnimeGenreRepository links;
    private GenreRepository genres;
    private AnimeGenreServiceImpl service;
    private JpaRepositoryFactory repositories;

    @BeforeEach void setup() {
        factory = new Configuration()
                .addAnnotatedClass(Anime.class).addAnnotatedClass(AnimeTranslated.class)
                .addAnnotatedClass(Genre.class).addAnnotatedClass(AnimeGenre.class)
                .addAnnotatedClass(ExternalLink.class).addAnnotatedClass(User.class).addAnnotatedClass(Rating.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
        em = factory.createEntityManager();
        repositories = new JpaRepositoryFactory(em);
        links = repositories.getRepository(AnimeGenreRepository.class);
        genres = repositories.getRepository(GenreRepository.class);
        service = new AnimeGenreServiceImpl(genres, links);
    }

    @AfterEach void close() {
        if (em != null) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
        if (factory != null) factory.close();
    }

    private void tx(Runnable action) {
        em.getTransaction().begin();
        action.run();
        em.getTransaction().commit();
        em.clear();
    }

    @Test void sameMalIdentityUsesSameGenresBeforeAndAfterTranslationAndFiltersCatalog() {
        tx(() -> {
            var padding = new Anime(); padding.setMalId(999); padding.setSlug("padding"); padding.setTitle("Padding"); em.persist(padding);
            var original = new Anime(); original.setMalId(42); original.setSlug("shared-42"); original.setTitle("Original"); em.persist(original);
            service.replace(42, List.of("Action", "Adventure"));
        });
        assertThat(links.findGenresByAnimeMalId(42)).extracting(Genre::getName).containsExactly("Action", "Adventure");
        tx(() -> {
            var translated = new AnimeTranslated(); translated.setMalId(42); translated.setTitle("Translated"); em.persist(translated);
        });
        var originals = repositories.getRepository(AnimeRepository.class, new AnimeTranslationRepositoryImpl(em));
        var translated = repositories.getRepository(AnimeTranslatedRepository.class);
        var catalog = new CatalogServiceImpl(translated, originals, genres, links,
                repositories.getRepository(RatingRepository.class), new ResponseMapper());
        tx(() -> {
            assertThat(originals.findByMalId(42).orElseThrow().getId())
                    .isNotEqualTo(translated.findByMalId(42).orElseThrow().getId());
            var page = catalog.catalog(0, 20, null, "action", null, "title,asc");
            assertThat(page.meta().totalElements()).isEqualTo(1);
            assertThat(page.data().getFirst().genres()).extracting(g -> g.name()).containsExactly("Action", "Adventure");
            assertThat(catalog.catalog(0, 20, null, "missing", null, "title,asc").data()).isEmpty();
            assertThat(catalog.detail("shared-42").genres()).hasSize(2);
        });
    }

    @Test void repeatedImportsDeduplicateAndReplaceOnlyOneAnimeMembership() {
        tx(() -> {
            service.replace(42, List.of(" Action ", "action", "Slice   of Life"));
            service.replace(43, List.of("Action"));
        });
        tx(() -> service.replace(42, List.of("ACTION", "Slice of Life")));
        assertThat(genres.count()).isEqualTo(2);
        assertThat(links.count()).isEqualTo(3);
        tx(() -> service.replace(42, List.of("Drama")));
        assertThat(links.findGenresByAnimeMalId(42)).extracting(Genre::getName).containsExactly("Drama");
        assertThat(links.findGenresByAnimeMalId(43)).extracting(Genre::getName).containsExactly("Action");
        tx(() -> service.replace(42, null));
        assertThat(links.findGenresByAnimeMalId(42)).hasSize(1);
        tx(() -> service.replace(42, List.of()));
        assertThat(links.findGenresByAnimeMalId(42)).isEmpty();
        assertThat(genres.count()).isEqualTo(3);
    }

    @Test void existingGenreIdentityIsRetainedAndNameOnlyGenresNeedNoMalId() {
        tx(() -> {
            var old = new Genre(); old.setMalId(1); old.setName("Action"); old.setSlug("action"); em.persist(old);
        });
        tx(() -> service.replace(42, List.of("action", "Drama")));
        assertThat(genres.findBySlug("action").orElseThrow().getMalId()).isEqualTo(1);
        assertThat(genres.findBySlug("drama").orElseThrow().getMalId()).isNull();
        assertThat(genres.count()).isEqualTo(2);
    }

    @Test void invalidInputDoesNotEraseExistingLinksAndRollbackRestoresMemberships() {
        tx(() -> service.replace(42, List.of("Action")));
        em.getTransaction().begin();
        assertThatThrownBy(() -> service.replace(42, List.of("Drama", " "))).isInstanceOf(IllegalArgumentException.class);
        em.getTransaction().rollback(); em.clear();
        assertThat(links.findGenresByAnimeMalId(42)).extracting(Genre::getName).containsExactly("Action");
        em.getTransaction().begin();
        service.replace(42, List.of("Drama")); em.flush();
        em.getTransaction().rollback(); em.clear();
        assertThat(links.findGenresByAnimeMalId(42)).extracting(Genre::getName).containsExactly("Action");
        assertThat(genres.count()).isEqualTo(1);
    }
}
