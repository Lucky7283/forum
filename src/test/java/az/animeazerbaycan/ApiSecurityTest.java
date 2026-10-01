package az.animeazerbaycan;

import az.animeazerbaycan.dto.response.UserResponse;
import az.animeazerbaycan.controller.*;
import az.animeazerbaycan.config.SecurityConfig;
import az.animeazerbaycan.security.JwtService;
import az.animeazerbaycan.service.Interface.*;
import az.animeazerbaycan.repository.UserRepository;
import az.animeazerbaycan.entity.User;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(controllers={AuthController.class,CommunityController.class},properties={"app.jwt.signing-key=test-signing-key-with-at-least-32-bytes","app.jwt.ttl=PT1H","app.client-origin=https://frontend.example"})
@Import({SecurityConfig.class,JwtService.class})
class ApiSecurityTest {
 @Autowired MockMvc mvc; @Autowired JwtService jwt;
 @MockitoBean UserService users; @MockitoBean UserRepository repository; @MockitoBean CommunityService community;
 private Cookie auth() { User u=new User(); u.setId(1L); when(repository.findById(1L)).thenReturn(Optional.of(u)); return new Cookie("auth",jwt.issue(1L)); }
 @Test void protectsPrivateEndpoints() throws Exception { mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401)); }
 @Test void rejectsCrossOriginMutation() throws Exception { mvc.perform(post("/api/v1/auth/login").header("Origin","https://attacker.example").contentType("application/json").content("{}" )).andExpect(status().isForbidden()); verifyNoInteractions(users); }
 @Test void validatesRatingRange() throws Exception { mvc.perform(put("/api/v1/anime/1/rating").cookie(auth()).header("Origin","https://frontend.example").contentType("application/json").content("{\"localScore\":11}" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.localScore").exists()); verifyNoInteractions(community); }
 @Test void validatesPositiveIds() throws Exception { mvc.perform(put("/api/v1/anime/0/rating").cookie(auth()).header("Origin","https://frontend.example").contentType("application/json").content("{\"localScore\":7}" )).andExpect(status().isBadRequest()); verifyNoInteractions(community); }
 @Test void loginSetsSecureCookie() throws Exception {
  when(users.login(any())).thenReturn(new UserResponse(1L,"name","a@example.com",null,"USER",Instant.now()));
  mvc.perform(post("/api/v1/auth/login").header("Origin","https://frontend.example").contentType("application/json").content("{\"email\":\"a@example.com\",\"password\":\"password123\"}" )).andExpect(status().isOk()).andExpect(cookie().httpOnly("auth",true)).andExpect(cookie().secure("auth",true)).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("SameSite=Strict")));
 }
 @Test void logoutClearsCookie() throws Exception { mvc.perform(post("/api/v1/auth/logout").cookie(auth()).header("Origin","https://frontend.example")).andExpect(status().isNoContent()).andExpect(cookie().maxAge("auth",0)); }

 @Test void replyCreationRequiresAuthentication() throws Exception {
  mvc.perform(post("/api/v1/anime/50/comments").header("Origin","https://frontend.example")
    .contentType("application/json").content("{\"body\":\"Reply\",\"parentCommentId\":10}"))
    .andExpect(status().isUnauthorized());
  verifyNoInteractions(community);
 }
 @Test void replyAuthorComesFromCookieAndParentIsReturned() throws Exception {
  when(community.comment(eq(1L),eq(50L),any())).thenReturn(
    new az.animeazerbaycan.dto.response.CommentResponse(11L,50L,
      new az.animeazerbaycan.dto.response.ProfileResponse(1L,"alice",null,Instant.now()),"Reply",Instant.now(),10L,false));
  mvc.perform(post("/api/v1/anime/50/comments").cookie(auth()).header("Origin","https://frontend.example")
    .contentType("application/json").content("{\"body\":\"Reply\",\"parentCommentId\":10,\"userId\":999,\"author\":{\"id\":999}}"))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.author.id").value(1))
    .andExpect(jsonPath("$.parentCommentId").value(10)).andExpect(jsonPath("$.deleted").value(false));
  verify(community).comment(1L,50L,new az.animeazerbaycan.dto.request.CommentRequest("Reply",10L));
 }
 @Test void rejectsNonpositiveParentId() throws Exception {
  mvc.perform(post("/api/v1/anime/50/comments").cookie(auth()).header("Origin","https://frontend.example")
    .contentType("application/json").content("{\"body\":\"Reply\",\"parentCommentId\":0}"))
    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.parentCommentId").exists());
  verifyNoInteractions(community);
 }
 @Test void directRepliesArePublicAndKeepPaginationParameters() throws Exception {
  when(community.replies(10L,1,5)).thenReturn(new az.animeazerbaycan.dto.response.PagedResponse<>(java.util.List.of(),
    new az.animeazerbaycan.dto.response.MetaResponse(1,5,0,0)));
  mvc.perform(get("/api/v1/comments/10/replies?page=1&size=5"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray()).andExpect(jsonPath("$.meta.page").value(1));
  verify(community).replies(10L,1,5);
 }
 @Test void legacyCommentRequestStillCreatesRoot() throws Exception {
  mvc.perform(post("/api/v1/anime/50/comments").cookie(auth()).header("Origin","https://frontend.example")
    .contentType("application/json").content("{\"body\":\"Root\"}"))
    .andExpect(status().isCreated());
  verify(community).comment(1L,50L,new az.animeazerbaycan.dto.request.CommentRequest("Root",null));
 }
}
