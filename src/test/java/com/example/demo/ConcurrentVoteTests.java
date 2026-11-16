package com.example.demo;

import com.example.demo.Models.*;
import com.example.demo.Repositories.*;
import com.example.demo.Services.CommunityService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ConcurrentVoteTests {
    @Autowired CommunityService community;
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Test void simultaneousVotesLeaveOneRowPerUserAndTarget() throws Exception {
        var tx=new TransactionTemplate(manager);
        Long[] ids=tx.execute(status -> {
            var a=users.save(new User("A","Test",LocalDate.of(1990,1,1),"concurrentA","a@concurrent.test","unused-hash"));
            var b=users.save(new User("B","Test",LocalDate.of(1990,1,1),"concurrentB","b@concurrent.test","unused-hash"));
            var p=posts.save(new Post("Concurrent","Test",Visibility.PUBLIC,"https://example.test/a.wav",1,a));
            return new Long[]{a.getId(),b.getId(),p.getId()};
        });
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
            Callable<Void> vote=()->{ready.countDown();if(!go.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Timeout");community.rate(ids[1],ids[0],4);community.react(ids[1],ids[2],1);return null;};
            var first=executor.submit(vote);var second=executor.submit(vote);
            assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                assertEquals(1L,em.createQuery("select count(r) from ProfileRating r where r.reviewer.id=:id",Long.class).setParameter("id",ids[1]).getSingleResult());
                assertEquals(1L,em.createQuery("select count(r) from PostReaction r where r.user.id=:id",Long.class).setParameter("id",ids[1]).getSingleResult());
            });
        } finally { executor.shutdownNow(); }
    }
}
