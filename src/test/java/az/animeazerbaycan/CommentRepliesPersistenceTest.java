package az.animeazerbaycan;

import az.animeazerbaycan.entity.*;
import az.animeazerbaycan.repository.*;
import az.animeazerbaycan.dto.request.CommentRequest;
import az.animeazerbaycan.service.Interface.*;
import az.animeazerbaycan.service.Implementation.CommunityServiceImpl;
import az.animeazerbaycan.mapper.ResponseMapper;
import az.animeazerbaycan.common.ApiException;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CommentRepliesPersistenceTest {
    SessionFactory factory;
    EntityManager em;
    CommentRepository comments;
    CommunityServiceImpl service;
    Long alice, bob, animeId, otherAnimeId;

    @BeforeEach void setup() {
        factory = new Configuration().addAnnotatedClass(Comment.class).addAnnotatedClass(User.class)
                .addAnnotatedClass(AnimeTranslated.class).addAnnotatedClass(ExternalLink.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop").buildSessionFactory();
        em = factory.createEntityManager();
        comments = new JpaRepositoryFactory(em).getRepository(CommentRepository.class);
        var users = mock(UserService.class);
        var catalog = mock(CatalogService.class);
        when(users.require(anyLong())).thenAnswer(call -> em.find(User.class, call.getArgument(0, Long.class)));
        when(catalog.require(anyLong())).thenAnswer(call -> em.find(AnimeTranslated.class, call.getArgument(0, Long.class)));
        service = new CommunityServiceImpl(users, catalog, mock(RatingRepository.class), mock(FavoriteRepository.class),
                mock(WatchlistEntryRepository.class), comments, new ResponseMapper());
        tx(() -> {
            var a = user("alice"); em.persist(a); alice = a.getId();
            var b = user("bob"); em.persist(b); bob = b.getId();
            var anime = new AnimeTranslated(); anime.setMalId(42); anime.setTitle("Title"); em.persist(anime); animeId = anime.getId();
            var other = new AnimeTranslated(); other.setMalId(99); other.setTitle("Other"); em.persist(other); otherAnimeId = other.getId();
        });
    }
    User user(String name) { var u = new User(); u.setUsername(name); u.setEmail(name+"@example.com"); u.setPassword("test"); return u; }
    void tx(Runnable action) {
        em.getTransaction().begin();
        try { action.run(); em.getTransaction().commit(); }
        catch (Throwable failure) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); throw failure; }
        finally { em.clear(); }
    }
    @AfterEach void close() {
        if (em != null) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); em.close(); }
        if (factory != null) factory.close();
    }
    @Test void persistsRepliesAndPagesOnlyDirectChildrenWithCorrectAuthor() {
        Long[] ids = new Long[4];
        tx(() -> {
            ids[0] = service.comment(alice, animeId, new CommentRequest("Root")).id();
            ids[1] = service.comment(bob, animeId, new CommentRequest("Reply", ids[0])).id();
            ids[2] = service.comment(alice, animeId, new CommentRequest("Nested", ids[1])).id();
            ids[3] = service.comment(alice, animeId, new CommentRequest("Second reply", ids[0])).id();
        });
        tx(() -> {
            var roots = service.comments(animeId,0,20);
            assertThat(roots.meta().totalElements()).isEqualTo(1);
            assertThat(roots.data().getFirst().parentCommentId()).isNull();
            var replies = service.replies(ids[0],0,1);
            assertThat(replies.meta().totalElements()).isEqualTo(2);
            assertThat(replies.data()).hasSize(1);
            var all = service.replies(ids[0],0,20).data();
            assertThat(all).extracting(c -> c.id()).containsExactly(ids[3],ids[1]);
            assertThat(all).allSatisfy(c -> assertThat(c.parentCommentId()).isEqualTo(ids[0]));
            assertThat(all.get(1).author().id()).isEqualTo(bob);
            assertThat(service.replies(ids[1],0,20).data().getFirst().id()).isEqualTo(ids[2]);
            assertThat(service.replies(ids[0],2,1).data()).isEmpty();
        });
    }
    @Test void rejectsMissingDeletedAndDifferentAnimeParentsWithoutInserting() {
        Long[] parent = new Long[1];
        tx(() -> parent[0] = service.comment(alice, animeId, new CommentRequest("Root")).id());
        assertStatus(404, () -> service.comment(bob, animeId, new CommentRequest("Missing",99999L)));
        assertStatus(400, () -> service.comment(bob, otherAnimeId, new CommentRequest("Wrong anime",parent[0])));
        assertStatus(400, () -> service.comment(bob, animeId, new CommentRequest("Invalid",0L)));
        tx(() -> service.deleteComment(alice,parent[0]));
        assertStatus(404, () -> service.comment(bob,animeId,new CommentRequest("Deleted",parent[0])));
        assertStatus(404, () -> service.replies(99999L,0,20));
        assertStatus(400, () -> service.replies(parent[0],-1,20));
        assertStatus(400, () -> service.replies(parent[0],0,101));
        tx(() -> assertThat(comments.count()).isEqualTo(1));
    }
    void assertStatus(int code,Runnable action) {
        assertThatThrownBy(() -> tx(action)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.getStatus()).isEqualTo(code));
    }
    @Test void deletedParentsBecomeTextlessPlaceholdersWhileDeletedLeavesDisappear() {
        Long[] ids = new Long[3];
        tx(() -> {
            ids[0]=service.comment(alice,animeId,new CommentRequest("Private original text")).id();
            ids[1]=service.comment(bob,animeId,new CommentRequest("Private reply text",ids[0])).id();
            ids[2]=service.comment(alice,animeId,new CommentRequest("Visible nested reply",ids[1])).id();
        });
        assertStatus(403, () -> service.deleteComment(bob,ids[0]));
        tx(() -> { service.deleteComment(alice,ids[0]); service.deleteComment(bob,ids[1]); });
        tx(() -> {
            var root=service.comments(animeId,0,20).data().getFirst();
            assertThat(root.deleted()).isTrue(); assertThat(root.body()).isNull();
            var reply=service.replies(ids[0],0,20).data().getFirst();
            assertThat(reply.deleted()).isTrue(); assertThat(reply.body()).isNull();
            assertThat(service.replies(ids[1],0,20).data().getFirst().body()).isEqualTo("Visible nested reply");
            var time=comments.findById(ids[0]).orElseThrow().getDeletedAt();
            service.deleteComment(alice,ids[0]);
            assertThat(comments.findById(ids[0]).orElseThrow().getDeletedAt()).isEqualTo(time);
            service.deleteComment(alice,ids[2]); em.flush();
            assertThat(service.replies(ids[1],0,20).data()).isEmpty();
            assertThat(comments.count()).isEqualTo(3);
        });
    }
}
