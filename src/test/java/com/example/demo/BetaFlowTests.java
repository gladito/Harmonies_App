package com.example.demo;

import com.example.demo.Models.*;
import com.example.demo.Services.*;
import com.example.demo.Repositories.*;
import com.example.demo.Dtos.Auth.AuthRegisterRequestDto;
import com.example.demo.Dtos.Groups.GroupCreateRequestDto;
import com.example.demo.Dtos.Groups.GroupJoinRequestCreateDto;
import com.example.demo.Dtos.Posts.PostCreateRequestDto;
import com.example.demo.Dtos.Comments.CommentCreateRequestDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class BetaFlowTests {
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;
    @Autowired CommunityService community;
    @Autowired PostService posts;
    @Autowired CommentService comments;
    @Autowired GroupService groups;
    @Autowired GroupJoinRequestService requests;
    @Autowired AuthService auth;
    @Autowired StudioService studios;
    @Autowired JobService jobs;
    @Autowired StateMovementService movements;
    @Autowired PaymentService payments;
    @Autowired MockMvc mvc;
    User alice,bob,charlie;
    @BeforeEach void setup(){
        alice=save("alice");bob=save("bob");charlie=save("charlie");em.flush();
    }
    User save(String name){return users.save(new User(name,"Test",LocalDate.of(1990,1,1),name,name+"@example.test",encoder.encode("Harmony123")));}
    PostCreateRequestDto postForm(){var f=new PostCreateRequestDto();f.setTitle("Ensayo");f.setDescription("Idea para compartir");f.setAudioUrl("https://example.test/demo.wav");f.setVisibility(Visibility.PUBLIC);return f;}
    Long createBand(){var f=new GroupCreateRequestDto();f.setName("Banda test");f.setDescription("Ensayo");return groups.createGroup(alice.getId(),f).getId();}
    MockHttpSession login(User u)throws Exception{
        var result=mvc.perform(post("/login").with(csrf()).param("email",u.getEmail()).param("password","Harmony123"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/home")).andReturn();
        return (MockHttpSession)result.getRequest().getSession(false);
    }
    @Test void personAndProfilePersistSeparately(){
        em.flush();em.clear();var u=users.findById(alice.getId()).orElseThrow();
        assertNotNull(u.getPerson().getId());assertNotNull(u.getProfile().getId());
        assertEquals("alice",u.getPerson().getFirstName());
        assertEquals(3L,em.createQuery("select count(p) from Person p",Long.class).getSingleResult());
    }
    @Test void registrationRejectsWeakOriginalPassword(){
        var f=new AuthRegisterRequestDto();f.setFirstName("Test");f.setLastName("User");f.setBirthDate(LocalDate.of(1990,1,1));
        f.setUsername("newuser");f.setEmail("new@example.test");f.setPassword("weak");
        assertThrows(IllegalArgumentException.class,()->auth.register(f));
        f.setPassword("StrongPass123");var u=auth.register(f);em.flush();assertNotNull(u.getProfile().getId());
    }
    @Test void ratingChangesAndRemovalDoNotDuplicate(){
        community.rate(bob.getId(),alice.getId(),5);community.rate(bob.getId(),alice.getId(),3);
        community.rate(charlie.getId(),alice.getId(),5);em.flush();
        var summary=community.rating(alice.getId(),bob.getId());assertEquals(2,summary.count());assertEquals(4,summary.average());assertEquals(3,summary.mine());
        community.rate(bob.getId(),alice.getId(),0);assertEquals(1,community.rating(alice.getId(),charlie.getId()).count());
    }
    @Test void selfAndInvalidRatingsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->community.rate(alice.getId(),alice.getId(),5));
        assertThrows(IllegalArgumentException.class,()->community.rate(bob.getId(),alice.getId(),6));
    }
    @Test void reactionsSwitchAndRemove(){
        var p=posts.createPost(alice.getId(),postForm());community.react(bob.getId(),p.getId(),1);community.react(bob.getId(),p.getId(),-1);em.flush();
        var s=community.reactions(List.of(p.getId()),bob.getId()).get(p.getId());assertEquals(0,s.likes());assertEquals(1,s.dislikes());
        community.react(bob.getId(),p.getId(),0);assertEquals(0,community.reactions(List.of(p.getId()),bob.getId()).get(p.getId()).dislikes());
    }
    @Test void privateAndDeletedPostsRejectReadAndInteractions(){
        var f=postForm();f.setVisibility(Visibility.PRIVATE);var p=posts.createPost(alice.getId(),f);
        assertTrue(posts.getPostDetail(p.getId(),bob.getId()).isEmpty());
        assertThrows(IllegalArgumentException.class,()->community.react(bob.getId(),p.getId(),1));
        var comment=new CommentCreateRequestDto();comment.setPostId(p.getId());comment.setContent("Hello");
        assertThrows(IllegalArgumentException.class,()->comments.createComment(bob.getId(),comment));
        posts.deletePost(p.getId(),alice.getId());assertThrows(IllegalArgumentException.class,()->comments.createComment(alice.getId(),comment));
    }
    @Test void blockedUsersCannotReadProfileFeedOrPost(){
        var p=posts.createPost(alice.getId(),postForm());community.block(alice.getId(),bob.getId(),true);em.flush();
        assertThrows(IllegalArgumentException.class,()->community.profile(alice.getId(),bob.getId()));
        assertTrue(posts.getLatestPublicPosts(12,bob.getId()).isEmpty());assertTrue(posts.getPostDetail(p.getId(),bob.getId()).isEmpty());
        assertThrows(IllegalArgumentException.class,()->community.rate(bob.getId(),alice.getId(),5));
        assertTrue(posts.getPostDetail(p.getId(),charlie.getId()).isPresent());
        community.block(alice.getId(),bob.getId(),false);assertFalse(posts.getLatestPublicPosts(12,bob.getId()).isEmpty());
    }
    @Test void bandHasInstrumentsDelegationRevocationAndTransfer(){
        Long id=createBand();groups.joinGroup(id,bob.getId());
        community.member(id,bob.getId(),bob.getId(),"Bajo",true,true);
        assertFalse(community.permitted(community.band(id),bob.getId(),true));
        community.member(id,alice.getId(),bob.getId(),"Bajo",true,true);
        assertEquals(id,posts.createBandPost(id,bob.getId(),postForm()).getBand().getId());
        assertTrue(community.members(id,alice.getId()).stream().anyMatch(m->m.id().equals(bob.getId())&&m.instrument().equals("Bajo")));
        community.member(id,alice.getId(),bob.getId(),"Bajo",false,false);
        assertThrows(IllegalArgumentException.class,()->posts.createBandPost(id,bob.getId(),postForm()));
        community.transfer(id,alice.getId(),bob.getId());em.flush();em.clear();
        assertEquals(bob.getId(),community.band(id).getOwner().getId());
        assertFalse(community.permitted(community.band(id),alice.getId(),true));
        assertThrows(IllegalArgumentException.class,()->community.transfer(id,alice.getId(),charlie.getId()));
    }
    @Test void delegatedMemberCanApproveAndDuplicatesDoNotJoinTwice(){
        Long id=createBand();groups.joinGroup(id,bob.getId());community.member(id,alice.getId(),bob.getId(),"Bajo",false,true);
        var f=new GroupJoinRequestCreateDto();f.setGroupId(id);f.setMessage("Quiero sumarme");requests.submitRequest(charlie.getId(),f);
        assertThrows(IllegalStateException.class,()->requests.submitRequest(charlie.getId(),f));
        var pending=requests.getPendingRequestsForOwner(bob.getId());assertEquals(1,pending.size());
        requests.approveRequest(pending.get(0).getId(),bob.getId());
        assertThrows(IllegalStateException.class,()->requests.approveRequest(pending.get(0).getId(),bob.getId()));
        assertEquals(3,community.band(id).getMemberCount());
    }
    @Test void bandsCanHaveMoreThanFourMembers(){
        Long id=createBand();groups.joinGroup(id,bob.getId());groups.joinGroup(id,charlie.getId());
        groups.joinGroup(id,save("dave").getId());groups.joinGroup(id,save("eve").getId());assertEquals(5,community.band(id).getMemberCount());
    }
    @Test void anonymousAccessAndMissingCsrfAreRejected()throws Exception{
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("HARMONY")));
        mvc.perform(get("/home")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").param("email",alice.getEmail()).param("password","Harmony123")).andExpect(status().isForbidden());
        var session=login(alice);mvc.perform(post("/profile/bio").session(session).param("bio","Test")).andExpect(status().isForbidden());
    }
    @Test void allMainPagesRenderWithRealData()throws Exception{
        var p=posts.createPost(alice.getId(),postForm());Long id=createBand();em.flush();var session=login(alice);
        for(String path:List.of("/home","/people","/people/"+alice.getId(),"/people/"+bob.getId(),"/bands/"+id,"/groups","/groups/explore","/groups/requests","/groups/new","/posts/new","/posts/"+p.getId(),"/account"))
            mvc.perform(get(path).session(session)).andExpect(status().isOk());
    }
    @Test void httpFormsPersistInteractions()throws Exception{
        var p=posts.createPost(alice.getId(),postForm());var session=login(bob);
        mvc.perform(post("/people/"+alice.getId()+"/rating").session(session).with(csrf()).param("score","4")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/posts/"+p.getId()+"/reaction").session(session).with(csrf()).param("value","1")).andExpect(status().is3xxRedirection());
        assertEquals(4,community.rating(alice.getId(),bob.getId()).mine());assertEquals(1,community.reactions(List.of(p.getId()),bob.getId()).get(p.getId()).likes());
    }
    @Test void tagsNormalizeFilterAndCanBeEditedOrRemoved(){
        var f=postForm();f.setTags(" Jazz, bajo, jazz ");
        var p=posts.createPost(alice.getId(),f);em.flush();
        assertEquals(2,p.getTags().size());
        assertEquals(1,posts.feed(bob.getId(),0," JAZZ ").posts().size());
        assertTrue(posts.feed(bob.getId(),0,"rock").posts().isEmpty());
        var edit=posts.getPostForEdit(p.getId(),alice.getId()).orElseThrow();
        assertEquals("bajo, jazz",edit.getTags());
        edit.setTags("rock");posts.updatePost(p.getId(),alice.getId(),edit);em.flush();
        assertTrue(posts.feed(bob.getId(),0,"jazz").posts().isEmpty());
        assertEquals(1,posts.feed(bob.getId(),0,"rock").posts().size());
        edit.setTags("");posts.updatePost(p.getId(),alice.getId(),edit);em.flush();
        assertTrue(posts.getPostForEdit(p.getId(),alice.getId()).orElseThrow().getTags().isEmpty());
    }
    @Test void tagLimitsAreEnforced(){
        var f=postForm();f.setTags("one,two,three,four,five,six");
        assertThrows(IllegalArgumentException.class,()->posts.createPost(alice.getId(),f));
        f.setTags("x".repeat(51));assertThrows(IllegalArgumentException.class,()->posts.createPost(alice.getId(),f));
    }
    @Test void paginationAppliesBlockingAndPrivacyBeforeLimiting(){
        for(int i=0;i<13;i++){var f=postForm();f.setTitle("visible "+i);posts.createPost(charlie.getId(),f);}
        for(int i=0;i<14;i++){var f=postForm();f.setTitle("blocked "+i);posts.createPost(alice.getId(),f);}
        var hidden=postForm();hidden.setVisibility(Visibility.PRIVATE);posts.createPost(charlie.getId(),hidden);
        community.block(alice.getId(),bob.getId(),true);em.flush();
        var first=posts.feed(bob.getId(),0,"");var second=posts.feed(bob.getId(),1,"");
        assertEquals(12,first.posts().size());assertTrue(first.hasMore());
        assertEquals(1,second.posts().size());assertFalse(second.hasMore());
        assertTrue(first.posts().stream().allMatch(p->p.getTitle().startsWith("visible")));
        assertFalse(first.posts().stream().anyMatch(p->p.getId().equals(second.posts().get(0).getId())));
    }
    @Test void englishChoicePersistsAndMainPagesTranslate()throws Exception{
        Long id=createBand();var session=login(alice);
        var response=mvc.perform(get("/home?lang=en").session(session)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Your next collaboration starts here."))).andReturn().getResponse();
        var language=response.getCookie("harmony-language");assertNotNull(language);
        mvc.perform(get("/people").session(session).cookie(language)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Discover other sounds.")));
        for(String path:List.of("/people/"+bob.getId(),"/bands/"+id,"/posts/new","/account"))
            mvc.perform(get(path).session(session).cookie(language)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("??"))));
        mvc.perform(get("/home?lang=es").session(session).cookie(language)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tu próxima colaboración empieza acá.")));
    }
    @Test void postFormContainsEditableTags()throws Exception{
        var f=postForm();f.setTags("jazz");var p=posts.createPost(alice.getId(),f);var session=login(alice);
        mvc.perform(get("/posts/new").session(session)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"tags\"")));
        mvc.perform(get("/posts/"+p.getId()+"/edit").session(session)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"jazz\"")));
    }
    @Test void englishPagesDoNotContainHardcodedSpanishSentinels()throws Exception{
        Long bandId=createBand();
        var p=posts.createPost(alice.getId(),postForm());
        var session=login(alice);
        var response=mvc.perform(get("/home?lang=en").session(session)).andExpect(status().isOk()).andReturn().getResponse();
        var language=response.getCookie("harmony-language");
        assertNotNull(language);
        List<String> paths=List.of(
            "/home","/people","/people/"+bob.getId(),"/bands/"+bandId,
            "/posts/new","/account","/groups","/groups/explore",
            "/groups/new","/groups/"+bandId+"/edit","/groups/requests",
            "/posts/"+p.getId(),"/posts/"+p.getId()+"/edit"
        );
        List<String> sentinels=List.of(
            "Guardar cambios","Crear banda","Integrantes:",
            "Duración:","Eliminar publicación","Sin descripción"
        );
        for(String path:paths){
            var html=mvc.perform(get(path).session(session).cookie(language))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            for(String sentinel:sentinels){
                assertFalse(html.contains(sentinel),"Path "+path+" contains hardcoded Spanish sentinel: "+sentinel);
            }
        }
    }
    @Test void logoutInvalidatesSessionAndRedirects()throws Exception{
        var session=login(alice);
        mvc.perform(post("/logout").with(csrf()).session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/home").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }
    @Test void invalidLoginRedirectsToError()throws Exception{
        mvc.perform(post("/login").with(csrf()).param("email","wrong@example.com").param("password","badpassword"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }
    @Test void weakPasswordChangeIsRejected()throws Exception{
        var session=login(alice);
        mvc.perform(post("/profile/password").with(csrf()).session(session)
                .param("currentPassword","Harmony123")
                .param("newPassword","weak"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile"))
            .andExpect(flash().attributeExists("passwordError"));
    }
    @Test void editPagesRenderSuccessfully()throws Exception{
        Long bandId=createBand();
        var p=posts.createPost(alice.getId(),postForm());
        var session=login(alice);
        mvc.perform(get("/groups/"+bandId+"/edit").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Banda test")));
        mvc.perform(get("/posts/"+p.getId()+"/edit").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Ensayo")));
    }
    @Test void studiosBookingConflictAndCancellationFlow()throws Exception{
        var s=studios.create(alice.getId(),"Sala Dorada",StudioType.REHEARSAL,"Insonorizada","Palermo","Batería y amplis",10000);
        em.flush();
        String slot=LocalDate.now().plusDays(2).atTime(18,0).toString();
        var booking=studios.book(bob.getId(),s.getId(),slot,2);
        assertEquals(20000,booking.getTotalPrice());
        // Overlapping booking rejected
        assertThrows(IllegalArgumentException.class,()->studios.book(charlie.getId(),s.getId(),LocalDate.now().plusDays(2).atTime(19,0).toString(),2));
        // Self booking rejected
        assertThrows(IllegalArgumentException.class,()->studios.book(alice.getId(),s.getId(),LocalDate.now().plusDays(3).atTime(18,0).toString(),1));
        // Cancel frees the slot
        studios.cancel(bob.getId(),booking.getId());
        var rebooked=studios.book(charlie.getId(),s.getId(),slot,2);
        assertEquals(BookingStatus.CONFIRMED,rebooked.getStatus());
        var session=login(bob);
        mvc.perform(get("/studios").session(session)).andExpect(status().isOk());
        mvc.perform(get("/studios/"+s.getId()).session(session)).andExpect(status().isOk());
    }
    @Test void jobPostingApplicationAndDecisionFlow()throws Exception{
        var j=jobs.create(alice.getId(),"Sesión de bajo",JobType.SESSION,"Grabación de disco","Bajo","CABA",false,"$ 25.000");
        em.flush();
        var app=jobs.apply(bob.getId(),j.getId(),"Tengo equipo propio");
        assertEquals(ApplicationStatus.PENDING,app.getStatus());
        assertThrows(IllegalArgumentException.class,()->jobs.apply(bob.getId(),j.getId(),"Duplicada"));
        assertThrows(IllegalArgumentException.class,()->jobs.apply(alice.getId(),j.getId(),"Propia"));
        jobs.decide(alice.getId(),app.getId(),true);
        assertEquals(ApplicationStatus.ACCEPTED,app.getStatus());
        var session=login(bob);
        mvc.perform(get("/jobs").session(session)).andExpect(status().isOk());
        mvc.perform(get("/jobs/"+j.getId()).session(session)).andExpect(status().isOk());
    }
    @Test void userAndBandProfilePicturesPersistAndEnforcePermissions()throws Exception{
        Long bandId=createBand();
        var aliceSession=login(alice);
        var bobSession=login(bob);

        // User sets avatar via URL
        mvc.perform(post("/profile/avatar").session(aliceSession).with(csrf())
                .param("avatarUrl","https://example.test/alice.png"))
            .andExpect(status().is3xxRedirection());
        assertEquals("https://example.test/alice.png",community.profile(alice.getId(),bob.getId()).avatarUrl());

        // User uploads avatar file
        var pngBytes=new byte[]{(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A};
        var userFile=new org.springframework.mock.web.MockMultipartFile("avatarFile","avatar.png","image/png",pngBytes);
        mvc.perform(multipart("/profile/avatar").file(userFile).session(aliceSession).with(csrf()))
            .andExpect(status().is3xxRedirection());
        String storedUserAvatar=community.profile(alice.getId(),bob.getId()).avatarUrl();
        assertNotNull(storedUserAvatar);
        assertTrue(storedUserAvatar.startsWith("/avatars/user-"));
        mvc.perform(get(storedUserAvatar).session(bobSession)).andExpect(status().isOk());

        // Band owner uploads band avatar
        var bandFile=new org.springframework.mock.web.MockMultipartFile("avatarFile","band.png","image/png",pngBytes);
        mvc.perform(multipart("/bands/"+bandId+"/avatar").file(bandFile).session(aliceSession).with(csrf()))
            .andExpect(status().is3xxRedirection());
        String storedBandAvatar=community.band(bandId).getAvatarUrl();
        assertNotNull(storedBandAvatar);
        assertTrue(storedBandAvatar.startsWith("/avatars/band-"));

        // Non-owner cannot change band avatar
        assertThrows(IllegalArgumentException.class,()->community.bandAvatar(bandId,bob.getId(),"https://example.test/hack.png"));

        // Remove user avatar
        mvc.perform(post("/profile/avatar").session(aliceSession).with(csrf()).param("remove","true"))
            .andExpect(status().is3xxRedirection());
        assertNull(community.profile(alice.getId(),bob.getId()).avatarUrl());
    }
    @Test void stateMovementsRecordTraceabilityAcrossAllStatefulFlows()throws Exception{
        Long bandId=createBand();
        var reqDto=new GroupJoinRequestCreateDto();
        reqDto.setGroupId(bandId);
        reqDto.setMessage("Quiero tocar la guitarra");

        // 1. Bob submits a request and then cancels it (null -> PENDING -> CANCELLED)
        requests.submitRequest(bob.getId(),reqDto);
        var bobPending=requests.getPendingRequestsByRequester(bob.getId());
        assertEquals(1,bobPending.size());
        Long req1Id=bobPending.get(0).getId();
        requests.cancelRequest(req1Id,bob.getId());

        var req1History=movements.historyForEntity(MovementEntityType.GROUP_JOIN_REQUEST,req1Id);
        assertEquals(2,req1History.size());
        assertNull(req1History.get(0).getPreviousStatus());
        assertEquals("PENDING",req1History.get(0).getNewStatus());
        assertEquals("PENDING",req1History.get(1).getPreviousStatus());
        assertEquals("CANCELLED",req1History.get(1).getNewStatus());

        // 2. Charlie submits a request and Alice approves it (null -> PENDING -> APPROVED)
        requests.submitRequest(charlie.getId(),reqDto);
        var ownerPending=requests.getPendingRequestsForOwner(alice.getId());
        assertEquals(1,ownerPending.size());
        Long req2Id=ownerPending.get(0).getId();
        requests.approveRequest(req2Id,alice.getId());

        var req2History=movements.historyForEntity(MovementEntityType.GROUP_JOIN_REQUEST,req2Id);
        assertEquals(2,req2History.size());
        assertEquals("PENDING",req2History.get(1).getPreviousStatus());
        assertEquals("APPROVED",req2History.get(1).getNewStatus());
        assertEquals(alice.getId(),req2History.get(1).getActor().getId());

        // 3. Studio booking and cancellation movements (null -> CONFIRMED -> CANCELLED)
        var studio=studios.create(alice.getId(),"Sala Trace",StudioType.REHEARSAL,"Sala","CABA","Equipos",8000);
        var booking=studios.book(bob.getId(),studio.getId(),LocalDate.now().plusDays(4).atTime(19,0).toString(),2);
        studios.cancel(bob.getId(),booking.getId());
        var bookingHistory=movements.historyForEntity(MovementEntityType.STUDIO_BOOKING,booking.getId());
        assertEquals(2,bookingHistory.size());
        assertEquals("CONFIRMED",bookingHistory.get(0).getNewStatus());
        assertEquals("CANCELLED",bookingHistory.get(1).getNewStatus());

        // 4. Job posting and application movements
        var job=jobs.create(alice.getId(),"Busco baterista",JobType.BAND_MEMBER,"Ensayos","Batería","CABA",false,"A convenir");
        var app=jobs.apply(bob.getId(),job.getId(),"Me sumo");
        jobs.decide(alice.getId(),app.getId(),false);
        jobs.setOpen(alice.getId(),job.getId(),false);

        var appHistory=movements.historyForEntity(MovementEntityType.JOB_APPLICATION,app.getId());
        assertEquals(2,appHistory.size());
        assertEquals("PENDING",appHistory.get(0).getNewStatus());
        assertEquals("REJECTED",appHistory.get(1).getNewStatus());

        var jobHistory=movements.historyForEntity(MovementEntityType.JOB_POSTING,job.getId());
        assertEquals(2,jobHistory.size());
        assertEquals("OPEN",jobHistory.get(0).getNewStatus());
        assertEquals("CLOSED",jobHistory.get(1).getNewStatus());

        var session=login(alice);
        mvc.perform(get("/movements").session(session)).andExpect(status().isOk());
        mvc.perform(get("/movements").param("type","GROUP_JOIN_REQUEST").session(session)).andExpect(status().isOk());
    }
    @Test void paymentsForStudiosAndJobContractsTrackStatusReceiptsAndMovements()throws Exception{
        // 1. Studio booking starts in PENDING_PAYMENT without receipt until transfer/card data is provided
        var studio=studios.create(alice.getId(),"Estudio Pago",StudioType.RECORDING,"Sala","CABA","Consola",15000);
        var booking=studios.book(bob.getId(),studio.getId(),LocalDate.now().plusDays(5).atTime(15,0).toString(),2,PaymentMethod.MERCADO_PAGO,false);
        var studioPayment=payments.findByReference(PaymentConceptType.STUDIO_BOOKING,booking.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING_PAYMENT,studioPayment.getStatus());
        assertEquals(30000,studioPayment.getAmount());
        assertNull(studioPayment.getReceiptCode());

        // Attempting to complete payment without transfer reference is rejected
        assertThrows(IllegalArgumentException.class,()->payments.completePayment(bob.getId(),studioPayment.getId(),PaymentMethod.MERCADO_PAGO,30000,"",null,null));

        // Completing payment with real operation reference sets PAID and receipt code
        payments.completePayment(bob.getId(),studioPayment.getId(),PaymentMethod.MERCADO_PAGO,30000,"98412055",null,null);
        assertEquals(PaymentStatus.PAID,studioPayment.getStatus());
        assertEquals("MP-98412055",studioPayment.getReceiptCode());

        studios.cancel(bob.getId(),booking.getId());
        assertEquals(PaymentStatus.REFUNDED,payments.findByReference(PaymentConceptType.STUDIO_BOOKING,booking.getId()).orElseThrow().getStatus());
        var studioPayHistory=movements.historyForEntity(MovementEntityType.PAYMENT,studioPayment.getId());
        assertEquals(3,studioPayHistory.size());
        assertEquals("PENDING_PAYMENT",studioPayHistory.get(0).getNewStatus());
        assertEquals("PAID",studioPayHistory.get(1).getNewStatus());
        assertEquals("REFUNDED",studioPayHistory.get(2).getNewStatus());

        // 2. Job contract: accepting applicant generates PENDING_PAYMENT order -> author pays via BANK_TRANSFER with operationReference
        var job=jobs.create(alice.getId(),"Sesión de cuerdas",JobType.SESSION,"Grabación","Violín","CABA",false,"$ 35.000 por sesión");
        var app=jobs.apply(charlie.getId(),job.getId(),"Disponible");
        jobs.decide(alice.getId(),app.getId(),true);

        var jobPayment=payments.findByReference(PaymentConceptType.JOB_CONTRACT,app.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING_PAYMENT,jobPayment.getStatus());
        assertEquals(35000,jobPayment.getAmount());
        assertNull(jobPayment.getReceiptCode());

        var aliceSession=login(alice);
        mvc.perform(post("/payments/"+jobPayment.getId()+"/pay").session(aliceSession).with(csrf())
                .param("paymentMethod","BANK_TRANSFER")
                .param("amount","35000")
                .param("operationReference","88776655"))
            .andExpect(status().is3xxRedirection());

        em.flush();em.clear();
        var paidJobPayment=payments.findByReference(PaymentConceptType.JOB_CONTRACT,app.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID,paidJobPayment.getStatus());
        assertEquals(PaymentMethod.BANK_TRANSFER,paidJobPayment.getPaymentMethod());
        assertEquals("TRANSF-88776655",paidJobPayment.getReceiptCode());

        mvc.perform(get("/payments").session(aliceSession)).andExpect(status().isOk());
    }
    @Test void forgotPasswordRequiresVerifiedEmailBeforeResettingPassword()throws Exception{
        MockHttpSession resetSession=new MockHttpSession();

        // 1. Step 1 page renders
        mvc.perform(get("/forgot-password").session(resetSession))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("forgot-password/verify")));

        // 2. Attempting to reset password without verifying email first is rejected
        mvc.perform(post("/forgot-password/reset").session(resetSession).with(csrf())
                .param("newPassword","NuevaClave99")
                .param("confirmPassword","NuevaClave99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/forgot-password"))
            .andExpect(flash().attributeExists("forgotError"));

        // 3. Unregistered email is rejected and does not unlock Step 2
        mvc.perform(post("/forgot-password/verify").session(resetSession).with(csrf())
                .param("email","inexistente@example.test"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/forgot-password"))
            .andExpect(flash().attributeExists("forgotError"));
        assertNull(resetSession.getAttribute("resetEmail"));

        // 4. Registered email unlocks Step 2
        mvc.perform(post("/forgot-password/verify").session(resetSession).with(csrf())
                .param("email",alice.getEmail()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/forgot-password"))
            .andExpect(flash().attributeExists("forgotSuccess"));
        assertEquals(alice.getEmail(),resetSession.getAttribute("resetEmail"));

        // 5. Mismatched or weak password in Step 2 is rejected while keeping verified email
        mvc.perform(post("/forgot-password/reset").session(resetSession).with(csrf())
                .param("newPassword","NuevaClave99")
                .param("confirmPassword","OtraClave99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/forgot-password"))
            .andExpect(flash().attributeExists("forgotError"));
        assertEquals(alice.getEmail(),resetSession.getAttribute("resetEmail"));

        // 6. Valid new password updates account and allows logging in with the new password
        mvc.perform(post("/forgot-password/reset").session(resetSession).with(csrf())
                .param("newPassword","NuevaClave99")
                .param("confirmPassword","NuevaClave99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andExpect(flash().attributeExists("loginSuccess"));
        assertNull(resetSession.getAttribute("resetEmail"));

        mvc.perform(post("/login").with(csrf())
                .param("email",alice.getEmail())
                .param("password","NuevaClave99"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/home"));
    }
    @Test void passwordIsPhysicallySplitAcrossUsersAndUserPasswordPartsTables(){
        em.flush();em.clear();
        var u=users.findById(alice.getId()).orElseThrow();
        String part1=u.getPasswordPart1();
        String part2=u.getPasswordPart2();

        assertNotNull(part1);
        assertNotNull(part2);
        assertEquals(30,part1.length());
        assertEquals(30,part2.length());

        // Neither part alone is a valid BCrypt hash for Harmony123
        assertFalse(encoder.matches("Harmony123",part1));
        assertFalse(encoder.matches("Harmony123",part2));

        // Combined across the 2 tables (users + user_password_parts), the password matches
        assertEquals(part1+part2,u.getPassword());
        assertTrue(encoder.matches("Harmony123",u.getPassword()));
        assertNotNull(u.getPasswordPart().getKeySalt());
    }
    @Test void userFollowUnfollowAndFollowingFeedFilterWork()throws Exception{
        posts.createPost(alice.getId(),postForm());
        posts.createPost(charlie.getId(),postForm());

        // Self-follow rejected
        assertThrows(IllegalArgumentException.class,()->community.follow(bob.getId(),bob.getId(),true));

        // Bob follows Alice via HTTP endpoint
        var bobSession=login(bob);
        mvc.perform(post("/people/"+alice.getId()+"/follow").session(bobSession).with(csrf())
                .param("following","true"))
            .andExpect(status().is3xxRedirection());

        var summary=community.followSummary(alice.getId(),bob.getId());
        assertEquals(1,summary.followersCount());
        assertTrue(summary.followedByMe());

        // Feed filtered by following shows only Alice's post to Bob
        var followingFeed=posts.feed(bob.getId(),0,"",true);
        assertEquals(1,followingFeed.posts().size());
        assertEquals(alice.getId(),followingFeed.posts().get(0).getAuthorId());

        // Blocking removes follow relationship
        community.block(alice.getId(),bob.getId(),true);
        assertFalse(community.following(bob.getId(),alice.getId()));
    }
}
