package com.example.demo.Services;

import com.example.demo.Models.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class CommunityService {
    @PersistenceContext private EntityManager em;
    public User user(Long id) {
        User u = id == null ? null : em.find(User.class,id);
        if(u==null || u.isDeleted() || !u.isEnabled()) throw new IllegalArgumentException("Usuario no disponible");
        return u;
    }
    private User lockUser(Long id) { User u=user(id); em.lock(u,LockModeType.PESSIMISTIC_WRITE); return u; }
    public boolean blocked(Long owner,Long target) {
        return em.createQuery("select count(b) from UserBlock b where b.owner.id=:o and b.target.id=:t",Long.class)
            .setParameter("o",owner).setParameter("t",target).getSingleResult()>0;
    }
    public boolean canSeeUser(Long viewer,Long owner) { return !blocked(owner,viewer); }
    public boolean canSee(Post p,Long viewer) {
        return !p.isDeleted() && !p.getAuthor().isDeleted() && p.getAuthor().isEnabled()
            && (p.getBand()==null || !p.getBand().isDeleted()) && canSeeUser(viewer,p.getAuthor().getId())
            && (p.getVisibility()==Visibility.PUBLIC || p.getAuthor().getId().equals(viewer));
    }
    public Post visiblePost(Long id,Long viewer) {
        user(viewer); Post p=em.find(Post.class,id);
        if(p==null || !canSee(p,viewer)) throw new IllegalArgumentException("Publicación no disponible");
        return p;
    }
    public record PublicUser(Long id,String username,String bio,String avatarUrl) {
        public PublicUser(Long id,String username,String bio) { this(id,username,bio,null); }
    }
    public List<PublicUser> people(Long viewer) {
        user(viewer);
        return em.createQuery("select u from User u join fetch u.profile where u.deleted=false and u.enabled=true and not exists (select b.id from UserBlock b where b.owner=u and b.target.id=:v) order by u.username",User.class)
            .setParameter("v",viewer).setMaxResults(100).getResultList().stream()
            .map(u -> new PublicUser(u.getId(),u.getUsername(),u.getProfile().getBio(),u.getProfile().getAvatarUrl())).toList();
    }
    public PublicUser profile(Long target,Long viewer) {
        user(viewer); User u=user(target);
        if(!canSeeUser(viewer,target)) throw new IllegalArgumentException("Perfil no disponible");
        return new PublicUser(u.getId(),u.getUsername(),u.getProfile().getBio(),u.getProfile().getAvatarUrl());
    }
    public void bio(Long actor,String value) {
        if(value==null || value.length()>160) throw new IllegalArgumentException("Biografía: máximo 160 caracteres");
        lockUser(actor).getProfile().setBio(value.trim());
    }
    public void avatar(Long actor,String avatarUrl) {
        lockUser(actor).getProfile().setAvatarUrl(avatarUrl);
    }
    public void bandAvatar(Long group,Long actor,String avatarUrl) {
        Group g=lockBand(group); user(actor);
        if(!g.getOwner().getId().equals(actor)) throw new IllegalArgumentException("Solo el dueño puede cambiar la foto de la banda");
        g.setAvatarUrl(avatarUrl);
    }
    public void block(Long actor,Long target,boolean block) {
        User a=lockUser(actor), t=user(target);
        if(actor.equals(target)) throw new IllegalArgumentException("No puedes bloquearte");
        var rows=em.createQuery("from UserBlock b where b.owner.id=:a and b.target.id=:t",UserBlock.class).setParameter("a",actor).setParameter("t",target).getResultList();
        if(block && rows.isEmpty()) { UserBlock b=new UserBlock(); b.owner=a;b.target=t;em.persist(b); }
        if(block) {
            em.createQuery("delete from UserFollow f where (f.follower.id=:a and f.target.id=:t) or (f.follower.id=:t and f.target.id=:a)")
                .setParameter("a",actor).setParameter("t",target).executeUpdate();
        }
        if(!block) rows.forEach(em::remove);
    }
    public List<PublicUser> blockedUsers(Long actor) {
        return em.createQuery("select b.target from UserBlock b where b.owner.id=:a",User.class).setParameter("a",actor).getResultList().stream()
            .map(u -> new PublicUser(u.getId(),u.getUsername(),null,u.getProfile()!=null?u.getProfile().getAvatarUrl():null)).toList();
    }
    public boolean following(Long follower,Long target) {
        if(follower==null || target==null) return false;
        return em.createQuery("select count(f) from UserFollow f where f.follower.id=:f and f.target.id=:t",Long.class)
            .setParameter("f",follower).setParameter("t",target).getSingleResult()>0;
    }
    public void follow(Long actor,Long target,boolean follow) {
        User a=lockUser(actor), t=user(target);
        if(actor.equals(target)) throw new IllegalArgumentException("No puedes seguirte a ti mismo");
        if(!canSeeUser(actor,target) || blocked(actor,target)) throw new IllegalArgumentException("Perfil no disponible");
        var rows=em.createQuery("from UserFollow f where f.follower.id=:a and f.target.id=:t",UserFollow.class)
            .setParameter("a",actor).setParameter("t",target).getResultList();
        if(follow && rows.isEmpty()) { UserFollow f=new UserFollow(); f.follower=a; f.target=t; em.persist(f); }
        if(!follow) rows.forEach(em::remove);
    }
    public record FollowSummary(long followersCount,long followingCount,boolean followedByMe) {}
    public FollowSummary followSummary(Long target,Long viewer) {
        profile(target,viewer);
        long followers=em.createQuery("select count(f) from UserFollow f where f.target.id=:t and f.follower.deleted=false and f.follower.enabled=true",Long.class)
            .setParameter("t",target).getSingleResult();
        long followingCnt=em.createQuery("select count(f) from UserFollow f where f.follower.id=:t and f.target.deleted=false and f.target.enabled=true",Long.class)
            .setParameter("t",target).getSingleResult();
        return new FollowSummary(followers,followingCnt,following(viewer,target));
    }
    public List<PublicUser> followersList(Long target,Long viewer) {
        profile(target,viewer);
        return em.createQuery("select f.follower from UserFollow f join fetch f.follower.profile where f.target.id=:t and f.follower.deleted=false and f.follower.enabled=true order by f.follower.username",User.class)
            .setParameter("t",target).setMaxResults(50).getResultList().stream()
            .filter(u -> canSeeUser(viewer,u.getId()))
            .map(u -> new PublicUser(u.getId(),u.getUsername(),u.getProfile().getBio(),u.getProfile().getAvatarUrl())).toList();
    }
    public List<PublicUser> followingList(Long target,Long viewer) {
        profile(target,viewer);
        return em.createQuery("select f.target from UserFollow f join fetch f.target.profile where f.follower.id=:t and f.target.deleted=false and f.target.enabled=true order by f.target.username",User.class)
            .setParameter("t",target).setMaxResults(50).getResultList().stream()
            .filter(u -> canSeeUser(viewer,u.getId()))
            .map(u -> new PublicUser(u.getId(),u.getUsername(),u.getProfile().getBio(),u.getProfile().getAvatarUrl())).toList();
    }
    public Set<Long> followedIds(Long actor) {
        user(actor);
        return new HashSet<>(em.createQuery("select f.target.id from UserFollow f where f.follower.id=:a",Long.class)
            .setParameter("a",actor).getResultList());
    }
    public void rate(Long actor,Long target,int score) {
        User a=lockUser(actor), t=user(target);
        if(actor.equals(target)) throw new IllegalArgumentException("No puedes calificarte");
        if(score<0 || score>5) throw new IllegalArgumentException("La calificación debe ser de 1 a 5");
        if(!canSeeUser(actor,target) || blocked(actor,target)) throw new IllegalArgumentException("Perfil no disponible");
        var rows=em.createQuery("from ProfileRating r where r.profile.owner.id=:t and r.reviewer.id=:a",ProfileRating.class).setParameter("t",target).setParameter("a",actor).getResultList();
        if(score==0) { rows.forEach(em::remove);return; }
        var rating=rows.isEmpty()?new ProfileRating():rows.get(0);
        rating.profile=t.getProfile();rating.reviewer=a;rating.score=score;
        if(rows.isEmpty()) em.persist(rating);
    }
    public record RatingSummary(long count,double average,int mine) {}
    public RatingSummary rating(Long target,Long viewer) {
        profile(target,viewer);
        Object[] a=em.createQuery("select count(r), avg(r.score) from ProfileRating r where r.profile.owner.id=:t",Object[].class).setParameter("t",target).getSingleResult();
        var mine=em.createQuery("select r.score from ProfileRating r where r.profile.owner.id=:t and r.reviewer.id=:v",Integer.class).setParameter("t",target).setParameter("v",viewer).getResultList();
        return new RatingSummary((Long)a[0],a[1]==null?0:(Double)a[1],mine.isEmpty()?0:mine.get(0));
    }
    public void react(Long actor,Long postId,int value) {
        User a=lockUser(actor); Post p=visiblePost(postId,actor);
        if(value < -1 || value>1) throw new IllegalArgumentException("Reacción inválida");
        var rows=em.createQuery("from PostReaction r where r.post.id=:p and r.user.id=:a",PostReaction.class).setParameter("p",postId).setParameter("a",actor).getResultList();
        if(value==0) {rows.forEach(em::remove);return;}
        var reaction=rows.isEmpty()?new PostReaction():rows.get(0);reaction.post=p;reaction.user=a;reaction.value=value;
        if(rows.isEmpty()) em.persist(reaction);
    }
    public record ReactionSummary(long likes,long dislikes,int mine) {}
    public Map<Long,ReactionSummary> reactions(List<Long> ids,Long viewer) {
        Map<Long,ReactionSummary> result=new HashMap<>(); if(ids.isEmpty())return result;
        for(Object[] row:em.createQuery("select r.post.id, sum(case when r.value=1 then 1 else 0 end), sum(case when r.value=-1 then 1 else 0 end), sum(case when r.user.id=:v then r.value else 0 end) from PostReaction r where r.post.id in :ids group by r.post.id",Object[].class).setParameter("v",viewer).setParameter("ids",ids).getResultList())
            result.put((Long)row[0],new ReactionSummary((Long)row[1],(Long)row[2],((Long)row[3]).intValue()));
        ids.forEach(id -> result.putIfAbsent(id,new ReactionSummary(0,0,0)));return result;
    }
    public Group band(Long id) {
        Group g=em.find(Group.class,id);
        if(g==null || g.isDeleted())throw new IllegalArgumentException("Banda no disponible"); return g;
    }
    private Group lockBand(Long id) {Group g=band(id);em.lock(g,LockModeType.PESSIMISTIC_WRITE);return g;}
    public Group publishingBand(Long id,Long actor) {
        Group g=lockBand(id);
        if(!permitted(g,actor,true))throw new IllegalArgumentException("No puedes publicar como esta banda");
        return g;
    }
    private Membership settings(Group g,Long id) {
        return em.createQuery("from Membership m where m.group.id=:g and m.user.id=:u",Membership.class).setParameter("g",g.getId()).setParameter("u",id).getResultStream().findFirst().orElse(null);
    }
    public boolean permitted(Group g,Long actor,boolean publish) {
        User u=user(actor); if(!g.hasMember(u) || g.isDeleted())return false;
        if(g.getOwner().getId().equals(actor))return true;
        Membership m=settings(g,actor); return m!=null && (publish?m.canPublish:m.canApprove);
    }
    public record MemberView(Long id,String username,String instrument,boolean publish,boolean approve,boolean owner,String avatarUrl) {}
    public List<MemberView> members(Long group,Long viewer) {
        Group g=band(group); user(viewer);
        return g.getMembers().stream().filter(u -> !u.isDeleted() && canSeeUser(viewer,u.getId())).map(u -> {
            Membership m=settings(g,u.getId());boolean owner=g.getOwner().getId().equals(u.getId());
            String av=u.getProfile()!=null?u.getProfile().getAvatarUrl():null;
            return new MemberView(u.getId(),u.getUsername(),m==null?"Por definir":m.instrument,owner || m!=null && m.canPublish,owner || m!=null && m.canApprove,owner,av);
        }).toList();
    }
    public void member(Long group,Long actor,Long target,String instrument,boolean publish,boolean approve) {
        Group g=lockBand(group);User u=user(target);user(actor);
        boolean owner=g.getOwner().getId().equals(actor);
        if(!g.hasMember(u) || (!owner && !actor.equals(target)))throw new IllegalArgumentException("Sin permiso para modificar integrante");
        if(instrument==null || instrument.isBlank() || instrument.length()>80)throw new IllegalArgumentException("Indicá un instrumento de hasta 80 caracteres");
        Membership m=settings(g,target);if(m==null){m=new Membership();m.group=g;m.user=u;em.persist(m);}
        m.instrument=instrument.trim(); if(owner){m.canPublish=publish;m.canApprove=approve;}
    }
    public void transfer(Long group,Long actor,Long target) {
        Group g=lockBand(group);User u=user(target);user(actor);
        if(!g.getOwner().getId().equals(actor) || !g.hasMember(u))throw new IllegalArgumentException("Solo el dueño puede transferir a un integrante");
        Membership previous=settings(g,actor);
        if(previous!=null){previous.canPublish=false;previous.canApprove=false;}
        g.assignOwner(u);
    }
    public List<Post> bandPosts(Long group,Long viewer) {
        band(group);return em.createQuery("from Post p where p.band.id=:g and p.deleted=false order by p.createdAt desc",Post.class)
            .setParameter("g",group).setMaxResults(50).getResultList().stream().filter(p -> canSee(p,viewer)).toList();
    }
    public List<Post> profilePosts(Long target,Long viewer) {
        profile(target,viewer);return em.createQuery("from Post p where p.author.id=:u and p.deleted=false order by p.createdAt desc",Post.class)
            .setParameter("u",target).setMaxResults(50).getResultList().stream().filter(p -> canSee(p,viewer)).toList();
    }
}
